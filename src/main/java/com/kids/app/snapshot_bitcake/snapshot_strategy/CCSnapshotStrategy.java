package com.kids.app.snapshot_bitcake.snapshot_strategy;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCBitcakeManager;
import com.kids.app.snapshot_bitcake.coordinated_checkpointing.CCSnapshot;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.message.implementation.cc.CCSnapshotRequestMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * Snapshot strategy implementation for the Coordinated Checkpointing algorithm.
 */
@AllArgsConstructor
public class CCSnapshotStrategy implements SnapshotStrategy {

    private final Map<Integer, CCSnapshot> collectedData;
    private final CCBitcakeManager bitcakeManager;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void initiateSnapshot() {
        // Check if we are already in snapshot mode
        if (bitcakeManager.isInSnapshotMode()) {
            AppConfig.timestampedErrorPrint("Already in snapshot mode, cannot initiate new snapshot");
            return;
        }
        
        // Create the initial SNAPSHOT_REQUEST message for ourselves
        CCSnapshotRequestMessage selfRequest = new CCSnapshotRequestMessage(
                AppConfig.myServentInfo,
                AppConfig.myServentInfo,
                AppConfig.myServentInfo.id()
        );
        
        // Handle snapshot request locally
        bitcakeManager.handleSnapshotRequest(selfRequest, snapshotCollector);
        
        // Send SNAPSHOT_REQUEST message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            CCSnapshotRequestMessage neighborRequest = new CCSnapshotRequestMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(neighbor),
                    AppConfig.myServentInfo.id()
            );
            
            MessageUtil.sendMessage(neighborRequest);
        }
    }

    @Override
    public boolean isSnapshotComplete() {
        // We have data from all nodes
        return collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processCollectedData() {
        int sum = 0;
        
        StringBuilder builder = new StringBuilder();
        builder.append("Coordinated Checkpointing Snapshot results are:\n");
        
        for (Map.Entry<Integer, CCSnapshot> entry : collectedData.entrySet()) {
            CCSnapshot snapshot = entry.getValue();
            int currentAmount = snapshot.recordedAmount();
            
            builder.append("Node ").append(entry.getKey())
                   .append(" had ").append(currentAmount)
                   .append(" bitcakes.\n");
            
            sum += currentAmount;
        }
        
        builder.append("Total amount of bitcakes in the system is ").append(sum);
        AppConfig.timestampedStandardPrint(builder.toString());
    }
} 