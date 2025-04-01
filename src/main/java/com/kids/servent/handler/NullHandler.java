package com.kids.servent.handler;

import com.kids.app.AppConfig;
import com.kids.servent.message.Message;

/**
 * This will be used if no proper handler is found for the message.
 * @author bmilojkovic
 *
 */
public class NullHandler implements MessageHandler {

	private final Message clientMessage;
	
	public NullHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		AppConfig.timestampedErrorPrint("Couldn't handle message: " + clientMessage);
	}

}
