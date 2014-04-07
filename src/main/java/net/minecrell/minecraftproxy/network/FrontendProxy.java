package net.minecrell.minecraftproxy.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import net.minecrell.minecraftproxy.network.proxy.ProxyBase;
import net.minecrell.minecraftproxy.network.proxy.ProxyType;

import java.net.InetSocketAddress;

public final class FrontendProxy extends ProxyBase {
    private InetSocketAddress remoteAddress;
    private volatile Channel outboundChannel;

    public FrontendProxy(NetworkManager manager) {
        super(manager);
    }

    @Override
    public ProxyType getProxyType() {
        return ProxyType.FRONTEND;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.remoteAddress = manager.getRemoteAddress().resolveAddressSafely(manager.getLogger());
        if (remoteAddress == null) ctx.channel().close();

        manager.getLogger().info("Proxying " + ctx.channel().remoteAddress() + " to " + remoteAddress);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        manager.getLogger().info("Disconnected " + ctx.channel().remoteAddress() + " from " + remoteAddress);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        final Channel inboundChannel = ctx.channel();

        ChannelFuture f = new Bootstrap().group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new BackendProxy(manager, inboundChannel))
                .option(ChannelOption.AUTO_READ, false)
                .connect(remoteAddress);

        this.outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess())
                    inboundChannel.read();
                else inboundChannel.close();
            }
        });
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess())
                        ctx.channel().read();
                    else ctx.channel().close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (outboundChannel != null) closeOnFlush(outboundChannel);
    }
}
