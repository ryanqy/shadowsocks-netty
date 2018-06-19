package com.ryanqy.shadowsocks.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Data
public class ShadowSocksServerConfig {

    private String server;

    @JsonProperty("server_port")
    private int serverPort = 8388;

    @JsonProperty("local_address")
    private String localAddress = "127.0.0.1";

    @JsonProperty("local_port")
    private int localPort = 1080;

    private String password;

    private int timeout = 300;

    private String method = "aes-256-cfb";

    @JsonProperty("fast_open")
    private boolean fastOpen = false;

}
