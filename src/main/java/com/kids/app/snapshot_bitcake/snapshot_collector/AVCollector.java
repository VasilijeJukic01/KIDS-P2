package com.kids.app.snapshot_bitcake.snapshot_collector;

/**
 * This interface defines the contract for a collector that gathers snapshots of the Alagar-Venkatesan algorithm.
 */
public interface AVCollector {
    void markAsDone(int id);
    void clearAVData();
}
