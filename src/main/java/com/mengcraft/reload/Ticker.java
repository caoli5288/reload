package com.mengcraft.reload;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created on 16-9-10.
 */
public class Ticker implements Runnable {

    private float result = 20;
    private int tick;
    private int latest;
    private int t;

    @Override
    public void run() {
        tick += 10;
    }

    public void update() {
        int time = Main.unixTime();
        if (latest > 0) {
            result = new BigDecimal(tick - t).divide(new BigDecimal(time - latest), 2, RoundingMode.HALF_UP).floatValue();
        }
        t = tick;
        latest = time;
    }

    public float get() {
        return result;
    }

}
