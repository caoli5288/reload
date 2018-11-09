package com.mengcraft.reload;


import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ticker implements Runnable {

    private int latest = -1;
    private int tick;

    private float shortVal = 20;
    private float mediumVal = 20;
    private float longVal = 20;

    @Override
    public void run() {
        // call in server thread per 20 ticks
        tick += 20;
    }

    public synchronized void update() {// use synchronized flush memory
        if (tick == 0) {// server not bootstrap done
            return;
        }
        // call in heartbeat thread per 15s
        if (latest == -1) {
            // just init kv pair in first update
            latest = tick;
            return;
        }
        int delta = tick - latest;
        latest = tick;
        // calc tps latest 15s. we trusted jvm always call update per 15s
        BigDecimal v = BigDecimal.valueOf(delta).divide(BigDecimal.valueOf(15), RoundingMode.HALF_UP);
        // update smoothed value
        shortVal = BigDecimal.valueOf(shortVal).multiply(BigDecimal.valueOf(3)).add(v).divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP).floatValue();
        mediumVal = BigDecimal.valueOf(mediumVal).multiply(BigDecimal.valueOf(19)).add(v).divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP).floatValue();
        longVal = BigDecimal.valueOf(longVal).multiply(BigDecimal.valueOf(59)).add(v).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).floatValue();
    }

    public long tick() {
        return tick;
    }

    public float getShort() {
        return shortVal;
    }

    public float getMedium() {
        return mediumVal;
    }

    public float getLong() {
        return longVal;
    }

}
