package com.kids.app;

import com.kids.app.handler_factory.MessageHandlerFactory;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.app.processing_strategy.CausalMessageProcessingStrategy;
import com.kids.app.processing_strategy.FIFOMessageProcessingStrategy;
import com.kids.app.processing_strategy.MessageProcessingStrategy;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Singleton class that manages the causal broadcast mechanism in a distributed system.
 * It handles message processing, vector clock management, and snapshot collection.
 */
public class CausalBroadcast {

    @Getter private static final CausalBroadcast instance;

    private final ExecutorService executor = Executors.newWorkStealingPool();

    @Getter private final VectorClock vectorClock = new VectorClock();
    private final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    @Getter private SnapshotCollector snapshotCollector;

    // AB Snapshot
    @Getter private final List<Message> sent = new CopyOnWriteArrayList<>();
    @Getter private final List<Message> received = new CopyOnWriteArrayList<>();
    private final Set<BasicMessage> receivedAbRequest = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // AV Snapshot
    @Getter @Setter private int initiatorId;
    @Getter @Setter private int recordedAmount;
    @Getter @Setter private Map<Integer, Integer> markerVectorClock = null;
    private final Map<Integer, Integer> inputChannel = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> outputChannel = new ConcurrentHashMap<>();

    // Message processing strategies
    private MessageProcessingStrategy fifoStrategy;
    private MessageProcessingStrategy causalStrategy;
    private MessageHandlerFactory messageHandlerFactory;

    static {
        instance = new CausalBroadcast();
    }

    private CausalBroadcast() {
        initializeStrategies();
    }

    private void initializeStrategies() {
        Consumer<Message> messageProcessor = this::processMessage;

        this.fifoStrategy = new FIFOMessageProcessingStrategy(pendingMessages, vectorClock, messageProcessor, lock);
        this.causalStrategy = new CausalMessageProcessingStrategy(pendingMessages, vectorClock, messageProcessor, lock);
    }

    /**
     * Initializes the vector clock with the given number of servents.
     *
     * @param serventCount the number of servents in the system.
     */
    public void initializeVectorClock(int serventCount) {
        vectorClock.initialize(serventCount);
    }

    /**
     * Increments the causal clock based on a new message.
     * Also checks for and processes any pending messages.
     *
     * @param newMessage the message triggering the clock update.
     */
    public void causalClockIncrement(Message newMessage) {
        AppConfig.timestampedStandardPrint("Committing # " + newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().id());
        checkPendingMessages();
    }

    /**
     * Checks the pending messages queue and commits messages that satisfy the causal order.
     */
    public void checkPendingMessages() {
        if (AppConfig.IS_FIFO) fifoStrategy.processPendingMessages();
        else causalStrategy.processPendingMessages();
    }

    /**
     * Process a message using the appropriate handler
     *
     * @param pendingMessage the message to process
     */
    private void processMessage(Message pendingMessage) {
        BasicMessage basicMessage = (BasicMessage) pendingMessage;
        AppConfig.timestampedStandardPrint("Committing: " + pendingMessage);
        incrementClock(pendingMessage.getOriginalSenderInfo().id());

        try {
            if (messageHandlerFactory == null) {
                handleMessageWithFallback(basicMessage);
                return;
            }
            
            Runnable handler = messageHandlerFactory.createHandler(basicMessage);
            if (handler != null) executor.submit(handler);
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error handling message " + basicMessage + ": " + e.getMessage());
        }
    }
    
    /**
     * Fallback handler for when messageHandlerFactory is not yet initialized
     */
    private void handleMessageWithFallback(BasicMessage message) {
        AppConfig.timestampedStandardPrint("Using fallback handler for message: " + message);
        // Store the message to process it later when the factory is available
        pendingMessages.add(message);
    }

    /**
     * Checks if a given transaction should be recorded for the AV snapshot.
     * If the vector clock condition is met, the transferred amount is stored in the input channel.
     *
     * @param senderVectorClock the vector clock of the transaction's sender
     * @param neighbor the ID of the neighbor sending or receiving this transaction
     * @param amount the amount of bitcake involved in the transaction
     */
    public void recordTransaction(Map<Integer, Integer> senderVectorClock, int neighbor, int amount) {
        if (markerVectorClock == null) return;

        if (senderVectorClock.get(initiatorId) <= markerVectorClock.get(initiatorId)) {
            inputChannel.compute(neighbor, (key, oldValue) -> (oldValue == null ? 0 : oldValue) + amount);
        }
    }

    /**
     * Increments the vector clock entry for a specific servent.
     *
     * @param serventId the identifier of the servent.
     */
    public void incrementClock(int serventId) {
        vectorClock.increment(serventId);
    }

    /**
     * Injects a snapshot collector into the broadcast system.
     * Also initializes the message handler factory which depends on it.
     *
     * @param snapshotCollector the snapshot collector to inject
     */
    public void injectSnapshotCollector(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
        this.messageHandlerFactory = new MessageHandlerFactory(snapshotCollector, receivedAbRequest);
        
        // Process any pending messages that were waiting for the factory
        if (!pendingMessages.isEmpty()) checkPendingMessages();
    }

    /**
     * Adds a message to the pending queue.
     *
     * @param msg the message to add
     */
    public void addPendingMessage(Message msg) {
        if (msg.getMessageType() == MessageType.CC_SNAPSHOT_REQUEST) {
            AppConfig.timestampedStandardPrint("Adding CC_SNAPSHOT_REQUEST to pending messages: " + msg);
        }
        pendingMessages.add(msg);
    }

    /**
     * Adds a received message to the list.
     *
     * @param receivedMessage the received message
     */
    public void addReceivedMessage(Message receivedMessage) {
        received.add(receivedMessage);
    }

    /**
     * Adds a sent message to the list.
     *
     * @param sentMessage the sent message
     */
    public void addSentMessage(Message sentMessage) {
        sent.add(sentMessage);
    }

    public Map<Integer, Integer> getInputChannel() {
        return Collections.unmodifiableMap(inputChannel);
    }

    public Map<Integer, Integer> getOutputChannel() {
        return Collections.unmodifiableMap(outputChannel);
    }
    
    /**
     * For compatibility with existing code that uses the static vector clock
     * 
     * @return The current vector clock values as a Map
     */
    public Map<Integer, Integer> getVectorClockValues() {
        return vectorClock.getClockValues();
    }
}
