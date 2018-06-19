package com.ryanqy.shadowsocks.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@ToString
public class ShadowSocksDestinationInfo implements DecoderResultProvider {

    @Getter
    @Setter
    private final Socks5AddressType dstAddrType;

    @Getter
    @Setter
    private final String dstAddr;

    @Getter
    @Setter
    private final int dstPort;

    private DecoderResult decoderResult = DecoderResult.SUCCESS;

    @Override
    public DecoderResult decoderResult() {
        return decoderResult;
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        if (decoderResult == null) {
            throw new NullPointerException("decoderResult");
        }
        this.decoderResult = decoderResult;
    }

    public ShadowSocksDestinationInfo(Socks5AddressType dstAddrType, String dstAddr, int dstPort) {
        this.dstAddrType = dstAddrType;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

    public ByteBuf getByteBuf() {
        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(dstAddrType.byteValue());

        if (Socks5AddressType.IPv4.equals(dstAddrType) || Socks5AddressType.IPv6.equals(dstAddrType)) {
            buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(dstAddr));
            buf.writeShort(dstPort);
        } else if (Socks5AddressType.DOMAIN.equals(dstAddrType)) {
            buf.writeByte(dstAddr.length());
            buf.writeBytes(dstAddr.getBytes(CharsetUtil.US_ASCII));
            buf.writeShort(dstPort);
        }

        return buf;
    }

}
