package com.kids.app.snapshot_bitcake.snapshot_strategy;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.alagar_venkatesan.AVBitcakeManager;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.av.AVMarkerMessage;
import com.kids.servent.message.implementation.av.AVTerminateMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class AVSnapshotStrategy implements SnapshotStrategy {

    private final List<Integer> collectedAVData;
    private final AVBitcakeManager bitcakeManager;

     // Indicates whether the snapshot process is complete.
    @Setter private boolean wait = true;

    @Override
    public void initiateSnapshot() {
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
        CausalBroadcast.initiatorId = AppConfig.myServentInfo.id();

        // Initialize input and output channels
        AppConfig.myServentInfo.neighbors().forEach(
                neighbor -> {
                    CausalBroadcast.inputChannel.put(neighbor, 0);
                    CausalBroadcast.outputChannel.put(neighbor, 0);
                }
        );

        // Create MARKER message
        CausalBroadcast.recordedAmount = bitcakeManager.getCurrentBitcakeAmount();
        Message markerMessage = new AVMarkerMessage(AppConfig.myServentInfo, null, null, vectorClock);
        CausalBroadcast.markerVectorClock = markerMessage.getSenderVectorClock();

        // Send MARKER message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            markerMessage = markerMessage.changeReceiver(neighbor);
            MessageUtil.sendMessage(markerMessage);
        }

        CausalBroadcast.causalClockIncrement(markerMessage);
    }

    @Override
    public boolean isSnapshotComplete() {
        // We have collected all DONE messages
        if (collectedAVData.size() + 1 == AppConfig.getServentCount()) {
            Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());

            // Create TERMINATE message
            Message terminateMessage = new AVTerminateMessage(
                    AppConfig.myServentInfo,
                    null,
                    null,
                    vectorClock
            );

            // Send TERMINATE message to all neighbors
            for (int neighbor : AppConfig.myServentInfo.neighbors()) {
                terminateMessage = terminateMessage.changeReceiver(neighbor);
                MessageUtil.sendMessage(terminateMessage);
            }

            CausalBroadcast.addPendingMessage(terminateMessage);
            CausalBroadcast.checkPendingMessages();
            return true;
        }
        return false;
    }

    @Override
    public void processCollectedData() {
        while (wait) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                AppConfig.timestampedErrorPrint("AVSnapshotStrategy: Error while sleeping: " + e.getMessage());
            }
        }
    }

}
