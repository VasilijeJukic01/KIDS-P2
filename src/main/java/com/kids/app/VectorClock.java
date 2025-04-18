package com.kids.app;

import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * VectorClock implementation for distributed system.
 * Manages vector clocks for causality tracking and message ordering.
 */
@NoArgsConstructor
public class VectorClock {

    private final Map<Integer, Integer> clockValues = new ConcurrentHashMap<>();
    
    /**
     * Creates a vector clock with specified number of nodes.
     * 
     * @param nodeCount The number of nodes in the system
     */
    public VectorClock(int nodeCount) {
        initialize(nodeCount);
    }
    
    /**
     * Creates a vector clock from an existing map of values.
     * 
     * @param values Existing clock values
     */
    public VectorClock(Map<Integer, Integer> values) {
        clockValues.putAll(values);
    }
    
    /**
     * Initializes the vector clock with zeros up to nodeCount.
     * 
     * @param nodeCount The number of nodes to initialize
     */
    public void initialize(int nodeCount) {
        Stream.iterate(0, i -> i + 1)
              .limit(nodeCount)
              .forEach(i -> clockValues.put(i, 0));
    }
    
    /**
     * Increments the clock value for a specific node.
     * 
     * @param nodeId The ID of the node to increment
     * @return This vector clock for method chaining
     */
    public VectorClock increment(int nodeId) {
        clockValues.computeIfPresent(nodeId, (k, v) -> v + 1);
        return this;
    }
    
    /**
     * Checks if any entry in other clock is greater than the corresponding entry in this clock.
     * 
     * @param other The other vector clock to compare with
     * @return true if other has at least one greater entry
     */
    public boolean isOtherClockGreater(VectorClock other) {
        if (clockValues.size() != other.clockValues.size()) {
            throw new IllegalArgumentException("Clocks are not same size");
        }
        
        return clockValues.entrySet().stream()
                .anyMatch(entry -> {
                    Integer otherValue = other.clockValues.get(entry.getKey());
                    return otherValue != null && otherValue > entry.getValue();
                });
    }
    
    /**
     * Compares with a map-based vector clock (for backward compatibility).
     * 
     * @param otherClock The other clock as a map
     * @return true if other has at least one greater entry
     */
    public boolean isOtherClockGreater(Map<Integer, Integer> otherClock) {
        return isOtherClockGreater(new VectorClock(otherClock));
    }
    
    /**
     * Returns an unmodifiable view of the clock values.
     * 
     * @return The current clock values
     */
    public Map<Integer, Integer> getClockValues() {
        return Collections.unmodifiableMap(clockValues);
    }
    
    /**
     * Creates a copy of this vector clock.
     * 
     * @return A new vector clock with the same values
     */
    public VectorClock copy() {
        return new VectorClock(clockValues);
    }
    
    /**
     * Checks if this vector clock satisfies FIFO ordering with the sender's clock.
     * 
     * @param senderClock The sender's vector clock
     * @param senderId The ID of the sender node
     * @return true if causality is violated, false otherwise
     */
    public boolean isCausalityViolatedFIFO(Map<Integer, Integer> senderClock, int senderId) {
        if (senderClock == null) {
            return true;
        }
        
        return senderClock.entrySet().stream()
                .anyMatch(entry -> {
                    int id = entry.getKey();
                    int clock = entry.getValue();
                    int localClock = clockValues.getOrDefault(id, 0);
                    
                    if (id == senderId) {
                        return clock != localClock + 1;
                    } else {
                        return clock > localClock;
                    }
                });
    }
    
    @Override
    public String toString() {
        return clockValues.toString();
    }
} 