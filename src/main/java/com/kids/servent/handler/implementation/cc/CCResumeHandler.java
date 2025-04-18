package com.kids.servent.handler.implementation.cc;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

/**
 * Handler for resume messages in the Coordinated Checkpointing algorithm.
 * <p>
 * When a node receives a resume message, it exits snapshot mode and processes any messages that were queued during the snapshot.
 */
@RequiredArgsConstructor
public class CCResumeHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.CC_RESUME) {
            CCBitcakeManager bitcakeManager = (CCBitcakeManager) snapshotCollector.getBitcakeManager();
            
            // Exit snapshot mode and process queued messages
            bitcakeManager.exitSnapshotMode();
            
            AppConfig.timestampedStandardPrint("Exited snapshot mode, resuming normal operation");
        } else {
            AppConfig.timestampedErrorPrint("CC RESUME HANDLER: Handler got wrong message type: " + clientMessage);
        }
    }
} 