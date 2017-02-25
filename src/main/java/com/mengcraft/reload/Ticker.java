package com.mengcraft.reload;

/**
 * Created on 16-9-10.
 */
public class Ticker implements Runnable {

    private final TBox bshort;
    private final TBox bmed;
    private final TBox blong;

    private final Main main;

    private int tick;
    private boolean debug;


    public Ticker(Main main) {
        this.main = main;
        debug = main.getConfig().getBoolean("debug");
        bshort = new TBox("short", 60);
        bmed = new TBox("medium", 300);
        blong = new TBox("long", 900);
    }

    @Override
    public void run() {
        tick = tick + 20;
    }

    public void update() {
        if (tick > 0) {
            int now = Main.now();
            bshort.update(now, tick);
            bmed.update(now, tick);
            blong.update(now, tick);
            if (debug) {
                main.log("Server current tick " + tick + ", latest 15m's TPS " + bshort.get() + ", " + bmed.get() + ", " + blong.get());
            }
        }
    }

    public int tick() {
        return tick;
    }

    public float getShort() {
        return bshort.get();
    }

    public float getMed() {
        return bmed.get();
    }

    public float getLong() {
        return blong.get();
    }

}
