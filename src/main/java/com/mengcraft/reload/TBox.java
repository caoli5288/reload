package com.mengcraft.reload;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;

/**
 * Created on 17-2-5.
 */
@Data
public final class TBox {

    private final LinkedList<Rec> list = new LinkedList<>();
    private final String name;
    private final int period;

    @Setter(value = AccessLevel.NONE)
    private float value = 20.0F;

    @AllArgsConstructor
    static class Rec {

        int time;
        int tick;
    }

    public void update(int time, int tick) {
        list.add(new Rec(time, tick));

        if (list.size() > 2 && list.element().time + period < time) list.remove();
        if (list.size() > 1) {
            Rec head = list.element();
            value = new BigDecimal(tick - head.tick).divide(new BigDecimal(time - head.time), 2, RoundingMode.HALF_UP).floatValue();
        }
    }

}
