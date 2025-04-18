package com.kids.app.snapshot_bitcake.coordinated_checkpointing;

import java.io.Serial;
import java.io.Serializable;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is recordedAmount.
 */
public record CCSnapshot(
        int serventId,
        int recordedAmount
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -2443515806440075979L;
} 