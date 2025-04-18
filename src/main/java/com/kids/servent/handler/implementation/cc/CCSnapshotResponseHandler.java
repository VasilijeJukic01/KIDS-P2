package com.kids.servent.handler.implementation.cc;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCSnapshot;
import com.kids.app.snapshot_bitcake.snapshot_collector.CCCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.cc.CCSnapshotResponseMessage;
import lombok.RequiredArgsConstructor;

/**
 * Handler for snapshot response messages in the Coordinated Checkpointing algorithm.
 * <p>
 * When the initiator receives a response from a node, it adds the node's snapshot to the collector. Once all nodes have responded, it sends resume messages.
 */
@RequiredArgsConstructor
public class CCSnapshotResponseHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.CC_SNAPSHOT_RESPONSE) {
            CCSnapshotResponseMessage response = (CCSnapshotResponseMessage) clientMessage;
            int senderId = clientMessage.getOriginalSenderInfo().id();
            
            AppConfig.timestampedStandardPrint("Received snapshot response from node " + senderId + " with amount " + response.getAmount());
            
            // Create snapshot for the node that sent the response
            CCSnapshot snapshot = new CCSnapshot(senderId, response.getAmount());
            ((CCCollector) snapshotCollector).addCCSnapshotInfo(senderId, snapshot);

            CCBitcakeManager bitcakeManager = (CCBitcakeManager) snapshotCollector.getBitcakeManager();
            
            // Only the initiator should check for completion and send resume messages
            if (bitcakeManager.isInitiator()) {
                int responseCount = ((CCCollector) snapshotCollector).getCollectedCCSize();
                
                AppConfig.timestampedStandardPrint("Current snapshot response count: " + responseCount + "/" + AppConfig.getServentCount());
                
                if (responseCount == AppConfig.getServentCount()) {
                    AppConfig.timestampedStandardPrint("Snapshot complete, sending resume messages");
                    // Send CC_RESUME messages to all nodes
                    bitcakeManager.sendResumeMessages();
                }
            }
        } else {
            AppConfig.timestampedErrorPrint("CC SNAPSHOT RESPONSE HANDLER: Handler got wrong message type: " + clientMessage);
        }
    }
} 