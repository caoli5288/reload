package com.mengcraft.reload;

/**
 * Created on 16-9-10.
 */
public enum Ticker implements Runnable {

    INST;

    private final TBox x = new TBox("short", 60);
    private final TBox y = new TBox("medium", 300);
    private final TBox z = new TBox("long", 900);

    private int tick;

    @Override
    public void run() {
        tick = tick + 20;
    }

    public void update() {
        if (tick > 0) {
            int now = ((int) (System.currentTimeMillis() / 1000));
            x.update(now, tick);
            y.update(now, tick);
            z.update(now, tick);
        }
    }

    public int tick() {
        return tick;
    }

    public float getShort() {
        return x.getValue();
    }

    public float getMed() {
        return y.getValue();
    }

    public float getLong() {
        return z.getValue();
    }

}
