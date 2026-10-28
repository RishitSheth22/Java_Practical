package com.themepark.model;

import java.math.BigDecimal;

public record RideRow(int rideId, String rideName, String status, int capacity, BigDecimal price, boolean archived) {}
