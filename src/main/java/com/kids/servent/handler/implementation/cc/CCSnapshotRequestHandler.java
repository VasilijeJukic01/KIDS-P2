package com.kids.servent.handler.implementation.cc;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

/**
 * Handler for snapshot request messages in the Coordinated Checkpointing algorithm.
 * When a node receives a snapshot request, it records its local state, enters snapshot mode,
 * and forwards the request to its neighbors.
 */
@RequiredArgsConstructor
public class CCSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.CC_SNAPSHOT_REQUEST) {
                if (!(snapshotCollector.getBitcakeManager() instanceof CCBitcakeManager bitcakeManager)) return;

                bitcakeManager.handleSnapshotRequest(clientMessage, snapshotCollector);
            } else {
                AppConfig.timestampedErrorPrint("CC SNAPSHOT REQUEST HANDLER: Handler got wrong message type: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error in CCSnapshotRequestHandler: " + e.getMessage());
        }
    }
} 