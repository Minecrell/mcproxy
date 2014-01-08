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

package net.minecrell.minecraftproxy.network.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import net.minecrell.minecraftproxy.network.NetworkManager;
import net.minecrell.minecraftproxy.util.Helper;

import java.util.logging.Level;

public abstract class ProxyBase extends ChannelInboundHandlerAdapter {
    @Getter protected final NetworkManager manager;

    protected ProxyBase(NetworkManager manager) {
        this.manager = Helper.argumentNotNull(manager, "Network Manager");
    }

    public abstract ProxyType getProxyType();

    public String getRemote(ChannelHandlerContext ctx) {
        return "[" + this.getProxyType().getDisplayName() + "|" + ctx.channel().remoteAddress() + "]";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        manager.getLogger().log(Level.WARNING,
                "An error occurred while connecting with " + this.getRemote(ctx),
                cause
        );

        closeOnFlush(ctx.channel());
    }

    protected static void closeOnFlush(Channel ch) {
        if (ch.isActive()) ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
