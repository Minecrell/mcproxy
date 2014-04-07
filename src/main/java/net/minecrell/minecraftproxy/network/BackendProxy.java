package net.minecrell.minecraftproxy.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecrell.minecraftproxy.network.proxy.ProxyBase;
import net.minecrell.minecraftproxy.network.proxy.ProxyType;
import net.minecrell.minecraftproxy.util.Helper;

public final class BackendProxy extends ProxyBase {
    private final Channel inboundChannel;

    protected BackendProxy(NetworkManager manager, Channel inboundChannel) {
        super(manager);
        this.inboundChannel = Helper.argumentNotNull(inboundChannel, "Inbound channel");
    }


    @Override
    public ProxyType getProxyType() {
        return ProxyType.BACKEND;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess())
                    ctx.channel().read();
                else future.channel().close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeOnFlush(inboundChannel);
    }
}
