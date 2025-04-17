package com.kids.servent.handler.implementation;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.alagar_venkatesan.AVBitcakeManager;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);

			if (bitcakeManager instanceof ABBitcakeManager) {
				CausalBroadcast.addReceivedMessage(clientMessage);
			}
			else if (bitcakeManager instanceof AVBitcakeManager) {
				CausalBroadcast.recordTransaction(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().id(), amountNumber);
			}

			AppConfig.timestampedStandardPrint("Transaction handler got: " + clientMessage);
		}
	}

}
