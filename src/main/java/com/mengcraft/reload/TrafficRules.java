package com.mengcraft.reload;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrafficRules extends ProxySelector {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final List<Rule> rules;
    private final ProxySelector superSelector;

    public TrafficRules(ProxySelector superSelector, List<Map<?, ?>> trafficRules) {
        this.superSelector = superSelector;
        rules = trafficRules.stream()
                .map(Rule::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<Proxy> select(URI uri) {
        for (Rule rule : rules) {
            RuleAction action = rule.apply(uri);
            Preconditions.checkState(action != RuleAction.REJECT, "%s is denied by %s", uri, rule);
            switch (action) {
                case PASS:
                    continue;
                case LOG:
                    info(uri);
                    continue;
                case TRACE:
                    info(uri);
                    Thread.dumpStack();
                    continue;
                case ACCEPT:
                    return superSelector.select(uri);
            }
        }
        return superSelector.select(uri);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        superSelector.connectFailed(uri, sa, ioe);
    }

    private void info(URI uri) {
        Map<String, Object> json = Maps.newHashMap();
        json.put("context", Thread.currentThread().getName());
        json.put("scheme", uri.getScheme());
        json.put("host", uri.getAuthority());
        json.put("path", Utils.getPathQuery(uri));
        Bukkit.getLogger().info("[NM]," + gson.toJson(json));
    }

    @Data
    public static class Rule implements Function<URI, RuleAction> {

        private RuleMatcher[] matchers;
        private RuleAction action;

        @Override
        public RuleAction apply(URI uri) {
            for (RuleMatcher matcher : matchers) {
                if (!matcher.matches(uri)) {
                    return RuleAction.PASS;
                }
            }
            return action;
        }

        public static Rule from(Map<?, ?> map) {
            Preconditions.checkState(map.containsKey("action"), "action not found");
            Rule rule = new Rule();
            rule.setAction(RuleAction.valueOf(map.get("action").toString()));
            List<RuleMatcher> list = Lists.newArrayList();
            if (map.containsKey("scheme")) {
                list.add(new SchemeRuleMatcher(map.get("scheme").toString()));
            }
            if (map.containsKey("host")) {
                list.add(new HostRuleMatcher(map.get("host").toString()));
            }
            if (map.containsKey("path")) {
                list.add(new PathRuleMatcher(map.get("path").toString()));
            }
            if (map.containsKey("context")) {
                list.add(new ContextRuleMatcher(map.get("context").toString()));
            }
            rule.setMatchers(list.toArray(new RuleMatcher[0]));
            return rule;
        }
    }

    public enum RuleAction {
        PASS,
        LOG,
        TRACE,
        ACCEPT,
        REJECT
    }

    public static abstract class RuleMatcher {

        private final Pattern pattern;

        RuleMatcher(String desc) {
            pattern = Pattern.compile(desc);
        }

        public boolean matches(URI uri) {
            String extract = extract(uri);
            return pattern.matcher(extract).matches();
        }

        protected abstract String extract(URI uri);
    }

    public static class SchemeRuleMatcher extends RuleMatcher {

        public SchemeRuleMatcher(String desc) {
            super(desc);
        }

        @Override
        protected String extract(URI uri) {
            return uri.getScheme();
        }
    }

    public static class HostRuleMatcher extends RuleMatcher {

        public HostRuleMatcher(String desc) {
            super(desc);
        }

        @Override
        protected String extract(URI uri) {
            return uri.getAuthority();
        }
    }

    public static class PathRuleMatcher extends RuleMatcher {

        public PathRuleMatcher(String desc) {
            super(desc);
        }

        @Override
        protected String extract(URI uri) {
            return Utils.getPathQuery(uri);
        }
    }

    public static class ContextRuleMatcher extends RuleMatcher {

        public ContextRuleMatcher(String desc) {
            super(desc);
        }

        @Override
        protected String extract(URI uri) {
            return Thread.currentThread().getName();
        }
    }
}
