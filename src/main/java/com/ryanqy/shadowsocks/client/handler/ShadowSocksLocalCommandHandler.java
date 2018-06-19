package com.ryanqy.shadowsocks.client.handler;

import com.ryanqy.shadowsocks.client.config.ShadowSocksLocalConfig;
import com.ryanqy.shadowsocks.client.manager.ProxyManager;
import com.ryanqy.shadowsocks.crypt.Crypt;
import com.ryanqy.shadowsocks.crypt.CryptFactory;
import com.ryanqy.shadowsocks.utils.ChannelUtils;
import com.ryanqy.shadowsocks.utils.ShadowSocksDestinationInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksLocalCommandHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {

    private ShadowSocksLocalConfig shadowSocksLocalConfig;

    private ShadowSocksLocalConfig.RemoteServer remoteServer;

    private Crypt crypt;

    public ShadowSocksLocalCommandHandler(ShadowSocksLocalConfig shadowSocksLocalConfig) {
        this.shadowSocksLocalConfig = shadowSocksLocalConfig;
        remoteServer = shadowSocksLocalConfig.getRemoteServer();
        crypt = CryptFactory.get(remoteServer.getMethod(), remoteServer.getPassword());
    }

    private ShadowSocksDestinationInfo getDestinationInfo(Socks5CommandRequest commandRequest) {
        return new ShadowSocksDestinationInfo(commandRequest.dstAddrType(), commandRequest.dstAddr(), commandRequest.dstPort());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5CommandRequest request) {
        //socks客户端到ss-local之间的channel
        Channel clientToLocalChannel = ctx.channel();

        log.info("receive client socks command request:{}, channel:{}", request, ctx.channel());

//        boolean isProxy = "autoProxyMode".equals(shadowSocksLocalConfig.getMode()) && ProxyManager.getInstance().isProxy(request.dstAddr());
        boolean isProxy = true;

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(eventLoopGroup);
        bootstrap.handler(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                Channel localToServerChannel = ctx.channel();
                clientToLocalChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType())).addListener(listener -> {
                    if (!listener.isSuccess()) {
                        log.info("send socks command response to client failed, channel:{}", clientToLocalChannel);
                        ChannelUtils.closeOnFlush(clientToLocalChannel);
                        ChannelUtils.closeOnFlush(localToServerChannel);
                    } else {
                        log.info("send socks command response to client success, channel:{}", clientToLocalChannel);

                        if (isProxy) {
                            ShadowSocksDestinationInfo destinationInfo = getDestinationInfo(request);
                            log.info("ss-local send destination info to ss-server, destination info:{}", destinationInfo);
                            ChannelUtils.writeCryptMessageAndFlush(crypt, localToServerChannel, destinationInfo.getByteBuf());
                        }

                        ctx.pipeline().remove(this);
                        clientToLocalChannel.pipeline().remove(ShadowSocksLocalCommandHandler.this);
                        clientToLocalChannel.pipeline().remove(Socks5ServerEncoder.class);

                        localToServerChannel.pipeline().addLast(new LocalToClientResponseHandler(crypt, clientToLocalChannel, isProxy));
                        clientToLocalChannel.pipeline().addLast(new LocalToServerRequestHandler(crypt, localToServerChannel, isProxy));
                    }
                });
            }
        });
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, shadowSocksLocalConfig.getTimeout());
        if (isProxy) {
            log.info("ss-local start connect to host:{}, port:{}, isProxy:true", remoteServer.getServer(), remoteServer.getServerPort());
        } else {
            log.info("ss-local start connect to host:{}, port:{}, isProxy:false", request.dstAddr(), request.dstPort());
        }
        ChannelFuture connectFuture = isProxy ? bootstrap.connect(remoteServer.getServer(), remoteServer.getServerPort()) : bootstrap.connect(request.dstAddr(), request.dstPort());
        connectFuture.addListener((ChannelFutureListener) listener -> {
            if (!listener.isSuccess()) {
                if (isProxy) {
                    log.error("ss-local connect to host:{}, port:{}, isProxy:true failed", remoteServer.getServer(), remoteServer.getServerPort());
                } else {
                    log.error("ss-local connect to host:{}, port:{}, isProxy:false failed", request.dstAddr(), request.dstPort());
                }
                clientToLocalChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.CONNECTION_REFUSED, request.dstAddrType()));
                clientToLocalChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            } else {
                if (isProxy) {
                    log.info("ss-local connect to host:{}, port:{}, isProxy:true success", remoteServer.getServer(), remoteServer.getServerPort());
                } else {
                    log.info("ss-local connect to host:{}, port:{}, isProxy:false success", request.dstAddr(), request.dstPort());
                }
            }
        });

    }

}
