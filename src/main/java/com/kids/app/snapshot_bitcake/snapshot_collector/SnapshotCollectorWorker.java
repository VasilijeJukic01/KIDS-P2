package com.kids.app.snapshot_bitcake.snapshot_collector;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.app.snapshot_bitcake.SnapshotType;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.app.snapshot_bitcake.alagar_venkatesan.AVBitcakeManager;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCSnapshot;
import com.kids.app.snapshot_bitcake.snapshot_strategy.ABSnapshotStrategy;
import com.kids.app.snapshot_bitcake.snapshot_strategy.AVSnapshotStrategy;
import com.kids.app.snapshot_bitcake.snapshot_strategy.CCSnapshotStrategy;
import com.kids.app.snapshot_bitcake.snapshot_strategy.SnapshotStrategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class that implements the SnapshotCollector functionality.
 * Supports multiple snapshot algorithms.
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
public class SnapshotCollectorWorker implements SnapshotCollector, ABCollector, AVCollector, CCCollector {

	private volatile boolean working = true;
	private final AtomicBoolean collecting = new AtomicBoolean(false);

	private final Map<String, ABSnapshot> collectedABData = new ConcurrentHashMap<>();
	private final List<Integer> collectedAVData = new CopyOnWriteArrayList<>();
	private final Map<Integer, CCSnapshot> collectedCCData = new ConcurrentHashMap<>();

	private BitcakeManager bitcakeManager;
	private SnapshotStrategy snapshotStrategy;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		switch(snapshotType) {
			case ACHARYA_BADRINATH -> {
				this.bitcakeManager = new ABBitcakeManager();
				this.snapshotStrategy = new ABSnapshotStrategy(collectedABData, (ABBitcakeManager) bitcakeManager);
			}
			case ALAGAR_VENKATESAN -> {
				this.bitcakeManager = new AVBitcakeManager();
				this.snapshotStrategy = new AVSnapshotStrategy(collectedAVData, (AVBitcakeManager) bitcakeManager);
			}
			case COORDINATED_CHECKPOINTING -> {
				this.bitcakeManager = new CCBitcakeManager();
				this.snapshotStrategy = new CCSnapshotStrategy(collectedCCData, (CCBitcakeManager) bitcakeManager, this);
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
	public void markAsDone(int id) {
		collectedAVData.add(id);
	}

	@Override
	public void clearAVData() {
		if (snapshotStrategy instanceof AVSnapshotStrategy) {
			((AVSnapshotStrategy) snapshotStrategy).setWait(false);
			collectedAVData.clear();
		}
	}

	@Override
	public void addCCSnapshotInfo(int id, CCSnapshot ccSnapshot) {
		collectedCCData.put(id, ccSnapshot);
	}
	
	@Override
	public int getCollectedCCSize() {
		return collectedCCData.size();
	}

	@Override
	public void run() {
		while(working) {
			
			// Not collecting yet - just sleep until we start actual work, or finish
			while (!collecting.get()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					AppConfig.timestampedErrorPrint("Interrupted while waiting to start collecting.");
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
			
			while (collecting.get()) {
				/*
				 * Wait for all the responses.
				 */
				if (snapshotStrategy.isSnapshotComplete()) {
					snapshotStrategy.processCollectedData();

					collectedABData.clear();
					collectedAVData.clear();
					collectedCCData.clear();

					collecting.set(false);
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					AppConfig.timestampedErrorPrint("Interrupted while waiting for snapshot to complete.");
				}
			}
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
