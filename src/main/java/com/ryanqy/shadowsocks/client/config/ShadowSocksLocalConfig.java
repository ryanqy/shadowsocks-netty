package com.ryanqy.shadowsocks.client.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Data
public class ShadowSocksLocalConfig {

    private int localPort = 1080;

    private boolean shareOverLan = false;

    private Integer workers;

    private int timeout = 1000;

    private String mode;

    private List<RemoteServer> configs = Lists.newArrayList();

    @Data
    public static class RemoteServer {

        private String method = "rc4-md5";

        private String password;

        private String server;

        @JsonProperty("server_port")
        private int serverPort;

        private String remarks;

        private boolean selected = false;

    }

    public RemoteServer getRemoteServer() {
        Optional<RemoteServer> optionalConfig = configs.stream().filter(config -> config.selected).findFirst();
        return optionalConfig.orElseGet(() -> configs.get(0));
    }

}

