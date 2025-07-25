package com.x3bits.springaireactagent.example.config;

import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.example.tool.Calculator;
import com.x3bits.springaireactagent.saver.jdbc.JdbcTemplateBranchMessageSaver;
import com.x3bits.springaireactagent.serializer.json.JsonMessageSerializer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ReActAgentConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model) {

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("spring.ai.openai.api-key configuration is required");
        }

        return OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .apiKey(apiKey)
                                .baseUrl(baseUrl)
                                .build())
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(model)
                                .build())
                .build();
    }

    @Bean
    public JdbcTemplateBranchMessageSaver jdbcTemplateBranchMessageSaver(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateBranchMessageSaver(jdbcTemplate, new JsonMessageSerializer());
    }

    @Bean
    public ReActAgent reActAgent(ChatModel chatModel, JdbcTemplateBranchMessageSaver messageSaver, Calculator calculator) {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(calculator)
                .build();

        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("你是一个整数加法计算器，你使用工具add计算两数之和。")
                .build();
    }


    @Bean
    public ReActAgent generalAgent(ChatModel chatModel, JdbcTemplateBranchMessageSaver messageSaver) {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("你是一个通用的AI助手，可以回答各种问题并提供帮助。")
                .build();
    }
} 