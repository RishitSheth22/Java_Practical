package com.themepark.service;

import com.themepark.dto.Requests;
import com.themepark.model.RideRow;
import com.themepark.model.SlotRow;
import com.themepark.repository.RideRepository;
import com.themepark.util.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CORE FUNCTION 2: ride management + ride log.
 * Every status change / slot change writes a RIDE_LOG row in the SAME transaction, so a change
 * can never exist without its log entry.
 */
@Service
public class RideService {
    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED", "MAINTENANCE");

    private final RideRepository rides;

    public RideService(RideRepository rides) {
        this.rides = rides;
    }

    public List<Map<String, Object>> listRides()     { return rides.listRides(); }
    public List<Map<String, Object>> listLocations() { return rides.listLocations(); }

    public List<Map<String, Object>> upcomingSlots(int rideId) {
        requireRide(rideId, false);
        return rides.upcomingSlots(rideId, LocalDate.now());
    }

    // ---------------------------------------------------------------- rides

    @Transactional
    public Map<String, Object> createRide(int staffId, Requests.RideSave r) {
        validateRide(r);
        int id = rides.insertRide(r.rideName().trim(), r.type(), r.capacity(),
                r.price() == null ? BigDecimal.ZERO : r.price(), r.locationId());
        writeLog(id, staffId, "CREATED", "Ride created with capacity " + r.capacity() + " (status CLOSED until opened)", null);
        return Map.of("rideId", id);
    }

    @Transactional
    public void updateRide(int staffId, int rideId, Requests.RideSave r) {
        validateRide(r);
        RideRow old = requireRide(rideId, true);
        // Edge 6: an OPEN ride must keep a valid capacity
        if ("OPEN".equals(old.status()) && r.capacity() <= 0) {
            throw ApiException.badRequest("An OPEN ride needs a capacity above zero. Close it first.");
        }
        BigDecimal price = r.price() == null ? BigDecimal.ZERO : r.price();
        rides.updateRide(rideId, r.rideName().trim(), r.type(), r.capacity(), price, r.locationId());
        if (old.capacity() != r.capacity()) {
            // Edge 2: existing bookings are honoured; only new requests see the new capacity
            if (r.capacity() > 0) rides.setFutureSlotCapacity(rideId, r.capacity(), LocalDate.now());
            writeLog(rideId, staffId, "CAPACITY_UPDATED",
                    "Capacity " + old.capacity() + " -> " + r.capacity() + " (existing bookings honoured)", null);
        } else {
            writeLog(rideId, staffId, "RIDE_UPDATED", "Ride details updated", null);
        }
    }

    /** OPEN / CLOSED / MAINTENANCE. */
    @Transactional
    public Map<String, Object> changeStatus(int staffId, int rideId, Requests.StatusChange c) {
        String next = c == null || c.status() == null ? "" : c.status().trim().toUpperCase();
        if (!STATUSES.contains(next)) throw ApiException.badRequest("Status must be OPEN, CLOSED or MAINTENANCE.");

        RideRow ride = requireRide(rideId, true);      // row lock => simultaneous updates are serialised
        if (ride.status().equals(next)) throw ApiException.badRequest("Ride is already " + next + ".");
        // Edge 6: reopening needs a valid capacity
        if ("OPEN".equals(next) && ride.capacity() <= 0) {
            throw ApiException.badRequest("Reopening blocked: set a capacity above zero first.");
        }

        LocalDate today = LocalDate.now();
        int flagged = 0;
        int restored = 0;
        if (!"OPEN".equals(next)) {
            // Edge 1: block new bookings (status) and flag existing upcoming bookings for review/refund
            flagged = rides.flagUpcomingForReview(rideId, today);
        } else {
            restored = rides.restoreReviewed(rideId, today);
        }
        rides.setStatus(rideId, next);

        String op = switch (next) {
            case "OPEN" -> "MAINTENANCE".equals(ride.status()) ? "RESUMED" : "OPENED";
            default -> next;
        };
        String note = c.note() == null ? "" : c.note().trim();
        String details = ride.status() + " -> " + next
                + (flagged > 0 ? "; " + flagged + " booking(s) flagged for review/refund" : "")
                + (restored > 0 ? "; " + restored + " booking(s) restored" : "")
                + (note.isEmpty() ? "" : "; " + note);
        writeLog(rideId, staffId, op, details, null);   // Edge 4: always logged, same transaction

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rideId", rideId);
        out.put("status", next);
        out.put("bookingsFlagged", flagged);
        return out;
    }

    @Transactional
    public void archiveRide(int staffId, int rideId) {
        requireRide(rideId, true);
        int active = rides.countActiveUpcomingBookings(rideId, LocalDate.now());
        if (active > 0) {
            throw ApiException.conflict("Cannot archive: " + active + " upcoming booking(s) exist. Cancel/refund them first.");
        }
        writeLog(rideId, staffId, "ARCHIVED", "Ride archived", null);
        rides.archive(rideId);
    }

    // ---------------------------------------------------------------- slots

