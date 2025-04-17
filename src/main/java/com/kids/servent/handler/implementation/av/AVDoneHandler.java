package com.kids.servent.handler.implementation.av;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.snapshot_collector.AVCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AVDoneHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AV_DONE) {
            ((AVCollector) snapshotCollector).markAsDone(clientMessage.getReceiverInfo().id());
        } else {
            AppConfig.timestampedErrorPrint("SNAPSHOT DONE HANDLER: Amount handler got: " + clientMessage);
        }
    }

}
