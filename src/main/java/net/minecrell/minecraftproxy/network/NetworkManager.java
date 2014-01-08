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

package net.minecrell.minecraftproxy.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import net.minecrell.minecraftproxy.MinecraftProxy;
import net.minecrell.minecraftproxy.ServerAddress;
import net.minecrell.minecraftproxy.util.Closeable;
import net.minecrell.minecraftproxy.util.Helper;
import net.minecrell.minecraftproxy.util.Startable;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetworkManager implements Startable, Closeable {
    @Getter
    private final MinecraftProxy proxy;

    private final EventLoopGroup bossGroup, workerGroup;
    private Channel channel;

    public NetworkManager(MinecraftProxy proxy) {
        this.proxy = Helper.argumentNotNull(proxy, "Proxy");

        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
    }

    public InetSocketAddress getLocalAddress() {
        return this.getProxy().getLocalAddress();
    }

    public ServerAddress getRemoteAddress() {
        return this.getProxy().getRemoteAddress();
    }

    public Logger getLogger() {
        return this.getProxy().getLogger();
    }

    @Override
    public boolean isStarted() {
        return channel != null;
    }

    @Override
    public boolean start() {
        this.getLogger().info("Starting Network Listener on " + this.getProxy().getLocalAddress() + "...");

        try {
            this.channel = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new FrontendProxy(NetworkManager.this)
                            );
                        }
                    }).childOption(ChannelOption.AUTO_READ, false)
                    .bind(this.getLocalAddress()).sync().channel();
            return true;
        } catch (Throwable e) {
            this.getLogger().log(Level.SEVERE, "An internal error occurred while starting the Network Listener!", e);
            this.close();
        }

        return false;
    }

    public void join() throws Exception {
        Helper.checkState(this.isStarted(), "Network Manager is not started!");
        this.channel.closeFuture().sync();
    }

    public void close() {
        if (bossGroup.isShuttingDown() || bossGroup.isShuttingDown()) return;

        this.getLogger().warning("Closing Network Listener...");

        try {
            if (channel != null) channel.close().awaitUninterruptibly();
        } catch (Throwable e) {
            this.getLogger().log(Level.SEVERE, "An internal error occurred while closing the Network Channel!", e);
        }

        try {
            bossGroup.shutdownGracefully();
        } catch (Throwable ignored) {}
        try {
            workerGroup.shutdownGracefully();
        } catch (Throwable ignored) {}

        this.getLogger().info("Network Listener closed.");
    }
}
