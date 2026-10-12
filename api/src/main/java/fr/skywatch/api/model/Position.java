package fr.skywatch.api.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** The latest known position of one aircraft, in aviation units. */
public record Position(
        @NotBlank @Size(max = 6) String icao24,
        @Size(max = 10) String callsign,
        @DecimalMin("-90") @DecimalMax("90") double latitude,
        @DecimalMin("-180") @DecimalMax("180") double longitude,
        double altitudeFt,
        @PositiveOrZero double groundSpeedKt,
        @DecimalMin("0") @DecimalMax("360") double trackDeg,
        double verticalRateFpm) {
}
