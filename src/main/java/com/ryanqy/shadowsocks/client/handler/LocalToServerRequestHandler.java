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
public class LocalToServerRequestHandler extends ChannelInboundHandlerAdapter {

    private Crypt crypt;

    private Channel localToServerChannel;

    private boolean isProxy;

    public LocalToServerRequestHandler(Crypt crypt, Channel localToServerChannel, boolean isProxy) {
        this.crypt = crypt;
        this.localToServerChannel = localToServerChannel;
        this.isProxy = isProxy;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ByteBuf requestPrintByteBuf = (ByteBuf) msg;
            ByteBuf requestByteBuf = requestPrintByteBuf.copy();
            int length = requestPrintByteBuf.readableBytes();
            byte[] bytes = new byte[length];
            requestPrintByteBuf.readBytes(bytes);
            log.info("receive client to ss-local message, content:\n{}", new String(bytes));
            if (localToServerChannel.isActive()) {
                if (isProxy) {
                    ChannelUtils.writeCryptMessageAndFlush(crypt, localToServerChannel, requestByteBuf);
                } else {
                    localToServerChannel.writeAndFlush(requestByteBuf);
                }
            }
        } finally {
//            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(localToServerChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelUtils.closeOnFlush(ctx.channel());
        ChannelUtils.closeOnFlush(localToServerChannel);
    }

}
