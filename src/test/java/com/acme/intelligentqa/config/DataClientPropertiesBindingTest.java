package com.acme.intelligentqa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joyintech.datahub.springboot.config.DataClientProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 验证 DataClientProperties 配置项的绑定行为与属性完整性。
 */
class DataClientPropertiesBindingTest {

    /**
     * 验证所有 DataHub 属性可以正确绑定至 DataClientProperties 实体。
     */
    @Test
    void bindsAllDataHubPropertiesSuccessfully() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("datahub.config.enable", "true");
        properties.put("datahub.config.app-code", "APP_001");
        properties.put("datahub.config.server-name", "server_001");
        properties.put("datahub.config.api-key", "api_key_test");
        properties.put("datahub.config.conf-url", "http://datahub.example.com/conf");
        properties.put("datahub.config.conf-key", "conf_key_test");
        properties.put("datahub.config.connect-timeout-sec", "15");
        properties.put("datahub.config.read-timeout-sec", "20");
        properties.put("datahub.config.health-url", "http://datahub.example.com/health");
        properties.put("datahub.config.initial-delay", "30");
        properties.put("datahub.config.period", "60");
        properties.put("datahub.config.temp-directory", "/var/tmp/datahub");
        properties.put("datahub.config.endpoint", "obs.example.com");
        properties.put("datahub.config.access-key", "ak_123");
        properties.put("datahub.config.secret-key", "sk_456");
        properties.put("datahub.config.bucket-name", "bucket_test");
        properties.put("datahub.config.object-path", "data/products");
        properties.put("datahub.config.stack-trace-limit", "10");
        properties.put("datahub.config.notify-if-empty", "Y");
        properties.put("datahub.config.obs-upload-conf.task-num", "8");
        properties.put("datahub.config.obs-upload-conf.part-size", "100");
        properties.put("datahub.config.obs-upload-conf.listener", "true");
        properties.put("datahub.config.obs-upload-conf.enable-checkpoint", "false");

        final ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        final Binder binder = new Binder(source);
        final DataClientProperties target = new DataClientProperties();
        binder.bind("datahub.config", Bindable.ofInstance(target));

        assertTrue(target.getEnable());
        assertEquals("APP_001", target.getAppCode());
        assertEquals("server_001", target.getServerName());
        assertEquals("api_key_test", target.getApiKey());
        assertEquals("http://datahub.example.com/conf", target.getConfUrl());
        assertEquals("conf_key_test", target.getConfKey());
        assertEquals(15, target.getConnectTimeoutSec());
        assertEquals(20, target.getReadTimeoutSec());
        assertEquals("http://datahub.example.com/health", target.getHealthUrl());
        assertEquals(30L, target.getInitialDelay());
        assertEquals(60L, target.getPeriod());
        assertEquals("/var/tmp/datahub", target.getTempDirectory());
        assertEquals("obs.example.com", target.getEndpoint());
        assertEquals("ak_123", target.getAccessKey());
        assertEquals("sk_456", target.getSecretKey());
        assertEquals("bucket_test", target.getBucketName());
        assertEquals("data/products", target.getObjectPath());
        assertEquals(10, target.getStackTraceLimit());
        assertEquals("Y", target.getNotifyIfEmpty());

        assertNotNull(target.getObsUploadConf());
        assertEquals(8, target.getObsUploadConf().getTaskNum());
        assertEquals(100L, target.getObsUploadConf().getPartSize());
        assertTrue(target.getObsUploadConf().getListener());
        assertFalse(target.getObsUploadConf().getEnableCheckpoint());
    }
}
