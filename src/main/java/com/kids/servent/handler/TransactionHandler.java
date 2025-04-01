package com.kids.servent.handler;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;

public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);


			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