    @Transactional
    public Map<String, Object> addSlot(int staffId, int rideId, Requests.SlotSave s) {
        RideRow ride = requireRide(rideId, true);
        if (s == null || s.slotDate() == null || s.startTime() == null || s.endTime() == null) {
            throw ApiException.badRequest("Date, start time and end time are required.");
        }
        if (s.slotDate().isBefore(LocalDate.now())) throw ApiException.badRequest("Slot date cannot be in the past.");
        if (!s.endTime().isAfter(s.startTime())) throw ApiException.badRequest("End time must be after start time.");
        int cap = s.capacity() == null ? ride.capacity() : s.capacity();
        if (cap <= 0) throw ApiException.badRequest("Slot capacity must be above zero.");
        try {
            int id = rides.insertSlot(rideId, s.slotDate(), s.startTime(), s.endTime(), cap);
            writeLog(rideId, staffId, "SLOT_ADDED", s.slotDate() + " " + s.startTime() + " (capacity " + cap + ")", null);
            return Map.of("slotId", id);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("A slot already exists for that ride, date and start time.");
        }
    }

    @Transactional
    public void updateSlot(int staffId, int slotId, Requests.SlotSave s) {
        SlotRow preview = rides.findSlot(slotId, false).orElseThrow(() -> ApiException.notFound("Slot not found."));
        requireRide(preview.rideId(), true);                       // lock order: ride, then slot
        SlotRow slot = rides.findSlot(slotId, true).orElseThrow(() -> ApiException.notFound("Slot not found."));
        if (s == null || s.startTime() == null || s.endTime() == null || s.capacity() == null) {
            throw ApiException.badRequest("Start time, end time and capacity are required.");
        }
        if (!s.endTime().isAfter(s.startTime())) throw ApiException.badRequest("End time must be after start time.");
        if (s.capacity() <= 0) throw ApiException.badRequest("Slot capacity must be above zero.");
        try {
            rides.updateSlot(slotId, s.startTime(), s.endTime(), s.capacity());
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("Another slot already starts at that time.");
        }
        writeLog(slot.rideId(), staffId, "SLOT_UPDATED",
                slot.slotDate() + " " + s.startTime() + " capacity " + slot.capacity() + " -> " + s.capacity(), null);
    }

    @Transactional
    public void deleteSlot(int staffId, int slotId) {
        SlotRow preview = rides.findSlot(slotId, false).orElseThrow(() -> ApiException.notFound("Slot not found."));
        requireRide(preview.rideId(), true);                       // lock order: ride, then slot
        SlotRow slot = rides.findSlot(slotId, true).orElseThrow(() -> ApiException.notFound("Slot not found."));
        if (rides.countActiveBookingsForSlot(slotId) > 0) {
            throw ApiException.conflict("This slot has active bookings and cannot be removed.");
        }
        writeLog(slot.rideId(), staffId, "SLOT_REMOVED", slot.slotDate() + " " + slot.startTime(), null);
        try {
            rides.deleteSlot(slotId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw ApiException.conflict("This slot has booking history and cannot be removed.");
        }
    }

    // ------------------------------------------------------------- ride log

    /** Manual log entry (inspection, cleaning, ...). */
    @Transactional
    public void addManualLog(int staffId, int rideId, Requests.LogEntry e) {
        if (e == null || e.operation() == null || e.operation().trim().isEmpty()) {
            throw ApiException.badRequest("Operation is required.");
        }
        String op = e.operation().trim().toUpperCase().replace(' ', '_');
        if (op.length() > 50) throw ApiException.badRequest("Operation name is too long.");
        writeLog(rideId, staffId, op, e.details(), e.logTime());
    }

    public List<Map<String, Object>> logs(Integer rideId) {
        return rides.listLogs(rideId);
    }

    /**
     * Single entry point for writing RIDE_LOG rows.
     * Edge 5: ride must exist and not be archived.  Edge 7: entries must stay chronological.
     */
    private void writeLog(int rideId, int staffId, String operation, String details, LocalDateTime when) {
        RideRow ride = rides.findRide(rideId, false)
                .orElseThrow(() -> ApiException.notFound("This ride no longer exists."));
        if (ride.archived() && !"ARCHIVED".equals(operation)) {
            throw ApiException.notFound("This ride no longer exists (archived). Operation rejected.");
        }
        LocalDateTime time = (when != null ? when : LocalDateTime.now()).withNano(0);
        LocalDateTime latest = rides.latestLogTime(rideId);
        if (latest != null && time.isBefore(latest)) {
            throw ApiException.conflict("Log entries must stay chronological. The latest entry for this ride is at " + latest + ".");
        }
        String d = details == null ? null : (details.length() > 255 ? details.substring(0, 255) : details);
        rides.insertLog(time, operation, rideId, staffId, d);
    }

    // -------------------------------------------------------------- helpers

    private RideRow requireRide(int rideId, boolean lock) {
        RideRow r = rides.findRide(rideId, lock).orElseThrow(() -> ApiException.notFound("Ride not found."));
        if (r.archived()) throw ApiException.notFound("This ride no longer exists (archived).");
        return r;
    }

    private static void validateRide(Requests.RideSave r) {
        if (r == null || r.rideName() == null || r.rideName().trim().isEmpty()) {
            throw ApiException.badRequest("Ride name is required.");
        }
        if (r.rideName().trim().length() > 50) throw ApiException.badRequest("Ride name is too long (max 50).");
        if (r.capacity() == null || r.capacity() < 0) throw ApiException.badRequest("Capacity must be zero or more.");
        if (r.price() != null && r.price().signum() < 0) throw ApiException.badRequest("Price cannot be negative.");
    }
}
