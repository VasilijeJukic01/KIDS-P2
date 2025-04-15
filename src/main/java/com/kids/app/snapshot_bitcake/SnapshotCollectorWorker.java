package com.kids.app.snapshot_bitcake;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.app.snapshot_bitcake.snapshot_strategy.ABSnapshotStrategy;
import com.kids.app.snapshot_bitcake.snapshot_strategy.SnapshotStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class that implements the SnapshotCollector functionality.
 * Supports the Acharya-Badrinath snapshot algorithm.
 *
 * <p>
 * The snapshot collection consists of three main stages:
 * <ol>
 *   <li>Sending snapshot request messages to neighboring nodes.</li>
 *   <li>Waiting for snapshot responses.</li>
 *   <li>Aggregating and printing the snapshot results.</li>
 * </ol>
 * </p>
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	private final AtomicBoolean collecting = new AtomicBoolean(false);
	private final Map<String, ABSnapshot> collectedABData = new ConcurrentHashMap<>();

	private BitcakeManager bitcakeManager;
	private SnapshotStrategy snapshotStrategy;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		switch(snapshotType) {
			case ACHARYA_BADRINATH -> {
				bitcakeManager = new ABBitcakeManager();
				this.snapshotStrategy = new ABSnapshotStrategy(collectedABData, (ABBitcakeManager) bitcakeManager);
			}
			case NONE -> {
				AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
				this.snapshotStrategy = null;
				System.exit(0);
			}
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	@Override
	public Map<String, ABSnapshot> getCollectedABValues() {
		return collectedABData;
	}

	@Override
	public void run() {
		while(working) {
			
			// Not collecting yet - just sleep until we start actual work, or finish
			while (!collecting.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!working) return;
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */

			snapshotStrategy.initiateSnapshot();

			while (!snapshotStrategy.isSnapshotComplete()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!working) return;
			}

			snapshotStrategy.processCollectedData(collectedABData);
			collecting.set(false);
		}
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
