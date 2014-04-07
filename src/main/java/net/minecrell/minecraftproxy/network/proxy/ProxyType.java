package net.minecrell.minecraftproxy.network.proxy;

public enum ProxyType {
    FRONTEND("CLIENT"), BACKEND("SERVER");

    private final String displayName;

    private ProxyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
