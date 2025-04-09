package com.kids.servent.handler.implementation;

import com.kids.app.AppConfig;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import lombok.RequiredArgsConstructor;

/**
 * This will be used if no proper handler is found for the message.
 * @author bmilojkovic
 *
 */
@RequiredArgsConstructor
public class NullHandler implements MessageHandler {

	private final Message clientMessage;
	
	@Override
	public void run() {
		AppConfig.timestampedErrorPrint("Couldn't handle message: " + clientMessage);
	}

}
