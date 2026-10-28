package com.themepark.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read-only queries for the admin dashboard. */
@Repository
public class ReportRepository {
    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** "What's today's total revenue?"  (refunds are negative, so this is net revenue) */
    public BigDecimal revenueOn(LocalDate date) {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(Amount),0) FROM `TRANSACTION` WHERE TransactionDate = ?", BigDecimal.class, date);
        return v == null ? BigDecimal.ZERO : v;
    }

    public int activeBookingsFrom(LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID "
              + "WHERE s.SlotDate >= ? AND b.Status IN ('CONFIRMED','REVIEW')", Integer.class, date);
        return n == null ? 0 : n;
    }

    public int bookingsNeedingReview() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM RIDE_BOOKING WHERE Status = 'REVIEW'", Integer.class);
        return n == null ? 0 : n;
    }

    /** "Which slots are overbooked?"  (only possible after a capacity reduction) */
    public List<Map<String, Object>> overbookedSlots() {
        return jdbc.queryForList(
                "SELECT s.SlotID AS slotId, r.RideName AS rideName, DATE_FORMAT(s.SlotDate,'%Y-%m-%d') AS slotDate, "
              + "TIME_FORMAT(s.StartTime,'%H:%i') AS startTime, s.Capacity AS capacity, SUM(b.Seats) AS booked "
              + "FROM RIDE_SLOT s JOIN RIDE r ON r.RideID = s.RideID "
              + "JOIN RIDE_BOOKING b ON b.SlotID = s.SlotID AND b.Status IN ('CONFIRMED','REVIEW') "
              + "GROUP BY s.SlotID, r.RideName, s.SlotDate, s.StartTime, s.Capacity HAVING SUM(b.Seats) > s.Capacity");
    }

    /** "What's the most popular ride?" */
    public List<Map<String, Object>> popularRides() {
        return jdbc.queryForList(
                "SELECT r.RideName AS rideName, COUNT(b.BookingID) AS bookings, COALESCE(SUM(b.Seats),0) AS seats "
              + "FROM RIDE r JOIN RIDE_SLOT s ON s.RideID = r.RideID "
              + "JOIN RIDE_BOOKING b ON b.SlotID = s.SlotID AND b.Status <> 'CANCELLED' "
              + "GROUP BY r.RideID, r.RideName ORDER BY seats DESC LIMIT 5");
    }

    /** "Which rides are under maintenance?" */
    public List<Map<String, Object>> ridesByStatus(String status) {
        return jdbc.queryForList(
                "SELECT RideID AS rideId, RideName AS rideName FROM RIDE WHERE Status = ? AND Archived = 0 ORDER BY RideName",
                status);
    }

    /** "Average rating per ride?" */
    public List<Map<String, Object>> averageRatings() {
        return jdbc.queryForList(
                "SELECT r.RideName AS rideName, ROUND(AVG(f.Rating),1) AS avgRating, COUNT(f.FeedbackID) AS reviews "
              + "FROM FEEDBACK f JOIN RIDE r ON r.RideID = f.RideID GROUP BY r.RideID, r.RideName ORDER BY avgRating DESC");
    }
}
