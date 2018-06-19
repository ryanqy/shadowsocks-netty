package com.ryanqy.shadowsocks.client.manager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author roger01.wu
 * @date 2018/6/17
 */
@Slf4j
public class ProxyManager {

    private static ProxyManager ourInstance = new ProxyManager();

    public static ProxyManager getInstance() {
        return ourInstance;
    }

    private Invocable invocable;

    private static final String DIRECT = "DIRECT";

    private LoadingCache<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    List<String> parameters = Lists.newArrayList();
                    parameters.add(key);
                    parameters.add(new URL(key).getHost());
                    return (String) invocable.invokeFunction("FindProxyForURL", parameters.toArray());
                }
            });

    private ProxyManager() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        File file = new File("config/gfwlist.js");
        try {
            engine.eval(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        } catch (ScriptException | IOException e) {
            log.error("load gfwlist file failed");
            System.exit(1);
        }
        this.invocable = (Invocable) engine;

    }

    public boolean isProxy(String url) {
        url = "http://" + url;
        return !this.findProxyForURL(url).contains(DIRECT);
    }

    public List<String> findProxyForURL(String url) {
        try {
            String str = cache.get(url);
            return Arrays.stream(StringUtils.split(str, ";")).map(String::trim).collect(Collectors.toList());
        } catch (ExecutionException e) {
            log.error("find proxy for url:{} failed", url);
        }

        return Lists.newArrayList(DIRECT);
    }

}
