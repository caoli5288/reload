package com.mengcraft.reload.command.control;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.List;

public class If implements ICallable {

    private final List<ICallable> conditions = Lists.newArrayList();
    private final List<ICallable> commands = Lists.newArrayList();
    private final List<ICallable> altCommands = Lists.newArrayList();
    private Flow flow = Flow.INIT;
    private Deque<String> deque;

    public void parse(String[] commands) {
        Preconditions.checkState(flow == Flow.INIT);
        flow = Flow.IF;
        deque = Lists.newLinkedList();
        for (String command : commands) {
            deque.add(command.trim());
        }
        parse();
    }

    private void parse() {
        while (!isDone() && !deque.isEmpty()) {
            String cmd = deque.pop();
            if (flow == Flow.IF) {
                ifCmd(cmd);
            } else if (flow == Flow.THEN) {
                thenCmd(cmd);
            } else if (flow == Flow.ELSE) {
                elseCmd(cmd);
            }
        }
        Preconditions.checkState(isDone(), "syntax error");
    }

    private void ifCmd(String cmd) {
        String[] split = cmd.split(" ");
        switch (split[0]) {
            case "then": // jump to then
                Preconditions.checkState(!conditions.isEmpty(), "syntax error");
                Preconditions.checkState(split.length != 1, "syntax error");
                flow = Flow.THEN;
                thenCmd(StringUtils.join(split, ' ', 1, split.length));
                break;
            case "expr":
                Preconditions.checkState(split.length != 1, "syntax error");
                conditions.add(new ExprCallable(StringUtils.join(split, ' ', 1, split.length)));
                break;
            case "if":
                Preconditions.checkState(split.length != 1, "syntax error");
                deque.push(StringUtils.join(split, ' ', 1, split.length));
                conditions.add(ofChild());
                break;
            default:
                conditions.add(new CommandCallable(cmd));
                break;
        }
    }

    private void thenCmd(String cmd) {
        String[] split = cmd.split(" ");
        switch (split[0]) {
            case "fi":
                Preconditions.checkState(!commands.isEmpty(), "syntax error");
                flow = Flow.FI;
                break;
            case "else":
                Preconditions.checkState(!commands.isEmpty(), "syntax error");
                Preconditions.checkState(split.length != 1, "syntax error");
                flow = Flow.ELSE;
                elseCmd(StringUtils.join(split, ' ', 1, split.length));
                break;
            case "elif":
                Preconditions.checkState(!commands.isEmpty(), "syntax error");
                Preconditions.checkState(split.length != 1, "syntax error");
                flow = Flow.FI;
                deque.push(StringUtils.join(split, ' ', 1, split.length));
                altCommands.add(ofChild());
                break;
            case "if":
                Preconditions.checkState(split.length != 1, "syntax error");
                deque.push(StringUtils.join(split, ' ', 1, split.length));
                commands.add(ofChild());
                break;
            default:
                commands.add(new CommandCallable(cmd));
                break;
        }
    }

    private void elseCmd(String cmd) {
        String[] split = cmd.split(" ");
        switch (split[0]) {
            case "fi":
                Preconditions.checkState(!altCommands.isEmpty(), "syntax error");
                flow = Flow.FI;
                break;
            case "if":
                Preconditions.checkState(split.length != 1, "syntax error");
                deque.push(StringUtils.join(split, ' ', 1, split.length));
                altCommands.add(ofChild());
                break;
            default:
                commands.add(new CommandCallable(cmd));
                break;
        }
    }

    @NotNull
    private If ofChild() {
        If child = new If();
        child.flow = Flow.IF;
        child.deque = deque;
        child.parse();
        return child;
    }

    public boolean isDone() {
        return flow == Flow.FI;
    }

    @Override
    public boolean call(CommandSender caller) {
        Preconditions.checkState(isDone(), "syntax error");
        if (call(caller, conditions)) {
            return call(caller, commands);
        }
        return call(caller, altCommands);
    }

    private boolean call(CommandSender caller, List<ICallable> callables) {
        boolean b = true;// default to true if no callables
        for (ICallable callable : callables) {
            b = callable.call(caller);
        }
        return b;
    }

    enum Flow {
        INIT,
        IF,
        THEN,
        ELSE,
        FI;
    }
}
