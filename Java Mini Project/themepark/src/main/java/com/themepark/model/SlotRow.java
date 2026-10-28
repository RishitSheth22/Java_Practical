package com.themepark.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotRow(int slotId, int rideId, LocalDate slotDate, LocalTime startTime, int capacity) {}
