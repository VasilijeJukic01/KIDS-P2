package com.kids.servent.message.implementation.av;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;
import java.util.Map;

/**
 * This message is used to signal the termination of the AV process between servents.
 */
public class AVTerminateMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 3271045368214793650L;

    public AVTerminateMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_TERMINATE, sender, receiver, neighbor, senderVectorClock);
    }

}
