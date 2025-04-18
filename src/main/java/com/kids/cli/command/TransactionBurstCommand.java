package com.kids.cli.command;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.servent.ServentInfo;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.TransactionMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes a burst of random transactions by creating multiple worker threads.
 * Each worker picks a different receiving node and sends a transaction message with a randomly selected amount.
 */
@RequiredArgsConstructor
public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 5;
	private static final int MAX_TRANSFER_AMOUNT = 5;

	private final SnapshotCollector snapshotCollector;
	private final Object lock = new Object();

	private class TransactionBurstWorker implements Runnable {

		@Override
		public void run() {
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				int amount = 1 + (int) (Math.random() * MAX_TRANSFER_AMOUNT);
				Message transaction;

				if (AppConfig.IS_FIFO) {
					for (int neighbor : AppConfig.myServentInfo.neighbors()) {
						ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);

						Message transactionMessage = new TransactionMessage(
								AppConfig.myServentInfo,
								neighborInfo,
								neighborInfo,
								amount,
								snapshotCollector.getBitcakeManager(),
								null
						);

						MessageUtil.sendMessage(transactionMessage);
					}
				}
				else {
					synchronized (lock) {
						ServentInfo receiverInfo = AppConfig.getInfoById((int) (Math.random() * AppConfig.getServentCount()));

						// Choose a random receiver that is not us
						while (receiverInfo.id() == AppConfig.myServentInfo.id()) {
							receiverInfo = AppConfig.getInfoById((int) (Math.random() * AppConfig.getServentCount()));
						}

						Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());

						transaction = new TransactionMessage(
								AppConfig.myServentInfo,
								receiverInfo,
								null,
								amount,
								snapshotCollector.getBitcakeManager(),
								vectorClock
						);

						if (snapshotCollector.getBitcakeManager() instanceof ABBitcakeManager) {
							CausalBroadcast.addSentMessage(transaction);
						}

						// Deduct the amount and send the message
						transaction.sendEffect();
						CausalBroadcast.causalClockIncrement(transaction);
					}

					AppConfig.myServentInfo.neighbors()
							.forEach(neighbor -> MessageUtil.sendMessage(transaction.changeReceiver(neighbor).makeMeASender()));
				}
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			t.start();
		}
	}

}
