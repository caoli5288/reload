package com.mengcraft.reload;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Created by on 10月12日.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Machine {

    enum Token {

        FLOW(machine -> "" + machine.flow),
        JOIN(machine -> "" + machine.join),
        LOGIN(machine -> "" + machine.login),
        MEMORY(machine -> "" + machine.memory()),
        ONLINE(machine -> "" + Bukkit.getOnlinePlayers().size()),
        TIME(machine -> "" + ((System.currentTimeMillis() - machine.now) / 1000)),
        TPS(machine -> "" + Main.getTicker().getShort()),
        ;

        final Function<Machine, String> func;
        final String key;

        Token(Function<Machine, String> func) {
            key = "${" + name().toLowerCase() + "}";
            this.func = func;
        }

    }

    final ScriptEngine engine;
    final String expr;
    final List<Token> list;
    final long now = System.currentTimeMillis();

    @Setter(value = AccessLevel.NONE)
    int flow;
    private int join;
    private int login;

    public void incFlow() {
        ++flow;
    }

    public void incLogin() {
        ++login;
    }

    public void incJoin() {
        ++join;
    }

    @SneakyThrows
    public boolean process() {
        return ((boolean) engine.eval(express(expr, list.iterator())));
    }

    String express(String expr, Iterator<Token> itr) {
        if (itr.hasNext()) {
            val token = itr.next();
            return express(expr.replace(token.key, token.func.apply(this)), itr);
        }
        return expr;
    }

    float memory() {
        val runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long free = runtime.freeMemory();
        long allocated = runtime.totalMemory();
        return new BigDecimal(allocated - free).divide(new BigDecimal(max), 2, RoundingMode.HALF_UP).floatValue();
    }

    public static Machine build(String expr) {
        ImmutableList.Builder<Token> b = ImmutableList.builder();
        for (Token token : Token.values()) {
            if (expr.contains(token.key)) b.add(token);
        }
        return new Machine(new ScriptEngineManager().getEngineByExtension("js"), expr, b.build());
    }

}
