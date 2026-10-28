package com.themepark.repository;

import com.themepark.model.BookingRow;
import com.themepark.util.Db;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Tickets, purchases, payments (TRANSACTION) and ride bookings. */
@Repository
public class BookingRepository {
    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> ticketTypes() {
        return jdbc.queryForList(
                "SELECT TypeID AS typeId, TypeName AS typeName, Price AS price, Description AS description "
              + "FROM TICKET_TYPE ORDER BY Price");
    }

    public Optional<Map<String, Object>> findTicketType(int typeId) {
        return jdbc.queryForList(
                "SELECT TypeID AS typeId, TypeName AS typeName, Price AS price FROM TICKET_TYPE WHERE TypeID = ?",
                typeId).stream().findFirst();
    }

    public int insertTransaction(BigDecimal amount, String method, LocalDate date) {
        return Db.insert(jdbc,
                "INSERT INTO `TRANSACTION` (Amount, PaymentMethod, TransactionDate) VALUES (?,?,?)",
                amount, method, date);
    }

    public int insertPurchase(LocalDate date, String mode, BigDecimal total, int visitorId, int transactionId) {
        return Db.insert(jdbc,
                "INSERT INTO TICKET_PURCHASE (PurchaseDate, PaymentMode, TotalAmount, VisitorID, TransactionID) VALUES (?,?,?,?,?)",
                date, mode, total, visitorId, transactionId);
    }

    public int insertTicket(String type, BigDecimal price, LocalDate issue, LocalDate validTo, int visitorId, int purchaseId) {
        return Db.insert(jdbc,
                "INSERT INTO TICKET (TicketType, Price, IssueDate, ValidToDate, VisitorID, PurchaseID) VALUES (?,?,?,?,?,?)",
                type, price, issue, validTo, visitorId, purchaseId);
    }

    public int insertBooking(int visitorId, int slotId, int purchaseId, int seats, BigDecimal amount) {
        return Db.insert(jdbc,
                "INSERT INTO RIDE_BOOKING (VisitorID, SlotID, PurchaseID, Seats, Amount, Status) VALUES (?,?,?,?,?,'CONFIRMED')",
                visitorId, slotId, purchaseId, seats, amount);
    }

    public boolean hasActiveBooking(int visitorId, int slotId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM RIDE_BOOKING WHERE VisitorID = ? AND SlotID = ? AND Status IN ('CONFIRMED','REVIEW')",
                Integer.class, visitorId, slotId);
        return n != null && n > 0;
    }

    public Optional<BookingRow> lockBooking(int bookingId) {
        return jdbc.query(
                "SELECT b.BookingID, b.VisitorID, b.SlotID, b.Seats, b.Amount, b.Status, s.SlotDate "
              + "FROM RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID WHERE b.BookingID = ? FOR UPDATE",
                (rs, i) -> new BookingRow(rs.getInt("BookingID"), rs.getInt("VisitorID"), rs.getInt("SlotID"),
                        rs.getInt("Seats"), rs.getBigDecimal("Amount"), rs.getString("Status"),
                        rs.getDate("SlotDate").toLocalDate()),
                bookingId).stream().findFirst();
    }

    public void markCancelled(int bookingId, int refundTransactionId) {
        jdbc.update("UPDATE RIDE_BOOKING SET Status = 'CANCELLED', RefundTransactionID = ? WHERE BookingID = ?",
                refundTransactionId, bookingId);
    }

    // ------------------------------------------------------- visitor views

    private static final String BOOKING_SELECT =
            "SELECT b.BookingID AS bookingId, r.RideName AS rideName, "
          + "DATE_FORMAT(s.SlotDate,'%Y-%m-%d') AS slotDate, TIME_FORMAT(s.StartTime,'%H:%i') AS startTime, "
          + "TIME_FORMAT(s.EndTime,'%H:%i') AS endTime, b.Seats AS seats, b.Amount AS amount, b.Status AS status, "
          + "DATE_FORMAT(b.BookingDate,'%Y-%m-%d %H:%i') AS bookedOn, v.Name AS visitorName "
          + "FROM RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID JOIN RIDE r ON r.RideID = s.RideID "
          + "JOIN VISITOR v ON v.VisitorID = b.VisitorID ";

    public List<Map<String, Object>> bookingsForVisitor(int visitorId) {
        return jdbc.queryForList(BOOKING_SELECT + "WHERE b.VisitorID = ? ORDER BY b.BookingDate DESC, b.BookingID DESC",
                visitorId);
    }

    public List<Map<String, Object>> allBookings() {
        return jdbc.queryForList(BOOKING_SELECT + "ORDER BY b.BookingDate DESC, b.BookingID DESC LIMIT 300");
    }

    /** "Is my ticket still valid?" */
    public List<Map<String, Object>> ticketsForVisitor(int visitorId, LocalDate today) {
        return jdbc.queryForList(
                "SELECT TicketID AS ticketId, TicketType AS ticketType, Price AS price, "
              + "DATE_FORMAT(IssueDate,'%Y-%m-%d') AS issueDate, DATE_FORMAT(ValidToDate,'%Y-%m-%d') AS validTo, "
              + "CASE WHEN ValidToDate >= ? THEN 'VALID' ELSE 'EXPIRED' END AS validity "
              + "FROM TICKET WHERE VisitorID = ? ORDER BY ValidToDate DESC, TicketID DESC",
                today, visitorId);
    }

    public int countTicketsForDate(int visitorId, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM TICKET WHERE VisitorID = ? AND ValidToDate = ?", Integer.class, visitorId, date);
        return n == null ? 0 : n;
    }

    /** Purchase history with payment details. */
    public List<Map<String, Object>> purchasesForVisitor(int visitorId) {
        return jdbc.queryForList(
                "SELECT p.PurchaseID AS purchaseId, DATE_FORMAT(p.PurchaseDate,'%Y-%m-%d') AS purchaseDate, "
              + "p.PaymentMode AS paymentMode, p.TotalAmount AS totalAmount, p.TransactionID AS transactionId, "
              + "(SELECT COUNT(*) FROM TICKET t WHERE t.PurchaseID = p.PurchaseID) AS tickets "
              + "FROM TICKET_PURCHASE p WHERE p.VisitorID = ? ORDER BY p.PurchaseDate DESC, p.PurchaseID DESC",
                visitorId);
    }

    // ------------------------------------------------------------ feedback

    public void insertFeedback(int visitorId, Integer rideId, int rating, String text, LocalDate date) {
        jdbc.update("INSERT INTO FEEDBACK (FeedbackText, Rating, FeedbackDate, VisitorID, RideID) VALUES (?,?,?,?,?)",
                text, rating, date, visitorId, rideId);
    }
}
