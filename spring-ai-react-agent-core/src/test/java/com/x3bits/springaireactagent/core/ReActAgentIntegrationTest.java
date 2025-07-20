package com.x3bits.springaireactagent.core;

import com.x3bits.springaireactagent.core.event.AssistantTextPartEvent;
import com.x3bits.springaireactagent.core.event.LlmMessageEvent;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.memory.MemoryBranchMessageSaver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag("integration") // 标记为集成测试，默认不执行
class ReActAgentIntegrationTest {

    static class Calculator {

        @Tool(description = "计算两数之和")
        public int add(int a, int b) {
            return a + b;
        }

    }

    @Test
    void simpleCallReactAgentTest() {
        // 初始化组件
        ReActAgent reActAgent = createReActAgent();
        final String threadId = "test-thread";

        // 测试第一个对话：简单问候
        List<ReActAgentEvent> events = runAndCollectEvents(threadId, reActAgent, "你好！我叫小明。", false);
        verifyBasicEventStructure(events, 2);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class);

        // 测试第二个对话：工具调用
        events = runAndCollectEvents(threadId, reActAgent, "123 + 456等于多少？", false);
        verifyBasicEventStructure(events, 4);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class, ToolResponseMessage.class,
                AssistantMessage.class);

        // 验证工具调用相关的断言
        AssistantMessage firstAssistant = getAssistantMessage(events, 1);
        AssistantMessage secondAssistant = getAssistantMessage(events, 3);
        Assertions.assertTrue(firstAssistant.hasToolCalls());
        Assertions.assertFalse(secondAssistant.hasToolCalls());

        String calculationResult = secondAssistant.getText();
        Assertions.assertNotNull(calculationResult);
        Assertions.assertTrue(calculationResult.contains(String.valueOf(123 + 456)));

        // 测试第三个对话：记忆功能
        events = runAndCollectEvents(threadId, reActAgent, "还记得我叫什么名字吗？", false);
        verifyBasicEventStructure(events, 2);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class);

        String memoryResponse = getAssistantMessageText(events, 1);
        Assertions.assertNotNull(memoryResponse);
        Assertions.assertTrue(memoryResponse.contains("小明"));
    }

    @Test
    void streamTest() {
        // 初始化组件
        ReActAgent reActAgent = createReActAgent();
        final String threadId = "test-thread";

        // 测试第一个对话：简单问候
        List<ReActAgentEvent> events = runAndCollectEvents(threadId, reActAgent, "你好！我叫小明。", true);
        events = removePartEvents(events);
        verifyBasicEventStructure(events, 2);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class);

        // 测试第二个对话：工具调用
        events = runAndCollectEvents(threadId, reActAgent, "123 + 456等于多少？", true);
        events = removePartEvents(events);
        verifyBasicEventStructure(events, 4);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class, ToolResponseMessage.class,
                AssistantMessage.class);

        // 验证工具调用相关的断言
        AssistantMessage firstAssistant = getAssistantMessage(events, 1);
        AssistantMessage secondAssistant = getAssistantMessage(events, 3);
        Assertions.assertTrue(firstAssistant.hasToolCalls());
        Assertions.assertFalse(secondAssistant.hasToolCalls());

        String calculationResult = secondAssistant.getText();
        Assertions.assertNotNull(calculationResult);
        Assertions.assertTrue(calculationResult.contains(String.valueOf(123 + 456)));

        // 测试第三个对话：记忆功能
        events = runAndCollectEvents(threadId, reActAgent, "还记得我叫什么名字吗？", true);
        events = removePartEvents(events);
        verifyBasicEventStructure(events, 2);
        verifyEventTypes(events, UserMessage.class, AssistantMessage.class);

        String memoryResponse = getAssistantMessageText(events, 1);
        Assertions.assertNotNull(memoryResponse);
        Assertions.assertTrue(memoryResponse.contains("小明"));
    }

    @Test
    void streamTest2() {
        ReActAgent reActAgent = createReActAgent();
        final String threadId = "test-thread";
        Flux<ReActAgentEvent> eventFlux = runUserMessage(threadId, reActAgent, "请解释什么是加法交换律,结合律，交换律", true);
        eventFlux.doOnNext(System.out::println).blockLast();
    }

    private static List<ReActAgentEvent> removePartEvents(List<ReActAgentEvent> events) {
        return events.stream().filter(event -> !(event instanceof AssistantTextPartEvent)).toList();
    }

    // 创建ReActAgent实例
    private ReActAgent createReActAgent() {
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(
                        OpenAiApi.builder()
                                .apiKey(System.getenv("OPENAI_API_KEY"))
                                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                                .build())
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("qwen-plus")
                                .build())
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(new Calculator())
                .build();
        BranchMessageSaver messageSaver = new MemoryBranchMessageSaver();
        return ReActAgent.builder(chatClient)
                .messageSaver(messageSaver)
                .systemPrompt("你是一个整数加法计算器，你使用工具add计算两数之和。")
                .build();
    }

    // 运行用户消息并收集事件
    private List<ReActAgentEvent> runAndCollectEvents(String threadId, ReActAgent reActAgent, String userMessage, boolean stream) {
        Flux<ReActAgentEvent> eventFlux = runUserMessage(threadId, reActAgent, userMessage, stream);
        return eventFlux.collectList().block();
    }

    // 验证事件的基本结构
    private void verifyBasicEventStructure(List<ReActAgentEvent> events, int expectedSize) {
        Assertions.assertNotNull(events);
        Assertions.assertEquals(expectedSize, events.size());
    }

    // 验证事件类型
    @SafeVarargs
    private void verifyEventTypes(List<ReActAgentEvent> events, Class<? extends Message>... expectedTypes) {
        for (int i = 0; i < expectedTypes.length; i++) {
            Message message = ((LlmMessageEvent) events.get(i)).message();
            Assertions.assertInstanceOf(expectedTypes[i], message);
        }
    }

    // 获取指定位置的AssistantMessage
    private AssistantMessage getAssistantMessage(List<ReActAgentEvent> events, int index) {
        return (AssistantMessage) ((LlmMessageEvent) events.get(index)).message();
    }

    // 获取指定位置的AssistantMessage文本
    private String getAssistantMessageText(List<ReActAgentEvent> events, int index) {
        return getAssistantMessage(events, index).getText();
    }

    private static Flux<ReActAgentEvent> runUserMessage(String threadId, ReActAgent reActAgent, String userMessage, boolean stream) {
        RunAgentOptions runAgentOptions = RunAgentOptions.builder()
                .newUserMessage(userMessage)
                .threadId(threadId)
                .enableStream(stream)
                .build();
        return reActAgent.run(runAgentOptions);
    }

}