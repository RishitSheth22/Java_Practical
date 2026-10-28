package com.themepark.service;

import com.themepark.dto.Requests;
import com.themepark.model.BookingRow;
import com.themepark.model.RideRow;
import com.themepark.model.SlotRow;
import com.themepark.repository.BookingRepository;
import com.themepark.repository.RideRepository;
import com.themepark.util.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CORE FUNCTION 1: ticket purchase + ride booking.
 * Every booking is validated against live database capacity inside one transaction,
 * with the slot row locked (SELECT ... FOR UPDATE) so two visitors cannot take the last seat.
 */
@Service
public class BookingService {
    static final int MAX_PER_VISITOR = 10;          // per-visitor ticket / seat limit
    static final int BOOKING_WINDOW_DAYS = 90;      // how far ahead a visit can be booked
    private static final Set<String> PAYMENT_MODES = Set.of("CARD", "UPI", "NETBANKING", "CASH");
    private static final Map<String, BigDecimal> RIDE_FEE_MULTIPLIERS = Map.of(
            "child", new BigDecimal("0.50"),
            "standard", BigDecimal.ONE,
            "express", new BigDecimal("1.00"),
            "vip", new BigDecimal("1.00"));

    private final BookingRepository bookings;
    private final RideRepository rides;

    public BookingService(BookingRepository bookings, RideRepository rides) {
        this.bookings = bookings;
        this.rides = rides;
    }

    // ------------------------------------------------------------ browsing

