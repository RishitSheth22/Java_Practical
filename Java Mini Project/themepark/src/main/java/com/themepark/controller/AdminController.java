package com.themepark.controller;

import com.themepark.dto.Requests;
import com.themepark.service.BookingService;
import com.themepark.service.ReportService;
import com.themepark.service.RideService;
import com.themepark.util.Auth;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin / staff endpoints: every call needs a staff session. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final RideService rides;
    private final BookingService bookings;
    private final ReportService reports;

    public AdminController(RideService rides, BookingService bookings, ReportService reports) {
        this.rides = rides;
        this.bookings = bookings;
        this.reports = reports;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpSession s) {
        Auth.staff(s);
        return reports.dashboard();
    }

    @GetMapping("/locations")
    public List<Map<String, Object>> locations(HttpSession s) {
        Auth.staff(s);
        return rides.listLocations();
    }

    // ---- rides
    @GetMapping("/rides")
    public List<Map<String, Object>> rides(HttpSession s) {
        Auth.staff(s);
        return rides.listRides();
    }

    @PostMapping("/rides")
    public Map<String, Object> createRide(@RequestBody Requests.RideSave r, HttpSession s) {
        return rides.createRide(Auth.staff(s), r);
    }

    @PutMapping("/rides/{id}")
    public Map<String, Object> updateRide(@PathVariable int id, @RequestBody Requests.RideSave r, HttpSession s) {
        rides.updateRide(Auth.staff(s), id, r);
        return Map.of("ok", true);
    }

    @PostMapping("/rides/{id}/status")
    public Map<String, Object> status(@PathVariable int id, @RequestBody Requests.StatusChange c, HttpSession s) {
        return rides.changeStatus(Auth.staff(s), id, c);
    }

    @DeleteMapping("/rides/{id}")
    public Map<String, Object> archive(@PathVariable int id, HttpSession s) {
        rides.archiveRide(Auth.staff(s), id);
        return Map.of("ok", true);
    }

    // ---- slots
    @GetMapping("/rides/{id}/slots")
    public List<Map<String, Object>> slots(@PathVariable int id, HttpSession s) {
        Auth.staff(s);
        return rides.upcomingSlots(id);
    }

    @PostMapping("/rides/{id}/slots")
    public Map<String, Object> addSlot(@PathVariable int id, @RequestBody Requests.SlotSave r, HttpSession s) {
        return rides.addSlot(Auth.staff(s), id, r);
    }

    @PutMapping("/slots/{slotId}")
    public Map<String, Object> updateSlot(@PathVariable int slotId, @RequestBody Requests.SlotSave r, HttpSession s) {
        rides.updateSlot(Auth.staff(s), slotId, r);
        return Map.of("ok", true);
    }

    @DeleteMapping("/slots/{slotId}")
    public Map<String, Object> deleteSlot(@PathVariable int slotId, HttpSession s) {
        rides.deleteSlot(Auth.staff(s), slotId);
        return Map.of("ok", true);
    }

    // ---- ride log
    @GetMapping("/log")
    public List<Map<String, Object>> log(@RequestParam(value = "rideId", required = false) Integer rideId, HttpSession s) {
        Auth.staff(s);
        return rides.logs(rideId);
    }

    @PostMapping("/rides/{id}/log")
    public Map<String, Object> addLog(@PathVariable int id, @RequestBody Requests.LogEntry e, HttpSession s) {
        rides.addManualLog(Auth.staff(s), id, e);
        return Map.of("ok", true);
    }

    // ---- bookings
    @GetMapping("/bookings")
    public List<Map<String, Object>> bookings(HttpSession s) {
        Auth.staff(s);
        return bookings.allBookings();
    }

    @PostMapping("/bookings/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable int id, HttpSession s) {
        Auth.staff(s);
        return bookings.cancelBooking(id, null);
    }
}
