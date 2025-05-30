package com.kids.servent;

import com.kids.app.AppConfig;
import com.kids.app.Cancellable;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.handler.implementation.CausalBroadcastHandler;
import com.kids.servent.handler.implementation.NullHandler;
import com.kids.servent.handler.implementation.TransactionHandler;
import com.kids.servent.handler.implementation.cc.CCResumeHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotRequestHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotResponseHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens for incoming messages on a specified port and processes them using a thread pool.
 * It handles messages in a concurrent environment and integrates with a snapshot collector for distributed system snapshots.
 */
public class SimpleServentListener implements Runnable, Cancellable {

	/*
	 * Thread pool for executing the handlers. Each client will get its own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();

	private volatile boolean working = true;
	private final SnapshotCollector snapshotCollector;
	private final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Object lock = new Object();

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
				MessageHandler messageHandler;
				if (AppConfig.IS_FIFO) {
					Message clientMessage;

					Socket clientSocket = listenerSocket.accept();
					clientMessage = MessageUtil.readMessage(clientSocket);

					messageHandler = new NullHandler(clientMessage);
					
					// Log received message before processing
					AppConfig.timestampedStandardPrint("Received message: " + clientMessage);
					
					switch (clientMessage.getMessageType()) {
						case TRANSACTION:
							messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
							break;
						case CC_SNAPSHOT_REQUEST:
							AppConfig.timestampedStandardPrint("Processing CC_SNAPSHOT_REQUEST in listener");
							messageHandler = new CCSnapshotRequestHandler(clientMessage, snapshotCollector);
							break;
						case CC_SNAPSHOT_RESPONSE:
							if (clientMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
								messageHandler = new CCSnapshotResponseHandler(clientMessage, snapshotCollector);
							}
							break;
						case CC_RESUME:
							messageHandler = new CCResumeHandler(clientMessage, snapshotCollector);
							break;
						case POISON:
							break;
						default:
							AppConfig.timestampedErrorPrint("Unhandled message type: " + clientMessage.getMessageType());
							break;
					}
				}
				else {
					Message clientMessage;
					// This blocks for up to 1s, after which SocketTimeoutException is thrown
					Socket clientSocket = listenerSocket.accept();
					clientMessage = MessageUtil.readMessage(clientSocket);

					messageHandler = new CausalBroadcastHandler(
							clientMessage,
							receivedBroadcasts,
							lock
					);
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
