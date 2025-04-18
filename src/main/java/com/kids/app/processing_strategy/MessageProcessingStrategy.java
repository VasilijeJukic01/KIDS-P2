package com.kids.app.processing_strategy;

import com.kids.servent.message.Message;

/**
 * Strategy interface for processing pending messages.
 * <p>
 * This follows the Strategy design pattern to allow different message processing algorithms.
 */
public interface MessageProcessingStrategy {
    
    /**
     * Process pending messages according to the specific strategy.
     * 
     * @return true if at least one message was processed, false otherwise
     */
    boolean processPendingMessages();
    
    /**
     * Checks if a message should be processed based on the strategy's rules.
     * 
     * @param message The message to check
     * @return true if the message can be processed, false otherwise
     */
    boolean canProcessMessage(Message message);
} 