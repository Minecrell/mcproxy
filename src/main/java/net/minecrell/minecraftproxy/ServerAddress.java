/*
 * MinecraftProxy - TCP-Proxy for Minecraft servers.
 * Written in 2013 by Minecrell <https://github.com/Minecrell>
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide.
 *
 * This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package net.minecrell.minecraftproxy;

import lombok.Value;
import net.minecrell.minecraftproxy.dns.MinecraftDNS;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Value
public class ServerAddress {
    public static final int DEFAULT_PORT = 25565;

    String host;
    Integer port;

    public ServerAddress(String host, final Integer port) {
        if (host != null && (host.isEmpty() || host.equals("*"))) host = null;
        this.host = host;
        this.port = port;
    }

    public ServerAddress(String host, String port) {
        this(host, (port != null && (port.length() > 0) ? Integer.parseInt(port) : null));
    }

    @Override
    public String toString() {
        return "Server Address [" + ((host != null) ? host : ".") + ((port != null) ? ":" + port : "") + "]";
    }

    private static InetSocketAddress testAddress(InetSocketAddress address) throws UnknownHostException {
        if (address.getAddress() == null) throw new UnknownHostException(address.toString());
        return address;
    }

    public InetSocketAddress resolveAddress() throws Exception {
        Integer port = this.port;

        if (host != null && port == null) {
            InetSocketAddress srvAddress = MinecraftDNS.resolveSRVRecord(host);
            if (srvAddress != null) return testAddress(srvAddress);
        }

        if (port == null) port = DEFAULT_PORT;
        return testAddress((host != null) ? new InetSocketAddress(host, port) : new InetSocketAddress(port));
    }

    public InetSocketAddress resolveAddressSafely() {
        try {
            return this.resolveAddress();
        } catch (Throwable e) {
            System.err.println("An error occurred while resolving the hostname of " + this.toString() + "!");
            e.printStackTrace();

            return null;
        }
    }

    public InetSocketAddress resolveAddressSafely(Logger logger) {
        logger.info("Resolving " + this.toString() + "...");

        try {
            return this.resolveAddress();
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "An error occurred while resolving the hostname of " + this.toString() + "!", e);
            return null;
        }
    }


    public static ServerAddress parse(String address) {
        if (address == null) return null;
        if (address.equals(":")) return new ServerAddress(null, (String) null);

        String[] host = address.split(":");
        if (host.length <= 0) return null;

        return new ServerAddress(host[0], (host.length > 1) ? host[1] : null);
    }
}
