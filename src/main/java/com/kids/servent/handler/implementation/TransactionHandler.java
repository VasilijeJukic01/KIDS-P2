package com.kids.servent.handler.implementation;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.alagar_venkatesan.AVBitcakeManager;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

/**
 * Handles the TRANSACTION message. This is a simple handler that adds the amount
 * to the BitcakeManager and then broadcasts the message to other nodes.
 *
 * @author bmilojkovic
 */
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
			
			// For Coordinated Checkpointing, we need to check if we are in snapshot mode
			if (bitcakeManager instanceof CCBitcakeManager ccBitcakeManager) {
				if (ccBitcakeManager.shouldQueueMessage(clientMessage)) {
					AppConfig.timestampedStandardPrint("Transaction queued due to active snapshot: " + clientMessage);
					return;
				}
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);

			CausalBroadcast instance = CausalBroadcast.getInstance();
			
			if (bitcakeManager instanceof ABBitcakeManager) {
				instance.addReceivedMessage(clientMessage);
			}
			else if (bitcakeManager instanceof AVBitcakeManager) {
				instance.recordTransaction(clientMessage.getSenderVectorClock(), clientMessage.getOriginalSenderInfo().id(), amountNumber);
			}

			AppConfig.timestampedStandardPrint("Transaction handler got: " + clientMessage);
		}
	}

}
