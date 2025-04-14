package com.kids.app.snapshot_bitcake.acharya_badrinath;

import com.kids.servent.message.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class ABSnapshot implements Serializable {
    private final int serventId;
    private final int amount;
    private List<Message> sent = new CopyOnWriteArrayList<>();
    private List<Message> received = new CopyOnWriteArrayList<>();
}
