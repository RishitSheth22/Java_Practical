package com.themepark.service;

import com.themepark.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportService {
    private final ReportRepository reports;

    public ReportService(ReportRepository reports) {
        this.reports = reports;
    }

    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("revenueToday", reports.revenueOn(today));
        m.put("upcomingBookings", reports.activeBookingsFrom(today));
        m.put("bookingsNeedingReview", reports.bookingsNeedingReview());
        m.put("overbookedSlots", reports.overbookedSlots());
        m.put("popularRides", reports.popularRides());
        m.put("underMaintenance", reports.ridesByStatus("MAINTENANCE"));
        m.put("closedRides", reports.ridesByStatus("CLOSED"));
        m.put("averageRatings", reports.averageRatings());
        return m;
    }
}
