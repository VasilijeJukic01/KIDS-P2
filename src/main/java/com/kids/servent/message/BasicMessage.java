package com.kids.servent.message;

import com.kids.app.AppConfig;
import com.kids.app.ServentInfo;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
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
	private final ServentInfo receiverInfo;
	private final List<ServentInfo> routeList;
	private final String messageText;
	private final boolean white;
	
	// This gives us a unique id - incremented in every natural constructor.
	private static final AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = AppConfig.isWhite.get();
		this.routeList = new ArrayList<>();
		this.messageText = "";
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, String messageText) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = AppConfig.isWhite.get();
		this.routeList = new ArrayList<>();
		this.messageText = messageText;
		
		this.messageId = messageCounter.getAndIncrement();
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
	public ServentInfo getReceiverInfo() {
		return receiverInfo;
	}
	
	@Override
	public boolean isWhite() {
		return white;
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
	
	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
						   boolean white, List<ServentInfo> routeList, String messageText, int messageId) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.white = white;
		this.routeList = routeList;
		this.messageText = messageText;
		this.messageId = messageId;
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
                getReceiverInfo(), isWhite(), newRouteList, getMessageText(), getMessageId());
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
                    newReceiverInfo, isWhite(), getRoute(), getMessageText(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			return null;
		}
	}
	
	@Override
	public Message setRedColor() {
        return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
                getReceiverInfo(), false, getRoute(), getMessageText(), getMessageId());
	}
	
	@Override
	public Message setWhiteColor() {
        return new BasicMessage(getMessageType(), getOriginalSenderInfo(),
                getReceiverInfo(), true, getRoute(), getMessageText(), getMessageId());
	}
	
	/**
	 * Comparing messages is based on their unique id and the original sender id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage other) {
            return getMessageId() == other.getMessageId() &&
                    getOriginalSenderInfo().getId() == other.getOriginalSenderInfo().getId();
		}
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are going to keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getOriginalSenderInfo().getId());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
					getMessageText() + "|" + getMessageType() + "|" +
					getReceiverInfo().getId() + "]";
	}

	/**
	 * Empty implementation, which will be suitable for most messages.
	 */
	@Override
	public void sendEffect() {
		
	}
}
