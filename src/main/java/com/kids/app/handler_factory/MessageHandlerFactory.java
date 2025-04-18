package com.kids.app.handler_factory;

import com.kids.app.AppConfig;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.servent.handler.implementation.TransactionHandler;
import com.kids.servent.handler.implementation.ab.ABSnapshotRequestHandler;
import com.kids.servent.handler.implementation.ab.ABSnapshotResponseHandler;
import com.kids.servent.handler.implementation.av.AVDoneHandler;
import com.kids.servent.handler.implementation.av.AVMarkerHandler;
import com.kids.servent.handler.implementation.av.AVTerminateHandler;
import com.kids.servent.handler.implementation.cc.CCResumeHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotRequestHandler;
import com.kids.servent.handler.implementation.cc.CCSnapshotResponseHandler;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory class for creating message handlers based on message type.
 * <p>
 * Implements the Factory pattern to create appropriate handlers for different message types.
 */
public class MessageHandlerFactory {
    
    private final SnapshotCollector snapshotCollector;
    private final Set<BasicMessage> receivedAbRequest;

    /*
     * A map of message types to their corresponding handler creation functions.
     * This allows for easy retrieval of the appropriate handler based on the message type.
     */
    private final Map<MessageType, Function<BasicMessage, Runnable>> handlerCreators;
    
    /**
     * Creates a new MessageHandlerFactory with the given snapshot collector and received request set.
     * 
     * @param snapshotCollector the snapshot collector to use
     * @param receivedAbRequest the set of received AB snapshot requests
     * @throws IllegalArgumentException if snapshotCollector is null
     */
    public MessageHandlerFactory(SnapshotCollector snapshotCollector, Set<BasicMessage> receivedAbRequest) {
        if (snapshotCollector == null) {
            throw new IllegalArgumentException("SnapshotCollector cannot be null");
        }
        this.snapshotCollector = snapshotCollector;
        this.receivedAbRequest = receivedAbRequest;
        this.handlerCreators = initializeHandlerCreators();
    }
    
    private Map<MessageType, Function<BasicMessage, Runnable>> initializeHandlerCreators() {
        Map<MessageType, Function<BasicMessage, Runnable>> creators = new EnumMap<>(MessageType.class);
        
        // Protect against null pointer if BitcakeManager is not yet initialized
        creators.put(MessageType.TRANSACTION, msg -> {
            try {
                if (msg.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id() && 
                    snapshotCollector.getBitcakeManager() != null) {
                    return new TransactionHandler(msg, snapshotCollector.getBitcakeManager());
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating TransactionHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.AB_SNAPSHOT_REQUEST, msg -> {
            try {
                if (receivedAbRequest.add(msg)) {
                    return new ABSnapshotRequestHandler(msg, snapshotCollector);
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating ABSnapshotRequestHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.AB_SNAPSHOT_RESPONSE, msg -> {
            try {
                if (msg.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                    return new ABSnapshotResponseHandler(msg, snapshotCollector);
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating ABSnapshotResponseHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.AV_MARKER, msg -> {
            try {
                if (snapshotCollector.getBitcakeManager() != null) {
                    return new AVMarkerHandler(msg, snapshotCollector.getBitcakeManager().getCurrentBitcakeAmount());
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating AVMarkerHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.AV_DONE, msg -> {
            try {
                if (msg.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                    return new AVDoneHandler(msg, snapshotCollector);
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating AVDoneHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.AV_TERMINATE, msg -> {
            try {
                return new AVTerminateHandler(msg, snapshotCollector);
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating AVTerminateHandler: " + e.getMessage());
                return () -> {};
            }
        });
        
        creators.put(MessageType.CC_SNAPSHOT_REQUEST, msg -> {
            try {
                AppConfig.timestampedStandardPrint("Processing CC_SNAPSHOT_REQUEST: " + msg);
                return new CCSnapshotRequestHandler(msg, snapshotCollector);
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating CCSnapshotRequestHandler: " + e.getMessage());
                return () -> {};
            }
        });
        
        creators.put(MessageType.CC_SNAPSHOT_RESPONSE, msg -> {
            try {
                if (msg.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                    return new CCSnapshotResponseHandler(msg, snapshotCollector);
                }
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating CCSnapshotResponseHandler: " + e.getMessage());
            }
            return () -> {};
        });
        
        creators.put(MessageType.CC_RESUME, msg -> {
            try {
                return new CCResumeHandler(msg, snapshotCollector);
            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Error creating CCResumeHandler: " + e.getMessage());
                return () -> {};
            }
        });
        
        return creators;
    }
    
    /**
     * Creates a message handler for the given message.
     * 
     * @param message The message to create a handler for
     * @return A runnable handler for the message, or an empty runnable if no appropriate handler exists
     */
    public Runnable createHandler(BasicMessage message) {
        if (message == null) {
            AppConfig.timestampedErrorPrint("Cannot create handler for null message");
            return () -> {};
        }
        
        try {
            Function<BasicMessage, Runnable> creatorFunction = handlerCreators.get(message.getMessageType());
            
            if (creatorFunction != null) return creatorFunction.apply(message);
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Error creating handler for message " + message + ": " + e.getMessage());
        }
        
        AppConfig.timestampedErrorPrint("Unhandled message type: " + message.getMessageType());
        return () -> {};
    }
} 