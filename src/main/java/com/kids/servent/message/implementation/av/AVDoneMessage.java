package com.kids.servent.message.implementation.av;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;
import java.util.Map;

/**
 * This message is used to signal the completion of the AV process between servents.
 */
public class AVDoneMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1596738203465981043L;

    public AVDoneMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_DONE, sender, receiver, neighbor, senderVectorClock);
    }
}