    public List<Map<String, Object>> ticketTypes() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> type : bookings.ticketTypes()) {
            Map<String, Object> pricedType = new LinkedHashMap<>(type);
            pricedType.put("ridePriceMultiplier", rideFeeMultiplier((String) type.get("typeName")));
            result.add(pricedType);
        }
        return result;
    }

    /** "How many seats remain right now?" for every ride on a date. */
    public List<Map<String, Object>> availability(LocalDate date) {
        if (date == null) throw ApiException.badRequest("Choose a date.");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> ride : rides.listRides()) {
            Map<String, Object> copy = new LinkedHashMap<>(ride);
            copy.put("slots", rides.slotsForRideOnDate(((Number) ride.get("rideId")).intValue(), date));
            out.add(copy);
        }
        return out;
    }

    // ------------------------------------------------- ticket + ride booking

    /** Buy tickets for a visit date and (optionally) reserve a ride slot in the same step. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> purchase(int visitorId, Requests.Purchase req) {
        if (req == null || req.ticketTypeId() == null) throw ApiException.badRequest("Select a ticket type.");
        LocalDate today = LocalDate.now();

        // Edge 4: visit date must be today..window
        validateVisitDate(req.visitDate(), today);
        // Edge 5: quantity validation (before touching the database)
        int qty = validateQuantity(req.quantity(), "Ticket quantity");
        String mode = validatePayment(req.paymentMethod());

        Map<String, Object> type = bookings.findTicketType(req.ticketTypeId())
                .orElseThrow(() -> ApiException.badRequest("Unknown ticket type."));
        BigDecimal ticketPrice = (BigDecimal) type.get("price");
        BigDecimal rideMultiplier = rideFeeMultiplier((String) type.get("typeName"));

        BigDecimal total = ticketPrice.multiply(BigDecimal.valueOf(qty));
        RideRow ride = null;
        SlotRow slot = null;
        if (req.slotId() != null) {
            // Edges 3, 1, 2, 8: ride status -> duplicate -> capacity (slot row is locked)
            Object[] checked = checkAndLockSlot(visitorId, req.slotId(), req.visitDate(), qty, today);
            ride = (RideRow) checked[0];
            slot = (SlotRow) checked[1];
            total = total.add(rideUnitPrice(ride, rideMultiplier).multiply(BigDecimal.valueOf(qty)));
        }

        // Edge 6: payment fails after the seat is held -> exception rolls the whole transaction back,
        // so the hold is released and nothing is saved.
        if (Boolean.TRUE.equals(req.simulateFailure())) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Payment failed. Your seat hold has been released and you were not charged.");
        }

        int txId = bookings.insertTransaction(total, mode, today);
        int purchaseId = bookings.insertPurchase(today, mode, total, visitorId, txId);
        List<Integer> ticketIds = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            ticketIds.add(bookings.insertTicket((String) type.get("typeName"), ticketPrice, today,
                    req.visitDate(), visitorId, purchaseId));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchaseId", purchaseId);
        result.put("transactionId", txId);
        result.put("ticketIds", ticketIds);
        result.put("totalAmount", total);
        result.put("visitDate", req.visitDate().toString());
        if (slot != null) {
            BigDecimal rideAmount = rideUnitPrice(ride, rideMultiplier).multiply(BigDecimal.valueOf(qty));
            int bookingId = bookings.insertBooking(visitorId, slot.slotId(), purchaseId, qty, rideAmount);
            int remaining = slot.capacity() - rides.seatsBooked(slot.slotId());
            result.put("bookingId", bookingId);
            result.put("rideName", ride.rideName());
            result.put("slot", slot.startTime().toString());
            result.put("remainingSeats", Math.max(0, remaining));
        }
        return result;
    }

    private static BigDecimal rideFeeMultiplier(String ticketType) {
        return RIDE_FEE_MULTIPLIERS.getOrDefault(ticketType.toLowerCase(Locale.ROOT), BigDecimal.ONE);
    }

    private static BigDecimal rideUnitPrice(RideRow ride, BigDecimal multiplier) {
        return ride.price().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /** Reserve a ride using tickets the visitor already holds for that date. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> bookRide(int visitorId, int slotId, int seats, String paymentMethod) {
        LocalDate today = LocalDate.now();
        int qty = validateQuantity(seats, "Seats");
        String mode = validatePayment(paymentMethod);

        SlotRow preview = rides.findSlot(slotId, false)
                .orElseThrow(() -> ApiException.notFound("That slot does not exist."));
        int tickets = bookings.countTicketsForDate(visitorId, preview.slotDate());
        if (tickets < qty) {
            throw ApiException.badRequest("You hold " + tickets + " ticket(s) valid for " + preview.slotDate()
                    + ". Buy tickets for that date first.");
        }
        Object[] checked = checkAndLockSlot(visitorId, slotId, preview.slotDate(), qty, today);
        RideRow ride = (RideRow) checked[0];
        SlotRow slot = (SlotRow) checked[1];

        BigDecimal amount = ride.price().multiply(BigDecimal.valueOf(qty));
        int txId = bookings.insertTransaction(amount, mode, today);
        int purchaseId = bookings.insertPurchase(today, mode, amount, visitorId, txId);
        int bookingId = bookings.insertBooking(visitorId, slot.slotId(), purchaseId, qty, amount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookingId", bookingId);
        result.put("transactionId", txId);
        result.put("rideName", ride.rideName());
        result.put("totalAmount", amount);
        result.put("remainingSeats", Math.max(0, slot.capacity() - rides.seatsBooked(slot.slotId())));
        return result;
    }

    /** Runs all slot rules; returns {RideRow, SlotRow}. Leaves the slot row locked until commit. */
    private Object[] checkAndLockSlot(int visitorId, int slotId, LocalDate visitDate, int seats, LocalDate today) {
        // Lock order is always RIDE first, then SLOT (same as ride management) to avoid deadlocks.
        SlotRow preview = rides.findSlot(slotId, false)
                .orElseThrow(() -> ApiException.notFound("That slot does not exist."));
        RideRow ride = rides.findRide(preview.rideId(), true)
                .orElseThrow(() -> ApiException.notFound("That ride does not exist."));
        SlotRow slot = rides.findSlot(slotId, true)
                .orElseThrow(() -> ApiException.notFound("That slot does not exist."));
        if (!slot.slotDate().equals(visitDate)) {
            throw ApiException.badRequest("The selected slot is not on your visit date.");
        }

        // Edge 3: CLOSED / MAINTENANCE / archived blocks the booking before capacity is even checked
        if (ride.archived()) throw ApiException.notFound("That ride is no longer available.");
        if (!"OPEN".equals(ride.status())) {
            throw ApiException.conflict(ride.rideName() + " is currently " + ride.status() + " and cannot be booked.");
        }
        if (slot.slotDate().equals(today) && slot.startTime().isBefore(LocalTime.now())) {
            throw ApiException.badRequest("That slot has already started. Choose a later slot.");
        }
        // Edge 8: duplicate booking for the same visitor + slot
        if (bookings.hasActiveBooking(visitorId, slot.slotId())) {
            throw ApiException.conflict("You already have a booking for this slot.");
        }
        // Capacity check: capacity - confirmed = available
        int booked = rides.seatsBooked(slot.slotId());
        int available = Math.max(0, slot.capacity() - booked);
        if (seats > available) {
            throw ApiException.conflict("Insufficient capacity. Only " + available + " seat(s) left for this slot.");
        }
        return new Object[]{ride, slot};
    }

    // ---------------------------------------------------------- cancellation

    /** Edge 9: cancel after confirmation -> seat returned to capacity, refund recorded. */
    @Transactional
    public Map<String, Object> cancelBooking(int bookingId, Integer visitorIdOrNull) {
        BookingRow b = bookings.lockBooking(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found."));
        if (visitorIdOrNull != null && b.visitorId() != visitorIdOrNull) {
            throw ApiException.notFound("Booking not found.");
        }
        if ("CANCELLED".equals(b.status())) throw ApiException.conflict("This booking is already cancelled.");
        if (visitorIdOrNull != null && b.slotDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Past bookings cannot be cancelled.");
        }
        int refundTx = bookings.insertTransaction(b.amount().negate(), "REFUND", LocalDate.now());
        bookings.markCancelled(bookingId, refundTx);   // status no longer counts toward capacity
        return Map.of("bookingId", bookingId, "refundTransactionId", refundTx, "refundAmount", b.amount());
    }

    // ----------------------------------------------------------- visitor data

    public List<Map<String, Object>> myBookings(int visitorId) { return bookings.bookingsForVisitor(visitorId); }
    public List<Map<String, Object>> myTickets(int visitorId)   { return bookings.ticketsForVisitor(visitorId, LocalDate.now()); }
    public List<Map<String, Object>> myPurchases(int visitorId) { return bookings.purchasesForVisitor(visitorId); }
    public List<Map<String, Object>> allBookings()              { return bookings.allBookings(); }

    public void addFeedback(int visitorId, Requests.Feedback f) {
        if (f == null || f.rating() == null || f.rating() < 1 || f.rating() > 5) {
            throw ApiException.badRequest("Rating must be between 1 and 5.");
        }
        String text = f.text() == null ? "" : f.text().trim();
        if (text.length() > 500) throw ApiException.badRequest("Feedback is too long (max 500 characters).");
        if (f.rideId() != null && rides.findRide(f.rideId(), false).isEmpty()) {
            throw ApiException.badRequest("Unknown ride.");
        }
        bookings.insertFeedback(visitorId, f.rideId(), f.rating(), text, LocalDate.now());
    }

    // ------------------------------------------------------------ validation

    private static void validateVisitDate(LocalDate d, LocalDate today) {
        if (d == null) throw ApiException.badRequest("Choose a visit date.");
        if (d.isBefore(today)) throw ApiException.badRequest("Visit date cannot be in the past.");
        if (d.isAfter(today.plusDays(BOOKING_WINDOW_DAYS))) {
            throw ApiException.badRequest("Visit date is outside the booking window (max " + BOOKING_WINDOW_DAYS + " days ahead).");
        }
    }

    private static int validateQuantity(Integer q, String label) {
        if (q == null || q < 1) throw ApiException.badRequest(label + " must be at least 1.");
        if (q > MAX_PER_VISITOR) throw ApiException.badRequest(label + " cannot exceed " + MAX_PER_VISITOR + " per visitor.");
        return q;
    }

    private static String validatePayment(String m) {
        String mode = m == null ? "" : m.trim().toUpperCase();
        if (!PAYMENT_MODES.contains(mode)) throw ApiException.badRequest("Choose a payment method.");
        return mode;
    }
}
