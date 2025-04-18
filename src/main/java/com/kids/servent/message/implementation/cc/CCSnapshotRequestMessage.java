package com.kids.servent.message.implementation.cc;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;
import lombok.Getter;

import java.io.Serial;

/**
 * Message used to request a snapshot during Coordinated Checkpointing.
 */
@Getter
public class CCSnapshotRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -3114137381691356339L;
    
    private final int initiatorId;

    public CCSnapshotRequestMessage(ServentInfo sender, ServentInfo receiver, int initiatorId) {
        super(MessageType.CC_SNAPSHOT_REQUEST, sender, receiver, receiver, String.valueOf(initiatorId), null);
        this.initiatorId = initiatorId;
    }

} 