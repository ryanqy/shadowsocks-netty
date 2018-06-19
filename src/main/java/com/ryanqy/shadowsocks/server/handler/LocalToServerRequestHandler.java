package com.ryanqy.shadowsocks.server.handler;

import com.ryanqy.shadowsocks.crypt.Crypt;
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
public class LocalToServerRequestHandler extends ChannelInboundHandlerAdapter {

    private Crypt crypt;

    private Channel localToServerChannel;

    public LocalToServerRequestHandler(Crypt crypt, Channel localToServerChannel) {
        this.crypt = crypt;
        this.localToServerChannel = localToServerChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf requestPrintByteBuf = (ByteBuf) msg;
        ByteBuf requestByteBuf = requestPrintByteBuf.copy();
        int length = requestPrintByteBuf.readableBytes();
        byte[] bytes = new byte[length];
        requestPrintByteBuf.readBytes(bytes);

        ChannelUtils.writeDecryptMessageAndFlush(crypt, localToServerChannel, requestByteBuf);
    }
}
