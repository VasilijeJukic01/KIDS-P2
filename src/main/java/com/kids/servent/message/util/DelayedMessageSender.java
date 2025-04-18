package com.kids.servent.message.util;

import com.kids.app.AppConfig;
import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.Message;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or some such.
 * 
 * @author bmilojkovic
 *
 */
@AllArgsConstructor
public class DelayedMessageSender implements Runnable {

	private Message messageToSend;
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 1000) + 500);
		} catch (InterruptedException ignore) { }
		
		ServentInfo receiverInfo = messageToSend.getReceiverInfo();
		if (MessageUtil.MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
		}

		try {
			Socket sendSocket = new Socket(receiverInfo.ipAddress(), receiverInfo.listenerPort());

			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(messageToSend);
			oos.flush();

			sendSocket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString());
		}
	}
	
}
