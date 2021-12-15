package com.mengcraft.reload;

import java.util.List;

public class Utils {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(List<?> l) {
        return l == null || l.isEmpty();
    }

    public static <T> T pick(List<T> list, int index) {
        if (list.size() > index) {
            return list.get(index);
        }
        return null;
    }
}
