package com.kids.servent.message.implementation;

import com.kids.app.servent.ServentInfo;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.servent.message.MessageType;

import java.io.Serial;
import java.util.Map;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	@Serial
	private static final long serialVersionUID = -333251402058492901L;

	private final transient BitcakeManager bitcakeManager;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, int amount, BitcakeManager bitcakeManager, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TRANSACTION, sender, receiver, neighbor, String.valueOf(amount), senderVectorClock);
		this.bitcakeManager = bitcakeManager;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		bitcakeManager.takeSomeBitcakes(amount);
	}
}
