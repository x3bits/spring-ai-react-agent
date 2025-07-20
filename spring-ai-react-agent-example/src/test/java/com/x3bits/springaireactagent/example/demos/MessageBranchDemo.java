package com.x3bits.springaireactagent.example.demos;

import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.core.RunAgentOptions;
import com.x3bits.springaireactagent.core.event.LlmMessageEvent;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.memory.MemoryBranchMessageSaver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 演示如何使用消息分支
 * 环境准备：
 * 把代码中的常量替换成你的配置
 */
public class MessageBranchDemo {

    // 环境变量常量
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_BASE_URL = System.getenv("OPENAI_BASE_URL");
    private static final String OPENAI_DEFAULT_MODEL = System.getenv("OPENAI_DEFAULT_MODEL");

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
                .build();
        //这里使用MemoryBranchMessageSaver，用于测试时临时保存对话历史。生产环境需要使用支持持久化的Saver，例如JdbcTemplateBranchMessageSaver
        BranchMessageSaver messageSaver = new MemoryBranchMessageSaver();
        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .build();
    }

    public static void main(String[] args) {
        ReActAgent reActAgent = createReActAgent();
        String threadId = "MessageBranchDemo-thread";

        // 第一轮对话
        System.out.println("开始第1轮对话");
        RunAgentOptions options = RunAgentOptions.builder()
                .newUserMessage("现在正在举行一场足球赛，红队和蓝队的比分现在是2:1，请记住。")
                .threadId(threadId)
                .build();
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);
        eventFlux = eventFlux.doOnNext(System.out::println);
        List<ReActAgentEvent> eventList = eventFlux
                .toStream().toList();
        String id1 = ((LlmMessageEvent)(eventList.getLast())).id();

        // 第2轮对话
        System.out.println("开始第2轮对话");
        options = RunAgentOptions.builder()
                .newUserMessage("蓝队得了1分，比分现在是2:2，请记住。")
                .threadId(threadId)
                .build();
        eventFlux = reActAgent.run(options);
        eventFlux = eventFlux.doOnNext(System.out::println);
        eventList = eventFlux
                .toStream().toList();
        String id2 = ((LlmMessageEvent)(eventList.getLast())).id();

        // 从第1轮对话末尾继续
        System.out.println("从第1轮末尾继续");
        options = RunAgentOptions.builder()
                // 这一行是关键，指定从上一次的哪个消息开始继续
                .previousMessageId(id1)
                .newUserMessage("现在比分是多少？")
                .threadId(threadId)
                .build();
        eventFlux = reActAgent.run(options);
        eventFlux.doOnNext(System.out::println).blockLast();
        //这里预计打印出比分是2:1的回答

        // 从第2轮对话末尾继续
        System.out.println("从第2轮末尾继续");
        options = RunAgentOptions.builder()
                // 如果不指定previousMessageId，会从最后一个event继续
                .previousMessageId(id2)
                .newUserMessage("现在比分是多少？")
                .threadId(threadId)
                .build();
        eventFlux = reActAgent.run(options);
        eventFlux.doOnNext(System.out::println).blockLast();
        //这里预计打印出比分是2:2的回答
    }

}
