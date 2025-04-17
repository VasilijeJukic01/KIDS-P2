package com.kids.servent.message.implementation.ab;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;
import java.util.Map;

/**
 * Represents a request message for initiating an AB snapshot.
 * This message is used to request a snapshot from a specific node in the distributed system.
 */
public class ABSnapshotRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1887472498490324672L;

    public ABSnapshotRequestMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AB_SNAPSHOT_REQUEST, sender, receiver, neighbor, senderVectorClock);
    }

}
