package com.x3bits.springaireactagent.example.demos;

import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.core.RunAgentOptions;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.memory.MemoryBranchMessageSaver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import reactor.core.publisher.Flux;

/**
 * 演示一个最简单的，使用工具的Agent
 * 环境准备：
 * 把代码中的常量替换成你的配置
 */
public class SimpleReActAgentDemo {

    // 环境变量常量
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_BASE_URL = System.getenv("OPENAI_BASE_URL");
    private static final String OPENAI_DEFAULT_MODEL = System.getenv("OPENAI_DEFAULT_MODEL");

    // 定义Tool
    private static class Calculator {
        @Tool(description = "计算两数之和")
        public int add(@ToolParam(description = "第1个加数") int a, @ToolParam(description = "第2个加数") int b) {
            return a + b;
        }
    }

    // 创建ReactAgent
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
        //Spring AI的ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(new Calculator())
                .build();
        //这里使用MemoryBranchMessageSaver，用于测试时临时保存对话历史。生产环境需要使用支持持久化的Saver，例如JdbcTemplateBranchMessageSaver
        BranchMessageSaver messageSaver = new MemoryBranchMessageSaver();
        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("你是一个整数加法计算器，你使用工具add计算两数之和。")
                .build();
    }

    public static void main(String[] args) {
        ReActAgent reActAgent = createReActAgent();
        RunAgentOptions options = RunAgentOptions.builder()
                .newUserMessage("123 + 456等于多少？")
                .threadId("test-thread")
                .enableStream(true)
                .build();
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);
        eventFlux.doOnNext(System.out::println).blockLast();
    }

}
