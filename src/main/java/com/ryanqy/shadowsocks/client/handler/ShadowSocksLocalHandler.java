package com.ryanqy.shadowsocks.client.handler;

import com.ryanqy.shadowsocks.client.config.ShadowSocksLocalConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksLocalHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private ShadowSocksLocalConfig shadowSocksLocalConfig;

    public ShadowSocksLocalHandler(ShadowSocksLocalConfig shadowSocksLocalConfig) {
        this.shadowSocksLocalConfig = shadowSocksLocalConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        SocksVersion version = msg.version();
        if (version.equals(SocksVersion.SOCKS4a)) {
            return;
        }

        Socks5Message message = (Socks5Message) msg;
        if (message instanceof Socks5InitialRequest) {
            Socks5InitialRequest initialRequest = (Socks5InitialRequest) message;
            List<Socks5AuthMethod> socks5AuthMethods = initialRequest.authMethods();
            if (CollectionUtils.isEmpty(socks5AuthMethods) || !socks5AuthMethods.contains(Socks5AuthMethod.NO_AUTH)) {
                ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED));
                return;
            }
            ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (message instanceof Socks5CommandRequest) {
            Socks5CommandRequest commandRequest = (Socks5CommandRequest) message;
            Socks5CommandType commandType = commandRequest.type();
            ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
            if (commandType.equals(Socks5CommandType.CONNECT)) {
                ctx.pipeline().addLast(new ShadowSocksLocalCommandHandler(shadowSocksLocalConfig));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(message);
            } else {
                ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, commandRequest.dstAddrType()));
            }
        }
    }
}
