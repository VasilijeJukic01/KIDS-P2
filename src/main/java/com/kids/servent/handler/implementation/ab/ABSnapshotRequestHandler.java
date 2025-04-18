package com.kids.servent.handler.implementation.ab;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.ab.ABSnapshotResponseMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ABSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AB_SNAPSHOT_REQUEST) {
            int currentAmount = snapshotCollector.getBitcakeManager().getCurrentBitcakeAmount();
            CausalBroadcast instance = CausalBroadcast.getInstance();
            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(instance.getVectorClockValues());

            Message response = new ABSnapshotResponseMessage(
                    AppConfig.myServentInfo,
                    clientMessage.getOriginalSenderInfo(),
                    null,
                    vectorClock,
                    currentAmount,
                    instance.getSent(),
                    instance.getReceived()
            );
            instance.causalClockIncrement(response);

            AppConfig.myServentInfo.neighbors()
                    .forEach(neighbor -> MessageUtil.sendMessage(response.changeReceiver(neighbor).makeMeASender()));

        } else {
            AppConfig.timestampedErrorPrint("SNAPSHOT REQUEST HANDLER: Amount handler got: " + clientMessage);
        }
    }

}
