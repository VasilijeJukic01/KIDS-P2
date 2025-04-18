package com.kids.app.snapshot_bitcake.snapshot_strategy;

public interface SnapshotStrategy {
    void initiateSnapshot();
    boolean isSnapshotComplete();
    void processCollectedData();
}
