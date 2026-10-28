package com.themepark.config;

import com.themepark.model.RideRow;
import com.themepark.repository.RideRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/** On startup, make sure every ride has slots for the next 7 days (safe to run repeatedly). */
@Component
public class SlotSeeder implements CommandLineRunner {
    private final RideRepository rides;
    private final boolean enabled;

    public SlotSeeder(RideRepository rides, @Value("${app.seed-slots:true}") boolean enabled) {
        this.rides = rides;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        LocalTime[] starts = {LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(16, 0)};
        LocalDate today = LocalDate.now();
        for (RideRow ride : rides.allActiveRides()) {
            if (ride.capacity() <= 0) continue;
            for (int d = 0; d < 7; d++) {
                for (LocalTime s : starts) {
                    rides.insertSlotIfMissing(ride.rideId(), today.plusDays(d), s, s.plusHours(1), ride.capacity());
                }
            }
        }
    }
}
