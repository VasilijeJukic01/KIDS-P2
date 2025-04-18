package com.kids.servent.message.implementation.cc;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;

/**
 * Message sent by the initiator to all nodes to inform them that the snapshot is complete and they can resume normal operation.
 */
public class CCResumeMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -5114137681491356339L;

    public CCResumeMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.CC_RESUME, sender, receiver, receiver, "RESUME", null);
    }
} 