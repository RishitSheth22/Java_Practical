package com.themepark.repository;

import com.themepark.model.RideRow;
import com.themepark.model.SlotRow;
import com.themepark.util.Db;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Rides, slots and the ride log. */
@Repository
public class RideRepository {
    private final JdbcTemplate jdbc;

    public RideRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- rides

    private static final String RIDE_COLS =
            "SELECT RideID, RideName, Status, Capacity, Price, Archived FROM RIDE WHERE RideID = ?";

    public Optional<RideRow> findRide(int rideId, boolean forUpdate) {
        List<RideRow> rows = jdbc.query(RIDE_COLS + (forUpdate ? " FOR UPDATE" : ""),
                (rs, i) -> new RideRow(rs.getInt("RideID"), rs.getString("RideName"), rs.getString("Status"),
                        rs.getInt("Capacity"), rs.getBigDecimal("Price"), rs.getInt("Archived") == 1),
                rideId);
        return rows.stream().findFirst();
    }

    public List<Map<String, Object>> listRides() {
        return jdbc.queryForList(
                "SELECT r.RideID AS rideId, r.RideName AS rideName, r.Type AS type, r.Capacity AS capacity, "
              + "r.Price AS price, r.Status AS status, r.LocationID AS locationId, l.LocationName AS location "
              + "FROM RIDE r LEFT JOIN LOCATION l ON l.LocationID = r.LocationID "
              + "WHERE r.Archived = 0 ORDER BY r.RideName");
    }

    public List<Map<String, Object>> listLocations() {
        return jdbc.queryForList(
                "SELECT LocationID AS locationId, LocationName AS locationName FROM LOCATION ORDER BY LocationName");
    }

    public int insertRide(String name, String type, int capacity, BigDecimal price, Integer locationId) {
        return Db.insert(jdbc,
                "INSERT INTO RIDE (RideName, Type, Capacity, Price, LocationID, Status, Archived) VALUES (?,?,?,?,?,'CLOSED',0)",
                name, type, capacity, price, locationId);
    }

    public void updateRide(int rideId, String name, String type, int capacity, BigDecimal price, Integer locationId) {
        jdbc.update("UPDATE RIDE SET RideName=?, Type=?, Capacity=?, Price=?, LocationID=? WHERE RideID=?",
                name, type, capacity, price, locationId, rideId);
    }

    public void setStatus(int rideId, String status) {
        jdbc.update("UPDATE RIDE SET Status = ? WHERE RideID = ?", status, rideId);
    }

    public void archive(int rideId) {
        jdbc.update("UPDATE RIDE SET Archived = 1 WHERE RideID = ?", rideId);
    }

    /** New ride capacity applies to future slots only; existing bookings are honoured. */
    public void setFutureSlotCapacity(int rideId, int capacity, LocalDate today) {
        jdbc.update("UPDATE RIDE_SLOT SET Capacity = ? WHERE RideID = ? AND SlotDate >= ?", capacity, rideId, today);
    }

    // ---------------------------------------------------------------- slots

    private static final String SLOT_SELECT =
            "SELECT s.SlotID AS slotId, s.RideID AS rideId, DATE_FORMAT(s.SlotDate,'%Y-%m-%d') AS slotDate, "
          + "TIME_FORMAT(s.StartTime,'%H:%i') AS startTime, TIME_FORMAT(s.EndTime,'%H:%i') AS endTime, "
          + "s.Capacity AS capacity, "
          + "COALESCE(SUM(CASE WHEN b.Status IN ('CONFIRMED','REVIEW') THEN b.Seats END),0) AS booked "
          + "FROM RIDE_SLOT s LEFT JOIN RIDE_BOOKING b ON b.SlotID = s.SlotID ";
    private static final String SLOT_GROUP =
            "GROUP BY s.SlotID, s.RideID, s.SlotDate, s.StartTime, s.EndTime, s.Capacity ";

