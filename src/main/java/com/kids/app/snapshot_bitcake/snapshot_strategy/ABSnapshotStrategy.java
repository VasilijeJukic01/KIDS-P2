package com.kids.app.snapshot_bitcake.snapshot_strategy;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.ab.ABSnapshotRequestMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class ABSnapshotStrategy implements SnapshotStrategy {

    private final Map<String, ABSnapshot> collectedData;
    private final ABBitcakeManager bitcakeManager;

    @Override
    public void initiateSnapshot() {
        // Create SNAPSHOT_REQUEST message
        CausalBroadcast instance = CausalBroadcast.getInstance();
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(instance.getVectorClockValues());
        Message request = new ABSnapshotRequestMessage(AppConfig.myServentInfo, null, null, vectorClock);

        // Send SNAPSHOT_REQUEST message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            Message neighborRequest = request.changeReceiver(neighbor);
            MessageUtil.sendMessage(neighborRequest);
        }

        // Save the current state
        ABSnapshot snapshotResult = new ABSnapshot(
                AppConfig.myServentInfo.id(),
                bitcakeManager.getCurrentBitcakeAmount(),
                new ArrayList<>(instance.getSent()),
                new ArrayList<>(instance.getReceived())
        );
        collectedData.put("node " + AppConfig.myServentInfo.id(), snapshotResult);

        instance.causalClockIncrement(request);
    }

    @Override
    public boolean isSnapshotComplete() {
        // We have collected all the responses
        return collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processCollectedData() {
        int nodeSum = 0;
        for (Map.Entry<String, ABSnapshot> entry : collectedData.entrySet()) {
            int nodeAmount = entry.getValue().getAmount();
            nodeSum += nodeAmount;
            AppConfig.timestampedStandardPrint("Snapshot for " + entry.getKey() + " = " + nodeAmount + " bitcake");
        }

        Map<String, Message> allSentMessages = new HashMap<>();
        Map<String, Message> allReceivedMessages = new HashMap<>();

        // Collect all sent and received messages
        for (ABSnapshot snapshot : collectedData.values()) {
            snapshot.getSent().stream()
                    .filter(sent -> sent.getMessageType() == MessageType.TRANSACTION)
                    .forEach(sent -> allSentMessages.put(getUniqueMessageKey(sent), sent));

            snapshot.getReceived().stream()
                    .filter(received -> received.getMessageType() == MessageType.TRANSACTION)
                    .forEach(received -> allReceivedMessages.put(getUniqueMessageKey(received), received));
        }
        
        // Messages in transit
        int inTransitSum = 0;
        for (Map.Entry<String, Message> entry : allSentMessages.entrySet()) {
            String key = entry.getKey();
            if (!allReceivedMessages.containsKey(key)) {
                Message message = entry.getValue();
                int amount = Integer.parseInt(message.getMessageText());
                inTransitSum += amount;
                
                AppConfig.timestampedStandardPrint("Unprocessed transaction "
                        + "[" + message.getOriginalSenderInfo().id() + " to " + message.getOriginalReceiverInfo().id() + "]: "  + amount + " bitcake");
            }
        }

        int total = nodeSum + inTransitSum;
        AppConfig.timestampedStandardPrint("Total node amount: " + nodeSum + " bitcake");
        AppConfig.timestampedStandardPrint("Total in-transit amount: " + inTransitSum + " bitcake");
        AppConfig.timestampedStandardPrint("System bitcake count: " + total);

        collectedData.clear();
    }

    private String getUniqueMessageKey(Message message) {
        return message.getOriginalSenderInfo().id() + "-" + message.getMessageId() + "-" + message.getOriginalReceiverInfo().id();
    }
}
