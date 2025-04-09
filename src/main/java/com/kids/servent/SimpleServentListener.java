package com.kids.servent;

import com.kids.app.AppConfig;
import com.kids.app.Cancellable;
import com.kids.app.snapshot_bitcake.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.handler.implementation.NullHandler;
import com.kids.servent.handler.implementation.TransactionHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServentListener implements Runnable, Cancellable {

	/*
	 * Thread pool for executing the handlers. Each client will get its own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	private volatile boolean working = true;
	private final SnapshotCollector snapshotCollector;

	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.listenerPort(), 100);
			// If there is no connection after 1s, wake up and see if we should terminate
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.listenerPort());
			System.exit(0);
		}
		
		while (working) {
			try {
				Message clientMessage;
				// This blocks for up to 1s, after which SocketTimeoutException is thrown
				Socket clientSocket = listenerSocket.accept();
				clientMessage = MessageUtil.readMessage(clientSocket);
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has its own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case TRANSACTION:
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case POISON:
					break;
				}
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				// Uncomment the next line to see that we are waking up every second
				// AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
