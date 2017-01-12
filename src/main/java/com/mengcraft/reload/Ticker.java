package com.mengcraft.reload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 16-9-10.
 */
public class Ticker implements Runnable {

    private final AtomicInteger tick = new AtomicInteger();
    private final Main main;
    private final boolean debug;
    private float result = 20;
    private int latest;
    private int t;

    public Ticker(Main main) {
        this.main = main;
        debug = main.getConfig().getBoolean("debug");
    }

    @Override
    public void run() {
        tick.addAndGet(20);
    }

    public void update() {
        int i = tick.get();
        if (i > 0) {
            int time = Main.unixTime();
            if (latest > 0) {
                result = new BigDecimal(i - t).divide(new BigDecimal(time - latest), 2, RoundingMode.HALF_UP).floatValue();
                if (debug) {
                    main.log("New TPS value " + result);
                }
            }
            t = i;
            latest = time;
        }
    }

    public float get() {
        return result;
    }

}
