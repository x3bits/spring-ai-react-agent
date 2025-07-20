package com.x3bits.springaireactagent.example.demos;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.core.RunAgentOptions;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.saver.jdbc.JdbcTemplateBranchMessageSaver;
import com.x3bits.springaireactagent.serializer.json.JsonMessageSerializer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;

/**
 * 演示将对话内容持久化到MySQL
 * 环境准备：
 * 创建数据库,最好使用MySQL 8.0及以上。更低的版本有对话轮数过多时相比MySQL 8.0会有比较大的性能差距
 * 创建以下这张表：
 CREATE TABLE `message_branch` (
 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键，用于排序',
 `message_id` varchar(64) NOT NULL COMMENT '消息唯一标识符',
 `thread_id` varchar(64) NOT NULL COMMENT '线程ID',
 `previous_id` varchar(64) DEFAULT NULL COMMENT '前一个消息ID，形成链式结构',
 `message_type` varchar(20) NOT NULL COMMENT '消息类型：USER/ASSISTANT/SYSTEM/TOOL',
 `message_content` text NOT NULL COMMENT '消息内容JSON',
 `depth` int NOT NULL DEFAULT '0' COMMENT '消息在树中的深度',
 `ancestor_path` text COMMENT '祖先路径，用逗号分隔的ID列表',
 `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（用于运维工具）',
 `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（用于运维工具）',
 PRIMARY KEY (`id`),
 UNIQUE KEY `message_id` (`message_id`),
 KEY `idx_thread_id` (`thread_id`) COMMENT '线程查询索引',
 KEY `idx_previous_id` (`previous_id`) COMMENT '前置消息查询索引',
 KEY `idx_thread_depth` (`thread_id`,`depth`) COMMENT '线程深度查询索引',
 KEY `idx_depth` (`depth`) COMMENT '深度查询索引',
 KEY `idx_thread_id_order` (`thread_id`,`id`) COMMENT '线程消息排序索引'
 ) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息分支存储表'
 * 把代码中的常量替换成你的配置
 */
public class MySqlPersistenceDemo {

    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    public static final String OPENAI_BASE_URL = System.getenv("OPENAI_BASE_URL");
    public static final String OPENAI_DEFAULT_MODEL = System.getenv("OPENAI_DEFAULT_MODEL");
    public static final String JDBC_URL = "jdbc:mysql://localhost:3306/spring_ai_react_agent";
    public static final String DB_USER_NAME = "root";
    public static final String DB_PASSWORD = "";

    private static JdbcTemplate createJdbcTemplate() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(JDBC_URL);
        dataSource.setUser(DB_USER_NAME);
        dataSource.setPassword(DB_PASSWORD);
        return new JdbcTemplate(dataSource);
    }

    private static ReActAgent createReActAgent() {
        //Spring AI的ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .apiKey(OPENAI_API_KEY)
                                .baseUrl(OPENAI_BASE_URL)
                                .build())
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(OPENAI_DEFAULT_MODEL)
                                .build())
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();
        // 消息做持久化时如何序列化
        JsonMessageSerializer messageSerializer = new JsonMessageSerializer();
        // 使用MySql做持久化的BranchMessageSaver，需要在pom中引入spring-ai-react-agent-saver-jdbc-template模块
        BranchMessageSaver messageSaver = new JdbcTemplateBranchMessageSaver(createJdbcTemplate(), messageSerializer);
        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("You are a helpful assistant.")
                .build();
    }

    /**
     * 预期结果：第一次运行，会回答不知道你的名字。以后运行，都会回答记得你叫小明
     */
    public static void main(String[] args) {
        ReActAgent reActAgent = createReActAgent();
        RunAgentOptions options = RunAgentOptions.builder()
                .newUserMessage("记得我叫什么名字吗？")
                .threadId("MySqlPersistenceDemo-thread")
                .build();
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);
        eventFlux.doOnNext(System.out::println).blockLast();
        options = RunAgentOptions.builder()
                .newUserMessage("我的名字是小明。")
                .threadId("MySqlPersistenceDemo-thread")
                .build();
        eventFlux = reActAgent.run(options);
        eventFlux.doOnNext(System.out::println).blockLast();
    }

}
