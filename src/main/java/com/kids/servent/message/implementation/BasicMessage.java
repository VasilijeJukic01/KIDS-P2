package com.kids.servent.message.implementation;

import com.kids.app.AppConfig;
import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modification methods.
 * If you don't override the modification, you might drop stuff.
 * @author bmilojkovic
 *
 */
public class BasicMessage implements Message {

	@Serial
	private static final long serialVersionUID = -9075856313609777945L;

	private final MessageType type;
	private final ServentInfo originalSenderInfo;
	private final ServentInfo originalReceiverInfo;
	private final ServentInfo receiverInfo;
	private final List<ServentInfo> routeList;
	private final String messageText;
	private final Map<Integer, Integer> senderVectorClock;
	
	// This gives us a unique id - incremented in every natural constructor.
	private static final AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;

	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo originalReceiverInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.originalReceiverInfo = originalReceiverInfo;
		this.receiverInfo = receiverInfo;
		this.senderVectorClock =  senderVectorClock;
		this.routeList = new ArrayList<>();
		this.messageText = "";

		this.messageId = messageCounter.getAndIncrement();
	}

	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo originalReceiverInfo, ServentInfo receiverInfo, String messageText, Map<Integer, Integer> senderVectorClock) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.originalReceiverInfo = originalReceiverInfo;
		this.receiverInfo = receiverInfo;
		this.senderVectorClock =  senderVectorClock;
		this.routeList = new ArrayList<>();
		this.messageText = messageText;

		this.messageId = messageCounter.getAndIncrement();
	}

	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo originalReceiverInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, String messageText, int messageId) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.originalReceiverInfo = originalReceiverInfo;
		this.receiverInfo = receiverInfo;
		this.senderVectorClock =  senderVectorClock;
		this.routeList = routeList;
		this.messageText = messageText;
		this.messageId = messageId;
	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}

	@Override
	public ServentInfo getOriginalSenderInfo() {
		return originalSenderInfo;
	}

	@Override
	public ServentInfo getOriginalReceiverInfo() {
		return originalReceiverInfo;
	}

	@Override
	public ServentInfo getReceiverInfo() {
		return receiverInfo;
	}
	
	@Override
	public List<ServentInfo> getRoute() {
		return routeList;
	}
	
	@Override
	public String getMessageText() {
		return messageText;
	}
	
	@Override
	public int getMessageId() {
		return messageId;
	}
	
	/**
	 * Used when resending a message. It will not change the original owner
	 * (so equality is not affected), but will add us to the route list, so
	 * message path can be retraced later.
	 */
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(routeList);
		newRouteList.add(newRouteItem);

        return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
                getOriginalReceiverInfo(), getReceiverInfo(), getSenderVectorClock(),
				newRouteList, getMessageText(), getMessageId()
		);
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.neighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return new BasicMessage(getMessageType(), getOriginalSenderInfo(), getOriginalReceiverInfo(),
                    newReceiverInfo, getSenderVectorClock(), getRoute(), getMessageText(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			return null;
		}
	}
	
	/**
	 * Comparing messages is based on their unique id and the original sender id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage other) {
            return getMessageId() == other.getMessageId() &&
                    getOriginalSenderInfo().id() == other.getOriginalSenderInfo().id();
		}
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are going to keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getOriginalSenderInfo().id());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[Original Sender: " + getOriginalSenderInfo().id() + "|Message ID: " + getMessageId() + "|Content: " + getMessageText() +
				"|Type: " + getMessageType() + "|Receiver: " + (getReceiverInfo() != null ? getReceiverInfo().id() : null) + "|Original Receiver: " +
				(getOriginalReceiverInfo() != null ? getOriginalReceiverInfo().id() : null) + "]";
	}

	/**
	 * Empty implementation, which will be suitable for most messages.
	 */
	@Override
	public void sendEffect() {
		
	}

	@Override
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}

}
