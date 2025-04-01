package com.kids.cli.command;

import com.kids.app.AppConfig;
import com.kids.app.ServentInfo;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.servent.message.Message;
import com.kids.servent.message.TransactionMessage;
import com.kids.servent.message.util.MessageUtil;

import java.util.concurrent.ThreadLocalRandom;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 10;
	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	//Chandy-Lamport
	//	private static final int TRANSACTION_COUNT = 3;
	//	private static final int BURST_WORKERS = 5;
	//	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	private final BitcakeManager bitcakeManager;
	
	public TransactionBurstCommand(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);
					
					int amount = 1 + rand.nextInt(MAX_TRANSFER_AMOUNT);
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */
					Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo, neighborInfo, amount, bitcakeManager);
					MessageUtil.sendMessage(transactionMessage);
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
