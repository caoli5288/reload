package com.mengcraft.reload;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;

/**
 * Created on 17-2-5.
 */
@Data
public final class TBox {

    private final LinkedList<Rec> queue = new LinkedList<>();
    private final String name;
    private final int period;

    @Setter(value = AccessLevel.NONE)
    private float value = 20.0F;

    public void update(int time, int tick) {
        queue.add(new Rec(time, tick));

        if (queue.size() > 2 && queue.element().time + period < time) queue.remove();
        if (queue.size() > 1) {
            Rec head = queue.element();
            value = new BigDecimal(tick - head.tick).divide(new BigDecimal(time - head.time), 2, RoundingMode.HALF_UP).floatValue();
        }
    }

    @RequiredArgsConstructor
    private static class Rec {

        private final int time;
        private final int tick;
    }

}
