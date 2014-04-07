package net.minecrell.minecraftproxy;

import lombok.Getter;
import net.minecrell.minecraftproxy.logging.ProxyLogger;
import net.minecrell.minecraftproxy.network.NetworkManager;
import net.minecrell.minecraftproxy.util.Closeable;
import net.minecrell.minecraftproxy.util.Helper;
import net.minecrell.minecraftproxy.util.Startable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Getter
public final class MinecraftProxy implements Startable, Closeable {
    private boolean started = false;

    private final ProxyLogger logger;

    private final InetSocketAddress localAddress;
    private final ServerAddress remoteAddress;

    private NetworkManager networkManager;

    public MinecraftProxy(InetSocketAddress localAddress, ServerAddress remoteAddress) {
        this.localAddress = Helper.argumentNotNull(localAddress, "Local address");
        this.remoteAddress = remoteAddress;

        if (remoteAddress == null)
            throw new UnsupportedOperationException("Dynamic remote addresses are not supported yet!");

        this.logger = new ProxyLogger();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                MinecraftProxy.this.close();
            }
        });
    }

    @Override
    public boolean start() {
        Helper.checkState(!this.isStarted(), "Proxy already started!");
        started = true;

        this.getLogger().info("Initializing Proxy...");

        this.getLogger().info("Starting Network Manager...");

        this.networkManager = new NetworkManager(this);
        if (!networkManager.start()) {
            this.close(); return false;
        }

        this.getLogger().info("Network Manager successfully loaded and listening for requests.");

        try {
            networkManager.join();
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Unable to join network thread!", e);
            this.close(); return false;
        }

        return true;
    }

    @Override
    public void close() {
        Helper.checkState(this.isStarted(), "Proxy is not started!");
        this.getLogger().warning("Stopping Proxy Server!");

        if (networkManager != null) {
            this.getLogger().info("Stopping Network Manager...");
            networkManager.close();
        }

        this.getLogger().info("Closing file logger...");
        this.getLogger().close();

        this.getLogger().info("Proxy Server successfully stopped!");
    }

    private static void exitUsage() {
        System.err.println("Usage: MinecraftProxy <Host:Port> [RemoteHost:RemotePort]");
        System.exit(-2);
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) exitUsage();
        ServerAddress localAddress = ServerAddress.parse(args[0]);
        if (localAddress == null) exitUsage();
        InetSocketAddress localSocketAddress = localAddress.resolveAddressSafely();
        if (localSocketAddress == null) System.exit(-1);

        ServerAddress remoteAddress = null;
        if (args.length == 2 && ((remoteAddress = ServerAddress.parse(args[1])) == null)) exitUsage();

        InetSocketAddress remoteSocketAddress = null;

        if (remoteAddress != null) {
            if ((remoteSocketAddress = remoteAddress.resolveAddressSafely()) == null) System.exit(-1);

            if (localAddress.equals(remoteAddress) || localSocketAddress.equals(remoteSocketAddress)) {
                System.err.println("Local address cannot be equal to the remote address!");
                System.exit(-2);
            }
        }

        try (InputStream titleStream = MinecraftProxy.class.getClassLoader().getResourceAsStream("TITLE")) {
            if (titleStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(titleStream, StandardCharsets.UTF_8));
                String line = null;

                while ((line = reader.readLine()) != null) System.out.println(line);
                System.out.println();
            }
        } catch (Exception ignored) {}

        System.out.println("This proxy server is running " +
                MinecraftProxy.class.getPackage().getImplementationTitle() +
                " v" + MinecraftProxy.class.getPackage().getImplementationVersion() + "."
        );
        System.out.println();

        System.out.println("Listening to: " + localSocketAddress);
        System.out.println("Proxying to: " + ((remoteSocketAddress != null) ? remoteSocketAddress.getAddress() : "Dynamic"));

        System.out.println();

        if (!new MinecraftProxy(localSocketAddress, remoteAddress).start()) System.exit(-1);
    }
}
