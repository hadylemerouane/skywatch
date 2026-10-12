package fr.skywatch.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** A batch of positions sent by the collector: {"positions": [...]}. */
public record PositionBatch(@NotNull List<@Valid Position> positions) {
}
