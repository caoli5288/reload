package com.mengcraft.reload;

import java.util.List;
import java.util.regex.Pattern;

public class Utils {

    public static final Pattern PATTERN_TIME = Pattern.compile("^\\d\\d:\\d\\d(:\\d\\d)?$");

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(List<?> l) {
        return l == null || l.isEmpty();
    }
}
