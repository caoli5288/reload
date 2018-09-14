package com.mengcraft.reload;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 16-9-10.
 */
public enum Ticker implements Runnable {

    INST;

    private final TBox x = new TBox("short", 60);
    private final TBox y = new TBox("medium", 300);
    private final TBox z = new TBox("long", 900);

    private final AtomicLong tick = new AtomicLong();

    @Override
    public void run() {
        tick.addAndGet(20);
    }

    public void update() {
        long i = tick.get();
        if (i >= 1) {
            long now = Instant.now().getEpochSecond();
            x.update(now, i);
            y.update(now, i);
            z.update(now, i);
        }
    }

    public long tick() {
        return tick.get();
    }

    public float getShort() {
        return x.getValue().floatValue();
    }

    public float getMedium() {
        return y.getValue().floatValue();
    }

    public float getLong() {
        return z.getValue().floatValue();
    }

}
