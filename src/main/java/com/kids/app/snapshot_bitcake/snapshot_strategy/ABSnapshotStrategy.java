package com.kids.app.snapshot_bitcake.snapshot_strategy;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABBitcakeManager;
import com.kids.app.snapshot_bitcake.acharya_badrinath.ABSnapshot;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.ABSnapshotRequestMessage;
import com.kids.servent.message.util.MessageUtil;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class ABSnapshotStrategy implements SnapshotStrategy {

    private final Map<String, ABSnapshot> collectedData;
    private final ABBitcakeManager bitcakeManager;

    @Override
    public void initiateSnapshot() {
        // Create SNAPSHOT_REQUEST message
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
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
                CausalBroadcast.getSent(),
                CausalBroadcast.getReceived()
        );
        collectedData.put("node " + AppConfig.myServentInfo.id(), snapshotResult);

        CausalBroadcast.causalClockIncrement(request);
    }

    @Override
    public boolean isSnapshotComplete() {
        // // We have collected all the responses
        return collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processCollectedData(Map<String, ABSnapshot> collectedAbValues) {
        int sum = 0;

        for (Map.Entry<String, ABSnapshot> result : collectedAbValues.entrySet()) {
            int bitCakeAmount = result.getValue().getAmount();
            List<Message> sentTransactions = result.getValue().getSent();

            sum += bitCakeAmount;
            AppConfig.timestampedStandardPrint("Snapshot for " + result.getKey() + " = " + bitCakeAmount + " bitcake");

            // Check for unprocessed transactions
            for (Message sent : sentTransactions) {
                ABSnapshot abSnapshotResult = collectedAbValues.get("node " + sent.getOriginalReceiverInfo().id());
                List<Message> receivedTransactions = abSnapshotResult.getReceived();

                boolean exist = receivedTransactions.stream()
                        .filter(received -> sent.getMessageId() == received.getMessageId())
                        .filter(received -> sent.getOriginalSenderInfo().id() == received.getOriginalSenderInfo().id())
                        .anyMatch(received -> sent.getOriginalReceiverInfo().id() == received.getOriginalReceiverInfo().id());

                if (!exist) {
                    AppConfig.timestampedStandardPrint("Info for unprocessed transaction: " + sent.getMessageText() + " bitcake");
                    int amountNumber = Integer.parseInt(sent.getMessageText());
                    sum += amountNumber;
                }
            }
        }

        AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
        collectedAbValues.clear();
    }

}
