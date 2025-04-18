package com.kids.app.snapshot_bitcake.snapshot_collector;

import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCSnapshot;

/**
 * Interface for collecting snapshots with the Coordinated Checkpointing algorithm.
 */
public interface CCCollector {
    void addCCSnapshotInfo(int id, CCSnapshot ccSnapshot);
    int getCollectedCCSize();
} 