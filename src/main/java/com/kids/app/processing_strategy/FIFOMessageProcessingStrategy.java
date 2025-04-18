package com.kids.app.processing_strategy;

import com.kids.app.VectorClock;
import com.kids.servent.message.Message;
import com.kids.servent.message.implementation.BasicMessage;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * FIFO implementation of the message processing strategy.
 * <p>
 * Ensures messages from the same sender are processed in order.
 */
@RequiredArgsConstructor
public class FIFOMessageProcessingStrategy implements MessageProcessingStrategy {
    
    private final Queue<Message> pendingMessages;
    private final VectorClock vectorClock;
    private final Consumer<Message> messageProcessor;
    private final Object lock;
    
    @Override
    public boolean processPendingMessages() {
        boolean processed = false;
        
        synchronized (lock) {
            boolean working = true;

            while (working) {
                working = false;
                Iterator<Message> iterator = pendingMessages.iterator();

                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    if (!canProcessMessage(pendingMessage)) continue;

                    messageProcessor.accept(pendingMessage);
                    iterator.remove();
                    working = true;
                    processed = true;
                    break;
                }
            }
        }
        
        return processed;
    }
    
    @Override
    public boolean canProcessMessage(Message message) {
        if (message == null) return false;
        
        BasicMessage basicMessage = (BasicMessage) message;
        return !isCausalityViolated(basicMessage);
    }
    
    /**
     * Checks if the FIFO causality requirement is violated for a message.
     * 
     * @param message The message to check
     * @return true if causality is violated, false otherwise
     */
    private boolean isCausalityViolated(BasicMessage message) {
        if (message.getSenderVectorClock() == null) return true;
        
        int senderId = message.getOriginalSenderInfo().id();
        return vectorClock.isCausalityViolatedFIFO(message.getSenderVectorClock(), senderId);
    }
} 