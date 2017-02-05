package com.mengcraft.reload;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created on 17-2-5.
 */
public final class TBox {

    private static final float FULL = 20;

    private final String name;
    private final int period;
    private int time;
    private int latest;
    private float value = FULL;

    public TBox(String name, int period) {
        this.name = name;
        this.period = period;
    }

    public void update(int now, int tick) {
        if (time + period < now) {
            if (time > 0) {
                value = new BigDecimal(tick - latest).divide(new BigDecimal(now - time), 2, RoundingMode.HALF_UP).floatValue();
            }
            time = now;
            latest = tick;
        }
    }

    public float get() {
        return value;
    }

    @Override
    public String toString() {
        return "TBox(" + name + ":" + value + ")";
    }

}
