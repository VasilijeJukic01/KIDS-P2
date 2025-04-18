package com.kids.servent.message.util;

import com.kids.app.AppConfig;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * <p>
 * Depending on the configuration it delegates sending either to a {@link DelayedMessageSender}
 * 
 * When reading, if we are FIFO, we send an ACK message on the same socket, so the other side
 * knows they can send the next message.
 * @author bmilojkovic
 *
 */
public class MessageUtil {

	/**
	 * Normally this should be true, because it helps with debugging.
	 * Flip this to false to disable printing every message send / receive.
	 */
	public static final boolean MESSAGE_UTIL_PRINTING = true;
	
	public static Map<Integer, BlockingQueue<Message>> pendingMessages = new ConcurrentHashMap<>();
	public static Map<Integer, BlockingQueue<Message>> pendingMarkers = new ConcurrentHashMap<>();
	
	public static void initializePendingMessages() {
		for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
			pendingMarkers.put(neighbor, new LinkedBlockingQueue<>());
			pendingMessages.put(neighbor, new LinkedBlockingQueue<>());
		}
	}
	
	public static Message readMessage(Socket socket) {
		Message clientMessage = null;
			
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			clientMessage = (Message) ois.readObject();
			
			if (AppConfig.IS_FIFO) {
				String response = "ACK";
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(response);
				oos.flush();
			}
			
			socket.close();
		} catch (Exception ignore) { }
		
		if (MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
		return clientMessage;
	}
	
	public static void sendMessage(Message message) {
		if (AppConfig.IS_FIFO) {
			try {
				// Special handling for marker/snapshot/control messages
				if (message.getMessageType() == MessageType.CC_SNAPSHOT_REQUEST ||
					message.getMessageType() == MessageType.CC_SNAPSHOT_RESPONSE ||
					message.getMessageType() == MessageType.CC_RESUME) {

					int receiverId = message.getOriginalReceiverInfo().id();
					
					// Check if we have a queue for this receiver
					if (!pendingMarkers.containsKey(receiverId)) {
						// Fallback to sending directly
						Thread delayedSender = new Thread(new DelayedMessageSender(message));
						delayedSender.start();
						return;
					}
					
					pendingMarkers.get(receiverId).put(message);
					AppConfig.timestampedStandardPrint("Added message to pendingMarkers queue: " + message);
				}
				else {
					// Check if the message has valid originalReceiverInfo
					if (message.getOriginalReceiverInfo() == null) {
						AppConfig.timestampedErrorPrint("Cannot send message with null originalReceiverInfo: " + message);
						return;
					}
					int receiverId = message.getOriginalReceiverInfo().id();
					
					// Check if we have a queue for this receiver
					if (!pendingMessages.containsKey(receiverId)) {
						// Try to send directly as fallback
						Thread delayedSender = new Thread(new DelayedMessageSender(message));
						delayedSender.start();
						return;
					}
					
					pendingMessages.get(receiverId).put(message);
				}
			} catch (InterruptedException e) {
				AppConfig.timestampedErrorPrint("Interrupted while putting message in queue: " + e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				AppConfig.timestampedErrorPrint("Error sending message: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			Thread delayedSender = new Thread(new DelayedMessageSender(message));
			delayedSender.start();
		}
	}
}
