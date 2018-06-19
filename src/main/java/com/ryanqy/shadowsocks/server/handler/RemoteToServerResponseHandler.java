package com.ryanqy.shadowsocks.server.handler;

import com.ryanqy.shadowsocks.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class RemoteToServerResponseHandler extends ChannelInboundHandlerAdapter {

    private Channel serverToLocalChannel;

    public RemoteToServerResponseHandler(Channel serverToLocalChannel) {
        this.serverToLocalChannel = serverToLocalChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf responsePrintByteBuf = (ByteBuf) msg;
        ByteBuf responseByteBuf = responsePrintByteBuf.copy();
        int length = responsePrintByteBuf.readableBytes();
        byte[] bytes = new byte[length];
        responsePrintByteBuf.readBytes(bytes);
        log.info("receive message from remote to ss-server, content:\n{}", new String(bytes));

        serverToLocalChannel.writeAndFlush(responseByteBuf);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(serverToLocalChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(serverToLocalChannel);
    }
}
