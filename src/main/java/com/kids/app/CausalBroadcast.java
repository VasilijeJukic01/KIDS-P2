package com.kids.app;

import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.implementation.ab.ABSnapshotRequestHandler;
import com.kids.servent.handler.implementation.ab.ABSnapshotResponseHandler;
import com.kids.servent.handler.implementation.TransactionHandler;
import com.kids.servent.handler.implementation.av.AVDoneHandler;
import com.kids.servent.handler.implementation.av.AVMarkerHandler;
import com.kids.servent.handler.implementation.av.AVTerminateHandler;
import com.kids.servent.handler.implementation.cc.CCResumeHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotRequestHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotResponseHandler;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcast {

    private static final ExecutorService executor = Executors.newWorkStealingPool();

    @Getter private static final Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static final Object lock = new Object();
    @Getter private static SnapshotCollector snapshotCollector;

    // AB Snapshot
    @Getter  private static final List<Message> sent = new CopyOnWriteArrayList<>();
    @Getter  private static final List<Message> received = new CopyOnWriteArrayList<>();
    private static final Set<Message> receivedAbRequest = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // AV Snapshot
    public static int initiatorId;
    public static int recordedAmount;
    public static Map<Integer, Integer> markerVectorClock = null;
    public static final Map<Integer, Integer> inputChannel = new ConcurrentHashMap<>();
    public static final Map<Integer, Integer> outputChannel = new ConcurrentHashMap<>();

    /**
     * Initializes the vector clock with the given number of servents.
     *
     * @param serventCount the number of servents in the system.
     */
    public static void initializeVectorClock(int serventCount) {
        Stream.iterate(0, i -> i + 1)
                .limit(serventCount)
                .forEach(i -> vectorClock.put(i, 0));
    }

    /**
     * Compares two vector clocks.
     * Returns true if any entry in clock2 is greater than the corresponding entry in clock1.
     *
     * @param clock1 the first vector clock.
     * @param clock2 the second vector clock.
     * @return true if clock2 is greater in any entry, false otherwise.
     * @throws IllegalArgumentException if the clocks are of different sizes.
     */
    private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Increments the causal clock based on a new message.
     * Also checks for and processes any pending messages.
     *
     * @param newMessage the message triggering the clock update.
     */
    public static void causalClockIncrement(Message newMessage) {
        AppConfig.timestampedStandardPrint("Committing # " + newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().id());
        checkPendingMessages();
    }

    /**
     * Checks the pending messages queue and commits messages that satisfy the causal order.
     */
    public static void checkPendingMessages() {
        if (AppConfig.IS_FIFO) processPendingMessagesFIFO();
        else processPendingMessagesCausal();
    }

    private static void processPendingMessagesFIFO() {
        synchronized (lock) {
            boolean working = true;

            while (working) {
                working = false;
                Iterator<Message> iterator = pendingMessages.iterator();

                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    if (isCausalityViolatedFIFO(pendingMessage)) {
                        continue;
                    }

                    processMessage(pendingMessage);
                    iterator.remove();
                    working = true;
                    break;
                }
            }
        }
    }

    private static boolean isCausalityViolatedFIFO(Message pendingMessage) {
        Map<Integer, Integer> senderVectorClock = pendingMessage.getSenderVectorClock();
        if (senderVectorClock == null) return true;

        int senderId = pendingMessage.getOriginalSenderInfo().id();

        for (Map.Entry<Integer, Integer> entry : senderVectorClock.entrySet()) {
            int id = entry.getKey();
            int clock = entry.getValue();
            int localClock = vectorClock.getOrDefault(id, 0);

            if (id == senderId) if (clock != localClock + 1) return true;
            else if (clock > localClock) return true;
        }

        return false;
    }

    private static void processPendingMessagesCausal() {
        boolean working = true;

        while (working) {
            working = false;

            synchronized (lock) {
                Iterator<Message> iterator = pendingMessages.iterator();
                Map<Integer, Integer> myVectorClock = getVectorClock();

                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    BasicMessage basicMessage = (BasicMessage) pendingMessage;

                    if (!otherClockGreater(myVectorClock, basicMessage.getSenderVectorClock())) {
                        processMessage(pendingMessage);
                        iterator.remove();
                        working = true;
                        break;
                    }
                }
            }
        }
    }

    private static void processMessage(Message pendingMessage) {
        BasicMessage basicMessage = (BasicMessage) pendingMessage;
        AppConfig.timestampedStandardPrint("Committing: " + pendingMessage);
        incrementClock(pendingMessage.getOriginalSenderInfo().id());

        try {
            switch (basicMessage.getMessageType()) {
                case TRANSACTION -> {
                    if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                        executor.submit(new TransactionHandler(basicMessage, snapshotCollector.getBitcakeManager()));
                    }
                }
                case AB_SNAPSHOT_REQUEST -> {
                    if (receivedAbRequest.add(basicMessage)) {
                        executor.submit(new ABSnapshotRequestHandler(basicMessage, snapshotCollector));
                    }
                }
                case AB_SNAPSHOT_RESPONSE -> {
                    if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                        executor.submit(new ABSnapshotResponseHandler(basicMessage, snapshotCollector));
                    }
                }
                case AV_MARKER -> {
                    executor.submit(new AVMarkerHandler(basicMessage, snapshotCollector.getBitcakeManager().getCurrentBitcakeAmount()));
                }
                case AV_DONE -> {
                    if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                        executor.submit(new AVDoneHandler(basicMessage, snapshotCollector));
                    }
                }
                case AV_TERMINATE -> {
                    executor.submit(new AVTerminateHandler(basicMessage, snapshotCollector));
                }
                case CC_SNAPSHOT_REQUEST -> {
                    AppConfig.timestampedStandardPrint("Processing CC_SNAPSHOT_REQUEST: " + basicMessage);
                    executor.submit(new CCSnapshotRequestHandler(basicMessage, snapshotCollector));
                }
                case CC_SNAPSHOT_RESPONSE -> {
                    if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                        executor.submit(new CCSnapshotResponseHandler(basicMessage, snapshotCollector));
                    }
                }
                case CC_RESUME -> {
                    executor.submit(new CCResumeHandler(basicMessage, snapshotCollector));
                }
                default -> {
                    AppConfig.timestampedErrorPrint("Unhandled message type: " + basicMessage.getMessageType());
                }
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error handling message " + basicMessage + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a given transaction should be recorded for the AV snapshot.
     * If the vector clock condition is met, the transferred amount is stored in the input channel.
     * <p>
     * The `markersVectorClock` is the recorded vector clock when the snapshot starts.
     * Comparing the sender's vector clock with `markersVectorClock` determines whether
     * the transaction occurred after the marker was sent and should be included.
     * </p>
     * @param senderVectorClock the vector clock of the transaction's sender
     * @param neighbor the ID of the neighbor sending or receiving this transaction
     * @param amount the amount of bitcake involved in the transaction
     */
    public static void recordTransaction(Map<Integer, Integer> senderVectorClock, int neighbor, int amount) {
        if (markerVectorClock == null) return;

        if (senderVectorClock.get(initiatorId) <= markerVectorClock.get(initiatorId)) {
            int oldAmount;
            if (inputChannel.get(neighbor) == null) oldAmount = 0;
            else oldAmount = inputChannel.get(neighbor);
            inputChannel.put(neighbor, oldAmount + amount);
        }
    }

    /**
     * Increments the vector clock entry for a specific servent.
     *
     * @param serventId the identifier of the servent.
     */
    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, (key, oldValue) -> oldValue + 1);
    }

    public static void injectSnapshotCollector(SnapshotCollector snapshotCollector) {
        CausalBroadcast.snapshotCollector = snapshotCollector;
    }

    public static void addPendingMessage(Message msg) {
        if (msg.getMessageType() == MessageType.CC_SNAPSHOT_REQUEST) {
            AppConfig.timestampedStandardPrint("Adding CC_SNAPSHOT_REQUEST to pending messages: " + msg);
        }
        pendingMessages.add(msg);
    }

    public static void addReceivedMessage(Message receivedMessage) {
        received.add(receivedMessage);
    }

    public static void addSentMessage(Message sentMessage) {
        sent.add(sentMessage);
    }

}
