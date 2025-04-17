package com.kids.servent.message.implementation.av;

import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.MessageType;
import com.kids.servent.message.implementation.BasicMessage;

import java.io.Serial;
import java.util.Map;

public class AVMarkerMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 4839201745839012476L;

    public AVMarkerMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_MARKER, sender, receiver, neighbor, senderVectorClock);
    }

}
