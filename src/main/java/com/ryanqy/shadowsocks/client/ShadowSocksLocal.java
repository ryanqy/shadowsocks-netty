package com.ryanqy.shadowsocks.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryanqy.shadowsocks.client.config.ShadowSocksLocalConfig;
import com.ryanqy.shadowsocks.client.handler.ShadowSocksLocalHandler;
import com.ryanqy.shadowsocks.client.manager.ProxyManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksLocal {

    private static ObjectMapper mapper = new ObjectMapper();

    private static ShadowSocksLocalConfig loadConfig() throws IOException {
        ShadowSocksLocalConfig shadowSocksLocalConfig = mapper.readValue(new File("config/local.json"), ShadowSocksLocalConfig.class);
        if (CollectionUtils.isEmpty(shadowSocksLocalConfig.getConfigs())) {
            throw new IllegalArgumentException("cannot find any remoteServers in config file");
        }
        return shadowSocksLocalConfig;
    }

    public static void main(String[] args) {
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        ShadowSocksLocalConfig shadowSocksLocalConfig;
        try {
            log.info("start load config from config/local.json");
            shadowSocksLocalConfig = loadConfig();
            log.info("load config from config/local.json completed");
        } catch (IOException e) {
            log.error("load config from config/local.json failed", e);
            return;
        }

        ProxyManager.getInstance().findProxyForURL("http://www.google.com");
        NioEventLoopGroup bossEventLoopGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerEventLoopGroup = shadowSocksLocalConfig.getWorkers() == null ? new NioEventLoopGroup() : new NioEventLoopGroup(shadowSocksLocalConfig.getWorkers());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.group(bossEventLoopGroup, workerEventLoopGroup);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new SocksPortUnificationServerHandler());
                    pipeline.addLast(new ShadowSocksLocalHandler(shadowSocksLocalConfig));
                }
            });

            ChannelFuture bindFuture = bootstrap.bind(shadowSocksLocalConfig.getLocalPort()).sync();
            log.info("start ss-local completed");
            bindFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            log.error("bind port:{} failed", shadowSocksLocalConfig.getLocalPort());
            Thread.currentThread().interrupt();
        } finally {
            bossEventLoopGroup.shutdownGracefully();
            workerEventLoopGroup.shutdownGracefully();
        }
    }

}
