package com.kids.servent.handler.implementation.av;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.av.AVDoneMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AVMarkerHandler implements MessageHandler {

    private final Message clientMessage;
    private final int bitcakeAmount;

    @Override
    public void run() {
        // Initialize input and output channels
        AppConfig.myServentInfo.neighbors()
                .forEach(neighbor -> {
                    CausalBroadcast.inputChannel.put(neighbor, 0);
                    CausalBroadcast.outputChannel.put(neighbor, 0);
                });

        CausalBroadcast.markerVectorClock = clientMessage.getSenderVectorClock();
        CausalBroadcast.recordedAmount = bitcakeAmount;
        CausalBroadcast.initiatorId = clientMessage.getReceiverInfo().id();

        // Create DONE message
        Message doneMessage = new AVDoneMessage(
                AppConfig.myServentInfo,
                clientMessage.getOriginalSenderInfo(),
                null,
                clientMessage.getSenderVectorClock()
        );

        // Send DONE message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            doneMessage = doneMessage.changeReceiver(neighbor);
            MessageUtil.sendMessage(doneMessage);
        }

        CausalBroadcast.causalClockIncrement(doneMessage);
    }
}
