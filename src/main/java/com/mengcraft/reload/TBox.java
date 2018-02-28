package com.mengcraft.reload;

import com.google.common.util.concurrent.AtomicDouble;
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
    private final AtomicDouble value = new AtomicDouble(20);

    public void update(long time, long tick) {
        queue.add(new Rec(time, tick));

        if (queue.size() > 2 && queue.element().time + period < time) queue.remove();
        if (queue.size() > 1) {
            Rec head = queue.element();
            value.set(new BigDecimal(tick - head.tick).divide(new BigDecimal(time - head.time), 2, RoundingMode.HALF_UP).doubleValue());
        }
    }

    @RequiredArgsConstructor
    private static class Rec {

        private final long time;
        private final long tick;
    }

}
