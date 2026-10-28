package com.themepark.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** All request bodies in one place. */
public final class Requests {
    private Requests() {}

    public record Register(String name, String gender, LocalDate dob, String phone, String email, String password) {}
    public record Login(String email, String password) {}

    public record Purchase(Integer ticketTypeId, Integer quantity, LocalDate visitDate, Integer slotId,
                           String paymentMethod, Boolean simulateFailure) {}
    public record Feedback(Integer rideId, Integer rating, String text) {}

    public record RideSave(String rideName, String type, Integer capacity, BigDecimal price, Integer locationId) {}
    public record StatusChange(String status, String note) {}
    public record SlotSave(LocalDate slotDate, LocalTime startTime, LocalTime endTime, Integer capacity) {}
    public record LogEntry(String operation, String details, LocalDateTime logTime) {}
}
