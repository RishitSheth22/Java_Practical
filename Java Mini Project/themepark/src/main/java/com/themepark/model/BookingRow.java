package com.themepark.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingRow(int bookingId, int visitorId, int slotId, int seats, BigDecimal amount,
                         String status, LocalDate slotDate) {}
