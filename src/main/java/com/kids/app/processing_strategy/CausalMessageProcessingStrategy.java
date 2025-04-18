package com.kids.app.processing_strategy;

import com.kids.app.VectorClock;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.BasicMessage;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Causal implementation of the message processing strategy.
 * <p>
 * Ensures messages are processed according to causal order.
 */
@RequiredArgsConstructor
public class CausalMessageProcessingStrategy implements MessageProcessingStrategy {
    
    private final Queue<Message> pendingMessages;
    private final VectorClock vectorClock;
    private final Consumer<Message> messageProcessor;
    private final Object lock;
    
    @Override
    public boolean processPendingMessages() {
        boolean processed = false;
        boolean working = true;

        while (working) {
            working = false;

            synchronized (lock) {
                Iterator<Message> iterator = pendingMessages.iterator();

                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    
                    if (canProcessMessage(pendingMessage)) {
                        messageProcessor.accept(pendingMessage);
                        iterator.remove();
                        working = true;
                        processed = true;
                        break;
                    }
                }
            }
        }
        
        return processed;
    }
    
    @Override
    public boolean canProcessMessage(Message message) {
        if (message == null) return false;
        
        BasicMessage basicMessage = (BasicMessage) message;
        return !vectorClock.isOtherClockGreater(basicMessage.getSenderVectorClock());
    }
} 