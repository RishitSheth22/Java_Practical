package com.themepark.controller;

import com.themepark.dto.Requests;
import com.themepark.service.BookingService;
import com.themepark.util.Auth;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Visitor endpoints: every call needs a visitor session (30 min timeout). */
@RestController
@RequestMapping("/api/visitor")
public class VisitorController {
    private final BookingService bookings;

    public VisitorController(BookingService bookings) {
        this.bookings = bookings;
    }

    /** Ticket purchase (+ optional ride slot) in one step. */
    @PostMapping("/purchase")
    public Map<String, Object> purchase(@RequestBody Requests.Purchase req, HttpSession session) {
        return bookings.purchase(Auth.visitor(session), req);
    }

    /** Book a ride with tickets already owned: body {slotId, seats, paymentMethod}. */
    @PostMapping("/bookings")
    public Map<String, Object> book(@RequestBody Map<String, Object> body, HttpSession session) {
        int visitorId = Auth.visitor(session);
        Object slot = body.get("slotId");
        Object seats = body.get("seats");
        if (!(slot instanceof Number) || !(seats instanceof Number)) {
            throw com.themepark.util.ApiException.badRequest("slotId and seats are required.");
        }
        return bookings.bookRide(visitorId, ((Number) slot).intValue(), ((Number) seats).intValue(),
                body.get("paymentMethod") == null ? null : body.get("paymentMethod").toString());
    }

    @GetMapping("/bookings")
    public List<Map<String, Object>> myBookings(HttpSession session) {
        return bookings.myBookings(Auth.visitor(session));
    }

    @PostMapping("/bookings/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable int id, HttpSession session) {
        return bookings.cancelBooking(id, Auth.visitor(session));
    }

    @GetMapping("/tickets")
    public List<Map<String, Object>> myTickets(HttpSession session) {
        return bookings.myTickets(Auth.visitor(session));
    }

    @GetMapping("/purchases")
    public List<Map<String, Object>> myPurchases(HttpSession session) {
        return bookings.myPurchases(Auth.visitor(session));
    }

    @PostMapping("/feedback")
    public Map<String, Object> feedback(@RequestBody Requests.Feedback f, HttpSession session) {
        bookings.addFeedback(Auth.visitor(session), f);
        return Map.of("ok", true);
    }
}
