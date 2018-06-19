package com.ryanqy.shadowsocks.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ShadowSocksDestinationInfoDecoder extends ReplayingDecoder<ShadowSocksDestinationInfoDecoder.State> {

    enum State {
        INIT,
        SUCCESS,
        FAILURE
    }

    public ShadowSocksDestinationInfoDecoder() {
        super(ShadowSocksDestinationInfoDecoder.State.INIT);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            switch (state()) {
                case INIT: {
                    final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
                    final String dstAddr = Socks5AddressDecoder.DEFAULT.decodeAddress(dstAddrType, in);
                    final int dstPort = in.readUnsignedShort();

                    out.add(new ShadowSocksDestinationInfo(dstAddrType, dstAddr, dstPort));
                    checkpoint(ShadowSocksDestinationInfoDecoder.State.SUCCESS);
                }
                case SUCCESS: {
                    int readableBytes = actualReadableBytes();
                    if (readableBytes > 0) {
                        out.add(in.readRetainedSlice(readableBytes));
                    }
                    break;
                }
                case FAILURE: {
                    in.skipBytes(actualReadableBytes());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("decoder byte to ShadowSocksDestinationInfo failed", e);
            fail(out, e);
        }
    }

    private void fail(List<Object> out, Exception cause) {
        if (!(cause instanceof DecoderException)) {
            cause = new DecoderException(cause);
        }

        checkpoint(ShadowSocksDestinationInfoDecoder.State.FAILURE);

        ShadowSocksDestinationInfo m = new ShadowSocksDestinationInfo(Socks5AddressType.IPv4, "0.0.0.0", 1);
        m.setDecoderResult(DecoderResult.failure(cause));
        out.add(m);
    }

}
