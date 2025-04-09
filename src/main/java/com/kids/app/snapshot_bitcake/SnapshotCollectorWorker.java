package com.kids.app.snapshot_bitcake;

import com.kids.app.AppConfig;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	private final AtomicBoolean collecting = new AtomicBoolean(false);
	private final Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();

	private final SnapshotType snapshotType;
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;

		switch(snapshotType) {
		case NONE:
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
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
			
			// 1 send messages
			switch (snapshotType) {
			case NONE:
				break;
			}
			
			// 2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case NONE:
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!working) return;
			}

			int sum;
			switch (snapshotType) {
			case NAIVE:
				sum = 0;
				for (Entry<String, Integer> itemAmount : collectedNaiveValues.entrySet()) {
					sum += itemAmount.getValue();
					AppConfig.timestampedStandardPrint(
							"Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

				// Reset for next invocation
				collectedNaiveValues.clear();
				break;

			case NONE:
				break;
			}
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
