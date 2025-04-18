package com.kids.servent.handler.implementation.av;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.snapshot_collector.AVCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.MessageHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class AVTerminateHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AV_TERMINATE) {
            AppConfig.timestampedStandardPrint("AV Termination");
            CausalBroadcast instance = CausalBroadcast.getInstance();
            instance.setMarkerVectorClock(null);

            int sum = instance.getRecordedAmount();
            AppConfig.timestampedStandardPrint("Recorded bitcake amount: " + instance.getRecordedAmount());

            // Input channel
            for (Map.Entry<Integer, Integer> entry : instance.getInputChannel().entrySet()) {
                AppConfig.timestampedStandardPrint("Unreceived bitcake amount: " + entry.getValue() + " from " + entry.getKey());
                sum += entry.getValue();
            }

            // Output channel
            for (Map.Entry<Integer, Integer> entry : instance.getOutputChannel().entrySet()) {
                AppConfig.timestampedStandardPrint("Sent bitcake amount: " + entry.getValue() + " from " + entry.getKey());
                sum -= entry.getValue();
            }

            AppConfig.timestampedStandardPrint("Total node bitcake amount: " + sum);

            ((AVCollector) snapshotCollector).clearAVData();
        }
    }
}
