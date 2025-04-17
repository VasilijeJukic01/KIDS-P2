package com.kids.servent.handler.implementation.ab;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.snapshot_collector.ABCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.ab.ABSnapshotResponseMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ABSnapshotResponseHandler  implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.AB_SNAPSHOT_RESPONSE) {
                int neighborAmount = Integer.parseInt(clientMessage.getMessageText());
                ABSnapshotResponseMessage response = (ABSnapshotResponseMessage) clientMessage;

                ABSnapshot snapshotResult = new ABSnapshot(
                        clientMessage.getOriginalSenderInfo().id(),
                        neighborAmount,
                        response.getSent(),
                        response.getReceived()
                );

                ((ABCollector) snapshotCollector).getCollectedABValues().put("node " + clientMessage.getOriginalSenderInfo().id(), snapshotResult);
            } else {
                AppConfig.timestampedErrorPrint("SNAPSHOT RESPONSE HANDLER: Amount handler got: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint(e.getMessage());
        }
    }

}
