package com.kids.app.snapshot_bitcake.acharya_badrinath;

import com.kids.app.snapshot_bitcake.BitcakeManager;

import java.util.concurrent.atomic.AtomicInteger;

public class ABBitcakeManager implements BitcakeManager {

    private final AtomicInteger amount = new AtomicInteger(1000);

    @Override
    public void takeSomeBitcakes(int amount) {
        this.amount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        this.amount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return amount.get();
    }

}
