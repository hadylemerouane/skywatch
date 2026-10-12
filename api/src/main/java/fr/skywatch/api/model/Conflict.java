package fr.skywatch.api.model;

/** A loss of separation, current or predicted, between aircraft A and B. */
public record Conflict(
        String icao24A, String callsignA,
        String icao24B, String callsignB,
        double horizontalNm, double verticalFt,
        ConflictType type, double secondsToCpa) {
}
