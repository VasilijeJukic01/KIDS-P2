package com.kids.app.snapshot_bitcake.snapshot_strategy;

import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;

import java.util.Map;

public interface SnapshotStrategy {
    void initiateSnapshot();
    boolean isSnapshotComplete();
    void processCollectedData(Map<String, ABSnapshot> collectedData);
}
