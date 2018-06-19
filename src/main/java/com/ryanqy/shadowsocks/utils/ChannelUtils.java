package com.ryanqy.shadowsocks.utils;

import com.ryanqy.shadowsocks.crypt.Crypt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
public class ChannelUtils {

    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static void writeCryptMessageAndFlush(Crypt crypt, Channel channel, ByteBuf buf) {
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        channel.writeAndFlush(Unpooled.wrappedBuffer(crypt.encrypt(bytes)));
    }

    public static void writeDecryptMessageAndFlush(Crypt crypt, Channel channel, ByteBuf buf) {
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        channel.writeAndFlush(Unpooled.copiedBuffer(crypt.decrypt(bytes)));
    }

}
