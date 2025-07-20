package com.x3bits.springaireactagent.starter;

import com.x3bits.springaireactagent.web.controller.ChatController;
import com.x3bits.springaireactagent.web.controller.ThreadController;
import com.x3bits.springaireactagent.web.repository.ThreadRepository;
import com.x3bits.springaireactagent.web.repository.MysqlThreadRepository;
import com.x3bits.springaireactagent.web.repository.MemoryThreadRepository;
import com.x3bits.springaireactagent.web.service.ThreadService;
import com.x3bits.springaireactagent.web.config.WebConfig;
import com.x3bits.springaireactagent.core.ReActAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI ReAct Agent 自动配置类
 * 
 * 自动配置ChatController、ThreadController及其依赖的Bean
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass({
        ReActAgent.class,
        JdbcTemplate.class
})
@ConditionalOnProperty(prefix = "spring.ai.react-agent", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ReActAgentProperties.class)
public class ReActAgentAutoConfiguration {

    /**
     * 自动配置ThreadRepository
     * 
     * @param properties   配置属性
     * @param jdbcTemplate JdbcTemplate实例（可选，当使用mysql时需要）
     * @return ThreadRepository实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadRepository threadRepository(ReActAgentProperties properties,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        String storageType = properties.getStorageType();

        switch (storageType.toLowerCase()) {
            case "memory":
                return new MemoryThreadRepository();
            case "mysql":
                if (jdbcTemplate == null) {
                    throw new IllegalStateException(
                            "JdbcTemplate is required when using mysql storage type. " +
                                    "Please configure a DataSource or switch to memory storage type.");
                }
                return new MysqlThreadRepository();
            default:
                throw new IllegalArgumentException(
                        "Unsupported storage type: " + storageType +
                                ". Supported types are: mysql, memory");
        }
    }

    /**
     * 自动配置ThreadService
     * 
     * @param threadRepository ThreadRepository实例
     * @return ThreadService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadService threadService(ThreadRepository threadRepository) {
        return new ThreadService();
    }

    /**
     * 自动配置ChatController
     *
     * @param applicationContext Spring应用上下文，用于获取ReActAgent Bean
     * @return ChatController实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ReActAgent.class)
    public ChatController chatController(ApplicationContext applicationContext) {
        return new ChatController(applicationContext);
    }

    /**
     * 自动配置ThreadController
     *
     * @return ThreadController实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadController threadController() {
        return new ThreadController();
    }

    /**
     * 自动配置WebConfig
     *
     * @return WebConfig实例
     */
    @Bean
    @ConditionalOnMissingBean
    public WebConfig webConfig() {
        return new WebConfig();
    }
}