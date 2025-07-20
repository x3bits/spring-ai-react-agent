package com.x3bits.springaireactagent.example.demos;

import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.core.RunAgentOptions;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.memory.MemoryBranchMessageSaver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 演示如何与Spring AI的API结合，实现两个功能：
 * 1. 在工具中获取上下文信息
 * 2. 从工具中向外部发送Event
 */
public class ToolContextDemo {

    // 环境变量常量
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_BASE_URL = System.getenv("OPENAI_BASE_URL");
    private static final String OPENAI_DEFAULT_MODEL = System.getenv("OPENAI_DEFAULT_MODEL");

    // 定义Tool
    private static class CurrentUserInfoTool {
        @Tool(description = "查询当前用户名称")
        public String currentUserName(ToolContext toolContext) {
            @SuppressWarnings("unchecked") FluxSink<ReActAgentEvent> sink = (FluxSink<ReActAgentEvent>) (toolContext.getContext().get("eventSink"));
            sink.next(new CustomEvent("demo event currentUserName"));
            return toolContext.getContext().get("userName").toString();
        }
    }

    private record CustomEvent(String content) implements ReActAgentEvent {
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
                .defaultTools(new CurrentUserInfoTool())
                .build();
        //这里使用MemoryBranchMessageSaver，用于测试时临时保存对话历史。生产环境需要使用支持持久化的Saver，例如JdbcTemplateBranchMessageSaver
        BranchMessageSaver messageSaver = new MemoryBranchMessageSaver();
        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("你是一个助理。当需要用户名时，使用工具currentUserName查询。")
                .build();
    }

    public static void main(String[] args) {
        ReActAgent reActAgent = createReActAgent();

        Flux<ReActAgentEvent>  mergedEvents = Flux.create(sink -> {
            ChatOptions chatOptions = ToolCallingChatOptions.builder()
                    .toolContext("userName", "小明")
                    .toolContext("eventSink", sink)
                    .build();
            RunAgentOptions options = RunAgentOptions.builder()
                    .chatOptions(chatOptions)
                    .newUserMessage("知道我叫什么名字吗？")
                    .threadId("test-thread")
                    .enableStream(true)
                    .build();
            reActAgent.run(options).doOnNext(sink::next).blockLast();
            sink.complete();
        });
        mergedEvents.doOnNext(System.out::println).blockLast();
    }

}
