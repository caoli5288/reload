package com.mengcraft.reload;

/**
 * Created on 16-9-10.
 */
public class Ticker implements Runnable {

    private final Main main;
    private final TBox t1;
    private final TBox t2;
    private final TBox t3;

    private int i;
    private boolean debug;


    public Ticker(Main main) {
        this.main = main;
        debug = main.getConfig().getBoolean("debug");
        t1 = new TBox("short", 60);
        t2 = new TBox("medium", 300);
        t3 = new TBox("long", 900);
    }

    @Override
    public void run() {
        i = i + 20;
    }

    public void update() {
        if (i > 0) {
            int now = Main.unixTime();
            t1.update(now, i);
            t2.update(now, i);
            t3.update(now, i);
            if (debug) {
                main.log("Server current tick " + i + ", latest 15m's TPS " + t1.get() + ", " + t2.get() + ", " + t3.get());
            }
        }
    }

    public int i() {
        return i;
    }

    public float get1() {
        return t1.get();
    }

    public float get2() {
        return t2.get();
    }

    public float get3() {
        return t3.get();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
