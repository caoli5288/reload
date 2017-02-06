package com.mengcraft.reload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;

/**
 * Created on 17-2-5.
 */
public final class TBox {

    private static final float FULL = 20;

    private final LinkedList<Rec> list = new LinkedList<>();
    private final String name;
    private final int period;
    private float value = FULL;

    public TBox(String name, int period) {
        this.name = name;
        this.period = period;
    }

    static class Rec {

        int time;
        int tick;
    }

    public void update(int time, int tick) {
        Rec latest = new Rec();
        latest.time = time;
        latest.tick = tick;
        list.offer(latest);

        while (list.size() > 2 && list.peek().time + period < time) list.poll();
        if (list.size() > 1) {
            Rec head = list.peek();
            value = new BigDecimal(tick - head.tick).divide(new BigDecimal(time - head.time), 2, RoundingMode.HALF_UP).floatValue();
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
