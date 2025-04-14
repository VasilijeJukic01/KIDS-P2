package com.kids.app.snapshot_bitcake;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.ABSnapshotRequestMessage;
import com.kids.servent.message.util.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	private final Map<String, ABSnapshot> collectedAbValues = new ConcurrentHashMap<>();

	private final SnapshotType snapshotType;
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;

		switch(snapshotType) {
			case ACHARYA_BADRINATH -> bitcakeManager = new ABBitcakeManager();
			case NONE -> {
				AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
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
		return collectedAbValues;
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

			Map<Integer, Integer> vectorClock;
			Message request;

			// 1. Send request message
			switch (snapshotType) {
				case ACHARYA_BADRINATH -> {
					// Create SNAPSHOT_REQUEST message
					vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
					request = new ABSnapshotRequestMessage(AppConfig.myServentInfo, null, null, vectorClock);

					// Send SNAPSHOT_REQUEST message to all neighbors
					for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
						request = request.changeReceiver(neighbor);
						MessageUtil.sendMessage(request);
					}

					// Save the current state
					ABSnapshot snapshotResult = new ABSnapshot(
							AppConfig.myServentInfo.id(),
							bitcakeManager.getCurrentBitcakeAmount(),
							CausalBroadcast.getSent(),
							CausalBroadcast.getReceived()
					);
					collectedAbValues.put("node " + AppConfig.myServentInfo.id(), snapshotResult);

					CausalBroadcast.causalClockIncrement(request);
				}
				case NONE -> System.out.println("Error snapshot type is null");
			}

			// 2. Wait for all the responses
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
					case ACHARYA_BADRINATH -> {
						// We have collected all the responses
						if (collectedAbValues.size() == AppConfig.getServentCount()) {
							waiting = false;
						}
					}
					case NONE -> System.out.println("Error snapshot type is null");
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (!working) return;
			}

			int sum = 0;
			// 3. Print result
			switch (snapshotType) {
				case ACHARYA_BADRINATH -> {
					for (Entry<String, ABSnapshot> result : collectedAbValues.entrySet()) {
						boolean exist = false;
						int bitCakeAmount = result.getValue().getAmount();
						List<Message> sentTransactions = result.getValue().getSent();

						sum += bitCakeAmount;
						AppConfig.timestampedStandardPrint("Snapshot for " + result.getKey() + " = " + bitCakeAmount + " bitcake");

						// Check for unprocessed transactions
						for (Message sentTransaction : sentTransactions) {
							ABSnapshot abSnapshotResult = collectedAbValues.get("node " + sentTransaction.getOriginalReceiverInfo().id());
							List<Message> receivedTransactions = abSnapshotResult.getReceived();

							for (Message receivedTransaction : receivedTransactions) {
								if (sentTransaction.getMessageId() == receivedTransaction.getMessageId() &&
										sentTransaction.getOriginalSenderInfo().id() == receivedTransaction.getOriginalSenderInfo().id() &&
										sentTransaction.getOriginalReceiverInfo().id() == receivedTransaction.getOriginalReceiverInfo().id()) {
									exist = true;
									break;
								}
							}

							if (!exist) {
								AppConfig.timestampedStandardPrint("Info for unprocessed transaction: " + sentTransaction.getMessageText() + " bitcake");
								int amountNumber = Integer.parseInt(sentTransaction.getMessageText());
								sum += amountNumber;
							}
						}
					}

					AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
					collectedAbValues.clear();
				}
				case NONE -> System.out.println("Error snapshot type is null");
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
