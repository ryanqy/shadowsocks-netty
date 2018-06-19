package com.ryanqy.shadowsocks.server.handler;

import com.ryanqy.shadowsocks.crypt.Crypt;
import com.ryanqy.shadowsocks.crypt.CryptFactory;
import com.ryanqy.shadowsocks.server.config.ShadowSocksServerConfig;
import com.ryanqy.shadowsocks.utils.ChannelUtils;
import com.ryanqy.shadowsocks.utils.ShadowSocksDestinationInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksServerHandler extends SimpleChannelInboundHandler<ShadowSocksDestinationInfo> {

    private Crypt crypt;

    private ShadowSocksServerConfig shadowSocksServerConfig;

    public ShadowSocksServerHandler(ShadowSocksServerConfig shadowSocksServerConfig) {
        this.shadowSocksServerConfig = shadowSocksServerConfig;
        this.crypt = CryptFactory.get(shadowSocksServerConfig.getMethod(), shadowSocksServerConfig.getPassword());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShadowSocksDestinationInfo destinationInfo) {
        log.info("received destination info:{}", destinationInfo);
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(eventLoopGroup);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, shadowSocksServerConfig.getTimeout());

        bootstrap.handler(new RemoteToServerResponseHandler(ctx.channel()));

        bootstrap.connect(destinationInfo.getDstAddr(), destinationInfo.getDstPort()).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("connect to remote host:{} port:{} failed", destinationInfo.getDstAddr(), destinationInfo.getDstPort());
                ChannelUtils.closeOnFlush(ctx.channel());
            } else {
                log.info("connect to remote host:{} port:{} success", destinationInfo.getDstAddr(), destinationInfo.getDstPort());
                ctx.pipeline().remove(ShadowSocksServerHandler.class);
                ctx.pipeline().addLast(new LocalToServerRequestHandler(crypt, future.channel()));
            }
        });
    }

}
