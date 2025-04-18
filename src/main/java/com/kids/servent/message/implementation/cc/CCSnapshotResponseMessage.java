package com.kids.servent.message.implementation.cc;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;

/**
 * Message sent by a node to the initiator to confirm that it has recorded its local state and contains the amount of bitcakes it had.
 */
public class CCSnapshotResponseMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -4114137381491357339L;

    public CCSnapshotResponseMessage(ServentInfo sender, ServentInfo receiver, int amount) {
        super(MessageType.CC_SNAPSHOT_RESPONSE, sender, receiver, receiver, String.valueOf(amount), null);
    }
    
    public int getAmount() {
        return Integer.parseInt(getMessageText());
    }
} 