package com.x3bits.springaireactagent.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI ReAct Agent 配置属性
 */
@ConfigurationProperties(prefix = "spring.ai.react-agent")
public class ReActAgentProperties {

    /**
     * 是否启用ReAct Agent自动配置
     */
    private boolean enabled = true;

    /**
     * ThreadRepository存储类型
     * 可选值：mysql, memory
     * 默认：mysql
     */
    private String storageType = "mysql";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
}