package com.kids.servent.message.implementation;

import com.kids.app.AppConfig;
import com.kids.app.servent.ServentInfo;
import com.kids.servent.message.Message;
import com.kids.servent.message.MessageType;
import lombok.Getter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a response message for an AB snapshot.
 * This message contains the snapshot data, including sent and received transactions.
 */
@Getter
public class ABSnapshotResponseMessage extends BasicMessage{

    @Serial
    private static final long serialVersionUID = 1932837451964281053L;
    private final List<Message> sent;
    private final List<Message> received;

    public ABSnapshotResponseMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock, int amount, List<Message> sentTransactions, List<Message> receivedTransactions) {
        super(MessageType.AB_SNAPSHOT_RESPONSE, sender, receiver, neighbor, String.valueOf(amount), senderVectorClock);

        this.sent = new CopyOnWriteArrayList<>(sentTransactions);
        this.received = new CopyOnWriteArrayList<>(receivedTransactions);
    }

    protected ABSnapshotResponseMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo originalReceiverInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList,
                                        String messageText, int messageId, List<Message> sentTransactions, List<Message> receivedTransactions) {
        super(type, originalSenderInfo, originalReceiverInfo, receiverInfo, senderVectorClock, routeList, messageText, messageId);

        this.sent = new CopyOnWriteArrayList<>(sentTransactions);
        this.received = new CopyOnWriteArrayList<>(receivedTransactions);
    }

    /**
     * Creates a new ABSnapshotResponseMessage with updated route information.
     *
     * @return A new ABSnapshotResponseMessage with the current node added to the route.
     */
    @Override
    public Message makeMeASender() {
        ServentInfo newRouteItem = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
        newRouteList.add(newRouteItem);

        return new ABSnapshotResponseMessage(
                getMessageType(),
                getOriginalSenderInfo(),
                getOriginalReceiverInfo(),
                getReceiverInfo(),
                getSenderVectorClock(),
                newRouteList,
                getMessageText(),
                getMessageId(),
                getSent(),
                getReceived()
        );
    }

    /**
     * Creates a new ABSnapshotResponseMessage with a changed receiver.
     *
     * @param newReceiverId The ID of the new receiver.
     * @return A new ABSnapshotResponseMessage with the updated receiver, or null if the receiver is not a neighbor.
     */
    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.neighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return new ABSnapshotResponseMessage(
                    getMessageType(),
                    getOriginalSenderInfo(),
                    getOriginalReceiverInfo(),
                    newReceiverInfo,
                    getSenderVectorClock(),
                    getRoute(),
                    getMessageText(),
                    getMessageId(),
                    getSent(),
                    getReceived()
            );
        } else {
            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
            return null;
        }
    }
}
