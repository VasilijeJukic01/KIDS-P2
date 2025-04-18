package com.kids.servent.message.util;

import com.kids.app.AppConfig;
import com.kids.app.Cancellable;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * This worker implements the sending of messages for FIFO causal broadcast.
 * <p>
 * It waits for objects from two concurrent queues, one with regular messages
 * the other with CL_REQUEST messages.
 * <p>
 * If a message is found in the marker queue, it is sent right away. If not,
 * we check that we are white, and then we check the regular messages queue.
 * 
 * @author bmilojkovic
 *
 */
@RequiredArgsConstructor
public class FifoSendWorker implements Runnable, Cancellable {

	private final int neighbor;
	private volatile boolean working = true;
	
	@Override
	public void run() {
		while (working) {
			try {
				// First check if there are any marker/control messages to send (high priority)
				Message messageToSend = MessageUtil.pendingMarkers.get(neighbor).poll(200, TimeUnit.MILLISECONDS);
				
				// If no marker/control messages, then check regular messages when we're in white state
				if (messageToSend == null) {
					if (AppConfig.isWhite.get()) {
						messageToSend = MessageUtil.pendingMessages.get(neighbor).poll(200, TimeUnit.MILLISECONDS);
					}
				}
				
				if (messageToSend == null) continue;
				if (messageToSend.getMessageType() == MessageType.POISON) break;
				
				Socket sendSocket;
				
				/*
				 * We are doing a compound operation on our color. In Chandy-Lamport, we want to be sure
				 * that we are white when we are taking away our bitcakes.
				 * The bitcake reducing is done in the sendEffect() method of the
				 * Transaction message, which is at the bottom of this sync.
				 */
				synchronized (AppConfig.colorLock) {
					// For control/marker messages, or when we're white, proceed with sending
					if (isMarkerOrControlMessage(messageToSend) || AppConfig.isWhite.get()) {
						if (MessageUtil.MESSAGE_UTIL_PRINTING) {
							AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
						}
						
						// Validate the message has a valid receiver
						if (messageToSend.getOriginalReceiverInfo() == null) {
							AppConfig.timestampedErrorPrint("Cannot send message with null originalReceiverInfo: " + messageToSend);
							continue;
						}
						
						try {
							sendSocket = new Socket(
								messageToSend.getOriginalReceiverInfo().ipAddress(), 
								messageToSend.getOriginalReceiverInfo().listenerPort()
							);
							
							ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
							oos.writeObject(messageToSend);
							oos.flush();
							
							messageToSend.sendEffect();
						} catch (Exception e) {
							AppConfig.timestampedErrorPrint("Error connecting to neighbor: " + e.getMessage());
							// Put the message back in the queue to try again later
							if (isMarkerOrControlMessage(messageToSend)) {
								MessageUtil.pendingMarkers.get(neighbor).put(messageToSend);
							} else {
								MessageUtil.pendingMessages.get(neighbor).put(messageToSend);
							}
							continue;
						}
					} else {
						// We're not white and this is a regular message, put it back in the queue
						MessageUtil.pendingMessages.get(neighbor).put(messageToSend);
						continue;
					}
				}
				
				try {
					ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
					String ackString = (String)ois.readObject();
					if (!ackString.equals("ACK")) {
						AppConfig.timestampedErrorPrint("Got response which is not an ACK");
					}
					
					sendSocket.close();
				} catch (Exception e) {
					AppConfig.timestampedErrorPrint("Error receiving ACK: " + e.getMessage());
				}

			} catch (Exception e) {
				AppConfig.timestampedErrorPrint("Error in FifoSendWorker: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Helper method to check if a message is a marker or control message that should
	 * be processed with high priority regardless of color state.
	 */
	private boolean isMarkerOrControlMessage(Message message) {
		MessageType type = message.getMessageType();
		return type == MessageType.CC_SNAPSHOT_REQUEST || type == MessageType.CC_SNAPSHOT_RESPONSE || type == MessageType.CC_RESUME;
	}
	
	@Override
	public void stop() {
		working = false;
	}
}
