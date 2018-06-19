package com.ryanqy.shadowsocks.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryanqy.shadowsocks.server.config.ShadowSocksServerConfig;
import com.ryanqy.shadowsocks.server.handler.ShadowSocksServerHandler;
import com.ryanqy.shadowsocks.utils.ShadowSocksDestinationInfoDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksServer {

    private static ObjectMapper mapper = new ObjectMapper();

    private static ShadowSocksServerConfig loadConfig() throws IOException {
        return mapper.readValue(new File("config/shadowsocks.json"), ShadowSocksServerConfig.class);
    }

    public static void main(String[] args) {
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        ShadowSocksServerConfig shadowSocksServerConfig;
        try {
            log.info("start load server config from config/shadowsocks.json");
            shadowSocksServerConfig = loadConfig();
            log.info("load server config from config/shadowsocks.json completed");
        } catch (IOException e) {
            log.info("oad server config from config/shadowsocks.json failed", e);
            return;
        }

        NioEventLoopGroup bossEventLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerEventLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.group(bossEventLoopGroup, workerEventLoopGroup);

            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new ShadowSocksDestinationInfoDecoder());
                    ch.pipeline().addLast(new ShadowSocksServerHandler(shadowSocksServerConfig));
                }
            });

            ChannelFuture bindFuture = bootstrap.bind(shadowSocksServerConfig.getLocalAddress(), shadowSocksServerConfig.getLocalPort()).sync();
            log.info("start ss-server at address:{} port:{} success", shadowSocksServerConfig.getLocalAddress(), shadowSocksServerConfig.getLocalPort());
            bindFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("bind port:{} failed", shadowSocksServerConfig.getLocalPort());
            Thread.currentThread().interrupt();
        } finally {
            bossEventLoopGroup.shutdownGracefully();
            workerEventLoopGroup.shutdownGracefully();
        }

    }

}
