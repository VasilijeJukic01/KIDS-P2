package com.kids.app.snapshot_bitcake.coordinated_checkpointing;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.BitcakeManager;
import com.kids.app.snapshot_bitcake.snapshot_collector.CCCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.cc.CCResumeMessage;
import com.kids.servent.message.implementation.cc.CCSnapshotRequestMessage;
import com.kids.servent.message.implementation.cc.CCSnapshotResponseMessage;
import com.kids.servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CCBitcakeManager implements BitcakeManager {

    private final AtomicInteger amount = new AtomicInteger(1000);
    private final AtomicBoolean snapshotMode = new AtomicBoolean(false);
    private final Map<Integer, Message> pendingMessages = new ConcurrentHashMap<>();
    private int initiatorId = -1;

    @Override
    public void takeSomeBitcakes(int amount) {
        this.amount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        this.amount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return amount.get();
    }

    /**
     * Handles a snapshot request from the initiator.
     * When a node receives a snapshot request, it:
     * 1. Records its local state
     * 2. Enters snapshot mode (blocking new messages)
     * 3. Forwards the request to neighbors
     * 4. Sends a response back to the initiator
     */
    public void handleSnapshotRequest(Message requestMessage, SnapshotCollector snapshotCollector) {
        try {
            AppConfig.timestampedStandardPrint("Handling snapshot request: " + requestMessage);
            
            // Only proceed if not already in snapshot mode
            if (snapshotMode.compareAndSet(false, true)) {
                CCSnapshotRequestMessage request = (CCSnapshotRequestMessage) requestMessage;
                initiatorId = request.getInitiatorId();
                
                AppConfig.timestampedStandardPrint("Entering snapshot mode, initiator: " + initiatorId);
                
                // Record local state
                int currentAmount = getCurrentBitcakeAmount();
                CCSnapshot snapshot = new CCSnapshot(AppConfig.myServentInfo.id(), currentAmount);

                AppConfig.timestampedStandardPrint("Adding snapshot to collector, amount: " + currentAmount);
                ((CCCollector) snapshotCollector).addCCSnapshotInfo(AppConfig.myServentInfo.id(), snapshot);
                
                // Forward to neighbors
                for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
                    if (neighbor != requestMessage.getOriginalSenderInfo().id()) {
                        AppConfig.timestampedStandardPrint("Forwarding snapshot request to neighbor: " + neighbor);
                        
                        CCSnapshotRequestMessage forwardMessage = new CCSnapshotRequestMessage(
                                AppConfig.myServentInfo,
                                AppConfig.getInfoById(neighbor),
                                initiatorId
                        );
                        
                        MessageUtil.sendMessage(forwardMessage);
                    }
                }
                
                // Send response back to initiator
                if (AppConfig.myServentInfo.id() != initiatorId) {
                    AppConfig.timestampedStandardPrint("Sending snapshot response to initiator: " + initiatorId);
                    
                    CCSnapshotResponseMessage response = new CCSnapshotResponseMessage(
                            AppConfig.myServentInfo,
                            AppConfig.getInfoById(initiatorId),
                            currentAmount
                    );
                    
                    MessageUtil.sendMessage(response);
                } else {
                    AppConfig.timestampedStandardPrint("I am the initiator, not sending response to myself");
                }
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error handling snapshot request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exits snapshot mode when receiving a resume message.
     * Process any pending messages that were received during snapshot mode.
     */
    public void exitSnapshotMode() {
        if (snapshotMode.compareAndSet(true, false)) {
            AppConfig.timestampedStandardPrint("Exiting snapshot mode, processing " + pendingMessages.size() + " pending messages");
            initiatorId = -1;
            
            // Process any pending messages
            for (Message pendingMessage : pendingMessages.values()) {
                MessageUtil.sendMessage(pendingMessage);
            }
            pendingMessages.clear();
        }
    }

    /**
     * Checks if a message should be processed or queued while in snapshot mode.
     * 
     * @param message The message to check
     * @return true if the message should be queued, false if it can be processed immediately
     */
    public boolean shouldQueueMessage(Message message) {
        if (!snapshotMode.get()) return false;
        
        // Queue the message for later processing
        pendingMessages.put(message.getMessageId(), message);
        AppConfig.timestampedStandardPrint("Queued message during snapshot: " + message);
        return true;
    }

    /**
     * Called by the initiator to resume normal operation after the snapshot is complete.
     */
    public void sendResumeMessages() {
        if (AppConfig.myServentInfo.id() == initiatorId) {
            AppConfig.timestampedStandardPrint("Initiator sending resume messages to all nodes");
            
            // Send CC_RESUME message to all nodes
            for (int i = 0; i < AppConfig.getServentCount(); i++) {
                if (i != AppConfig.myServentInfo.id()) {
                    CCResumeMessage resumeMessage = new CCResumeMessage(
                            AppConfig.myServentInfo,
                            AppConfig.getInfoById(i)
                    );
                    MessageUtil.sendMessage(resumeMessage);
                }
            }

            exitSnapshotMode();
        }
    }
    
    /**
     * Checks if the node is currently in snapshot mode.
     */
    public boolean isInSnapshotMode() {
        return snapshotMode.get();
    }
    
    /**
     * Checks if this node is the initiator of the current snapshot.
     */
    public boolean isInitiator() {
        return AppConfig.myServentInfo.id() == initiatorId;
    }
} 