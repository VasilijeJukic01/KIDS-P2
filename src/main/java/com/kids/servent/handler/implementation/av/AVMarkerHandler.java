package com.kids.servent.handler.implementation.av;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.av.AVDoneMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class AVMarkerHandler implements MessageHandler {

    private final Message clientMessage;
    private final int bitcakeAmount;

    @Override
    public void run() {
        CausalBroadcast instance = CausalBroadcast.getInstance();
        
        // Initialize input and output channels for each neighbor
        AppConfig.myServentInfo.neighbors().forEach(neighbor -> {
            /*
             * Access inputChannel and outputChannel map through transaction recording
             * This is a workaround since we cant directly modify the private maps.
             */
            instance.recordTransaction(
                new ConcurrentHashMap<>(instance.getVectorClockValues()), 
                neighbor, 
                0
            );
        });

        instance.setMarkerVectorClock(clientMessage.getSenderVectorClock());
        instance.setRecordedAmount(bitcakeAmount);
        instance.setInitiatorId(clientMessage.getReceiverInfo().id());

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

        instance.causalClockIncrement(doneMessage);
    }
}
