package com.kids.app.snapshot_bitcake.snapshot_collector;

import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;

import java.util.Map;

/**
 * This interface defines the contract for a collector that gathers snapshots of the Acharya-Badrinath algorithm.
 */
public interface ABCollector {
    Map<String, ABSnapshot> getCollectedABValues();
}
