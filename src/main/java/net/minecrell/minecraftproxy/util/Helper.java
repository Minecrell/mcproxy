package net.minecrell.minecraftproxy.util;

public final class Helper {
    private Helper() {}

    public static <T> T argumentNotNull(T argument, String name) {
        if (argument == null) throw new IllegalArgumentException(name + " cannot be null!");
        return argument;
    }

    public static void checkState(boolean state, String message) {
        if (!state) throw new IllegalStateException(message);
    }
}
