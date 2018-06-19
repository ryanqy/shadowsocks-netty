package com.ryanqy.shadowsocks.client.handler;

import com.ryanqy.shadowsocks.crypt.Crypt;
import com.ryanqy.shadowsocks.utils.ChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class LocalToClientResponseHandler extends ChannelInboundHandlerAdapter {

    private Crypt crypt;

    private Channel clientToLocalChannel;

    private boolean isProxy;

    public LocalToClientResponseHandler(Crypt crypt, Channel clientToLocalChannel, boolean isProxy) {
        this.crypt = crypt;
        this.clientToLocalChannel = clientToLocalChannel;
        this.isProxy = isProxy;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ByteBuf responsePrintByteBuf = (ByteBuf) msg;
            ByteBuf responseByteBuf = responsePrintByteBuf.copy();
            int length = responsePrintByteBuf.readableBytes();
            byte[] bytes = new byte[length];
            responsePrintByteBuf.readBytes(bytes);
            log.info("receive message from ss-server to ss-local, content:\n" + new String(crypt.decrypt(bytes)));
            if (clientToLocalChannel.isActive()) {
                if (isProxy) {
                    ChannelUtils.writeDecryptMessageAndFlush(crypt, clientToLocalChannel, responseByteBuf);
                } else {
                    clientToLocalChannel.writeAndFlush(responseByteBuf);
                }
            }
        } finally {
//            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(clientToLocalChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(clientToLocalChannel);
    }

}
