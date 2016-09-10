package com.mengcraft.reload;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created on 16-9-10.
 */
public class TickPerSecond implements Runnable {

    private float tickPerSecond = 20;

    private int tick;
    private int timeLatest;
    private int tickLatest;

    @Override
    public void run() {
        tick += 10;
    }

    public void update() {
        int time = Main.unixTime();
        if (timeLatest > 0) {
            tickPerSecond = new BigDecimal(tick - tickLatest).divide(new BigDecimal(time - timeLatest), 2, RoundingMode.HALF_UP).floatValue();
        }
        timeLatest = time;
        tickLatest = tick;
    }

    public float get() {
        return tickPerSecond;
    }

}
