package net.minecrell.minecraftproxy.dns;

import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetSocketAddress;
import java.util.Hashtable;

public final class MinecraftDNS {
    private MinecraftDNS() {}

    public static final String SRV_SERVICE = "minecraft";

    private static final String DNS_PROVIDER_PACKAGE = "com.sun.jndi.dns";
    private static final String DNS_PROVIDER = DNS_PROVIDER_PACKAGE + ".DnsContextFactory";

    public static InetSocketAddress resolveSRVRecord(String host) throws Exception {
        try {
            Class.forName(DNS_PROVIDER);
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_PROVIDER);
            env.put(Context.PROVIDER_URL, "dns:");
            env.put(DNS_PROVIDER_PACKAGE + ".timeout.retries", "1");

            DirContext ctx = new InitialDirContext(env);
            Attributes records = ctx.getAttributes("_" + SRV_SERVICE + "._tcp." + host, new String[] {"SRV"});

            String[] srv = records.get("srv").get().toString().split(" ", 4);

            return new InetSocketAddress(srv[3], Integer.parseInt(srv[2]));
        } catch (Throwable ignored) {}

        return null;
    }
}
