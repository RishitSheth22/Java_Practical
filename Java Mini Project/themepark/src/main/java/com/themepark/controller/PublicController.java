package com.themepark.controller;

import com.themepark.service.BookingService;
import com.themepark.service.RideService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Anyone can check rides and availability without logging in. */
@RestController
@RequestMapping("/api")
public class PublicController {
    private final BookingService bookings;
    private final RideService rides;

    public PublicController(BookingService bookings, RideService rides) {
        this.bookings = bookings;
        this.rides = rides;
    }

    @GetMapping("/ticket-types")
    public List<Map<String, Object>> ticketTypes() {
        return bookings.ticketTypes();
    }

    @GetMapping("/rides")
    public List<Map<String, Object>> rides() {
        return rides.listRides();
    }

    @GetMapping("/availability")
    public List<Map<String, Object>> availability(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookings.availability(date);
    }
}