    /** Slots with seats-left calculated (capacity minus confirmed bookings). */
    public List<Map<String, Object>> slotsForRideOnDate(int rideId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                SLOT_SELECT + "WHERE s.RideID = ? AND s.SlotDate = ? " + SLOT_GROUP + "ORDER BY s.StartTime",
                rideId, date);
        addAvailable(rows);
        return rows;
    }

    public List<Map<String, Object>> upcomingSlots(int rideId, LocalDate from) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                SLOT_SELECT + "WHERE s.RideID = ? AND s.SlotDate >= ? " + SLOT_GROUP + "ORDER BY s.SlotDate, s.StartTime",
                rideId, from);
        addAvailable(rows);
        return rows;
    }

    private static void addAvailable(List<Map<String, Object>> rows) {
        for (Map<String, Object> r : rows) {
            int cap = Db.num(r.get("capacity"));
            int booked = Db.num(r.get("booked"));
            r.put("booked", booked);
            r.put("available", Math.max(0, cap - booked));
        }
    }

    public Optional<SlotRow> findSlot(int slotId, boolean forUpdate) {
        List<SlotRow> rows = jdbc.query(
                "SELECT SlotID, RideID, SlotDate, StartTime, Capacity FROM RIDE_SLOT WHERE SlotID = ?"
                        + (forUpdate ? " FOR UPDATE" : ""),
                (rs, i) -> new SlotRow(rs.getInt("SlotID"), rs.getInt("RideID"),
                        rs.getDate("SlotDate").toLocalDate(), rs.getTime("StartTime").toLocalTime(),
                        rs.getInt("Capacity")),
                slotId);
        return rows.stream().findFirst();
    }

    public int seatsBooked(int slotId) {
        Integer n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(Seats),0) FROM RIDE_BOOKING WHERE SlotID = ? AND Status IN ('CONFIRMED','REVIEW')",
                Integer.class, slotId);
        return n == null ? 0 : n;
    }

    public int insertSlot(int rideId, LocalDate date, LocalTime start, LocalTime end, int capacity) {
        return Db.insert(jdbc,
                "INSERT INTO RIDE_SLOT (RideID, SlotDate, StartTime, EndTime, Capacity) VALUES (?,?,?,?,?)",
                rideId, date, start, end, capacity);
    }

    public void updateSlot(int slotId, LocalTime start, LocalTime end, int capacity) {
        jdbc.update("UPDATE RIDE_SLOT SET StartTime=?, EndTime=?, Capacity=? WHERE SlotID=?", start, end, capacity, slotId);
    }

    public void deleteSlot(int slotId) {
        jdbc.update("DELETE FROM RIDE_SLOT WHERE SlotID = ?", slotId);
    }

    /** Used by the startup seeder; the unique key (RideID, SlotDate, StartTime) prevents duplicates. */
    public void insertSlotIfMissing(int rideId, LocalDate date, LocalTime start, LocalTime end, int capacity) {
        jdbc.update("INSERT IGNORE INTO RIDE_SLOT (RideID, SlotDate, StartTime, EndTime, Capacity) VALUES (?,?,?,?,?)",
                rideId, date, start, end, capacity);
    }

    public List<RideRow> allActiveRides() {
        return jdbc.query("SELECT RideID, RideName, Status, Capacity, Price, Archived FROM RIDE WHERE Archived = 0",
                (rs, i) -> new RideRow(rs.getInt("RideID"), rs.getString("RideName"), rs.getString("Status"),
                        rs.getInt("Capacity"), rs.getBigDecimal("Price"), false));
    }

    // ------------------------------------------------- bookings vs. ride state

    public int countActiveBookingsForSlot(int slotId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM RIDE_BOOKING WHERE SlotID = ? AND Status IN ('CONFIRMED','REVIEW')",
                Integer.class, slotId);
        return n == null ? 0 : n;
    }

    public int countActiveUpcomingBookings(int rideId, LocalDate today) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID "
              + "WHERE s.RideID = ? AND s.SlotDate >= ? AND b.Status IN ('CONFIRMED','REVIEW')",
                Integer.class, rideId, today);
        return n == null ? 0 : n;
    }

    /** Ride closed / in maintenance: flag upcoming confirmed bookings for review/refund. */
    public int flagUpcomingForReview(int rideId, LocalDate today) {
        return jdbc.update(
                "UPDATE RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID SET b.Status = 'REVIEW' "
              + "WHERE s.RideID = ? AND s.SlotDate >= ? AND b.Status = 'CONFIRMED'", rideId, today);
    }

    /** Ride reopened: bookings that were only waiting for review become confirmed again. */
    public int restoreReviewed(int rideId, LocalDate today) {
        return jdbc.update(
                "UPDATE RIDE_BOOKING b JOIN RIDE_SLOT s ON s.SlotID = b.SlotID SET b.Status = 'CONFIRMED' "
              + "WHERE s.RideID = ? AND s.SlotDate >= ? AND b.Status = 'REVIEW'", rideId, today);
    }

    // ------------------------------------------------------------- ride log

    public LocalDateTime latestLogTime(int rideId) {
        List<LocalDateTime> rows = jdbc.query("SELECT MAX(LogTime) AS t FROM RIDE_LOG WHERE RideID = ?",
                (rs, i) -> rs.getTimestamp("t") == null ? null : rs.getTimestamp("t").toLocalDateTime(), rideId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int insertLog(LocalDateTime time, String operation, int rideId, int staffId, String details) {
        return Db.insert(jdbc,
                "INSERT INTO RIDE_LOG (LogTime, OperationType, RideID, StaffID, Details) VALUES (?,?,?,?,?)",
                time, operation, rideId, staffId, details);
    }

    public List<Map<String, Object>> listLogs(Integer rideId) {
        String sql = "SELECT l.LogID AS logId, DATE_FORMAT(l.LogTime,'%Y-%m-%d %H:%i:%s') AS logTime, "
                   + "l.OperationType AS operation, l.Details AS details, r.RideName AS rideName, "
                   + "s.Name AS staffName, l.StaffID AS staffId "
                   + "FROM RIDE_LOG l JOIN RIDE r ON r.RideID = l.RideID JOIN STAFF s ON s.StaffID = l.StaffID "
                   + (rideId != null ? "WHERE l.RideID = ? " : "")
                   + "ORDER BY l.LogTime DESC, l.LogID DESC LIMIT 200";
        return rideId != null ? jdbc.queryForList(sql, rideId) : jdbc.queryForList(sql);
    }
}
