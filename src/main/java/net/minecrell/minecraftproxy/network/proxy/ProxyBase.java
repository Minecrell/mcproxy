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
