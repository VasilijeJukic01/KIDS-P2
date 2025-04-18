package com.kids.servent.handler.implementation;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.servent.ServentInfo;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Handles the CAUSAL_BROADCAST message. Fairly simple, as we assume that we are
 * in a clique. We add the message to a pending queue, and let the check on the queue
 * take care of the rest.
 *
 * @author bmilojkovic
 */
@RequiredArgsConstructor
public class CausalBroadcastHandler implements MessageHandler {

    private final Message clientMessage;
    private final Set<Message> receivedBroadcasts;
    private final Object lock;

    @Override
    public void run() {
        ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();

        if (senderInfo.id() == AppConfig.myServentInfo.id()) {
            AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
        } else {
            synchronized (lock) {
                boolean isAdded = receivedBroadcasts.add(clientMessage);

                if (isAdded) {
                    // Add the message to the pending queue and check for causal order.
                    CausalBroadcast instance = CausalBroadcast.getInstance();
                    instance.addPendingMessage(clientMessage);
                    instance.checkPendingMessages();

                    // Rebroadcast the message to neighbors
                    if (!AppConfig.IS_CLIQUE) {
                        AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

                        AppConfig.myServentInfo.neighbors().stream()
                                .filter(neighbor -> neighbor != senderInfo.id())
                                .forEach(neighbor -> {
                                    // Same message, different receiver, and add us to the route table.
                                    MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
                                });
                    }
                } else {
                    // We already got this from somewhere else.
                    AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
                }
            }
        }
    }

}
