package com.x3bits.springaireactagent.core;

import com.x3bits.springaireactagent.core.event.AssistantTextPartEvent;
import com.x3bits.springaireactagent.core.event.LlmMessageEvent;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.memory.MemoryBranchMessageSaver;
import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultReActAgentTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private BranchMessageSaver branchMessageSaver;

    private ReActAgent reActAgent;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        reActAgent = ReActAgent.builder(chatClient)
                .messageSaver(branchMessageSaver)
                .systemPrompt("你是一个智能助手")
                .build();
    }

    @Test
    void testRunWithSimpleUserMessage() {
        // 准备Mock响应
        String expectedResponse = "你好！我是一个智能助手，很高兴为你服务！";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .build();

        // Mock ChatModel的call方法
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 准备测试参数
        String threadId = "test-thread-1";
        String userMessage = "你好！";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(false)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);

        // 验证事件序列
        StepVerifier.create(eventFlux)
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof UserMessage &&
                                llmEvent.message().getText().equals(userMessage);
                    }
                    return false;
                })
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof AssistantMessage &&
                                llmEvent.message().getText().equals(expectedResponse);
                    }
                    return false;
                })
                .verifyComplete();

        // 验证ChatModel被调用了
        verify(chatModel, times(1)).call(any(Prompt.class));

        // 验证消息被保存了
        verify(branchMessageSaver, times(2)).save(eq(threadId), any(BranchMessageItem.class));
    }

    @Test
    void testRunWithStreamingResponse() {
        // 准备Mock流式响应
        String expectedResponse = "你好！我是一个智能助手，很高兴为你服务！";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);

        // 模拟流式响应的各个部分
        List<ChatResponse> responseParts = List.of(
                ChatResponse.builder().generations(List.of(new Generation(new AssistantMessage("你好！")))).build(),
                ChatResponse.builder().generations(List.of(new Generation(new AssistantMessage("我是一个智能助手，")))).build(),
                ChatResponse.builder().generations(List.of(new Generation(new AssistantMessage("很高兴为你服务！")))).build());

        Flux<ChatResponse> responseFlux = Flux.fromIterable(responseParts);

        // Mock ChatModel的stream方法
        when(chatModel.stream(any(Prompt.class))).thenReturn(responseFlux);

        // 准备测试参数
        String threadId = "test-thread-2";
        String userMessage = "你好！";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(true)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);

        // 验证事件序列
        StepVerifier.create(eventFlux)
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof UserMessage &&
                                llmEvent.message().getText().equals(userMessage);
                    }
                    return false;
                })
                .expectNextMatches(event -> event instanceof AssistantTextPartEvent)
                .expectNextMatches(event -> event instanceof AssistantTextPartEvent)
                .expectNextMatches(event -> event instanceof AssistantTextPartEvent)
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof AssistantMessage;
                    }
                    return false;
                })
                .verifyComplete();

        // 验证ChatModel的stream方法被调用了
        verify(chatModel, times(1)).stream(any(Prompt.class));

        // 验证消息被保存了
        verify(branchMessageSaver, times(2)).save(eq(threadId), any(BranchMessageItem.class));
    }

    @Test
    void testRunWithSystemPrompt() {
        // 准备Mock响应
        String expectedResponse = "我理解了系统提示，我会按照要求行动。";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .build();

        // Mock ChatModel的call方法
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 准备测试参数
        String threadId = "test-thread-3";
        String userMessage = "请按照系统提示行动";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(false)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);

        // 收集所有事件
        List<ReActAgentEvent> events = eventFlux.collectList().block();

        // 验证事件数量
        assertEquals(2, events.size());

        // 验证第一个事件是用户消息
        assertTrue(events.get(0) instanceof LlmMessageEvent);
        LlmMessageEvent firstEvent = (LlmMessageEvent) events.get(0);
        assertTrue(firstEvent.message() instanceof UserMessage);
        assertEquals(userMessage, firstEvent.message().getText());

        // 验证第二个事件是助手消息
        assertTrue(events.get(1) instanceof LlmMessageEvent);
        LlmMessageEvent secondEvent = (LlmMessageEvent) events.get(1);
        assertTrue(secondEvent.message() instanceof AssistantMessage);
        assertEquals(expectedResponse, secondEvent.message().getText());

        // 验证ChatModel被调用时包含了系统提示
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(1)).call(promptCaptor.capture());

        Prompt capturedPrompt = promptCaptor.getValue();
        List<Message> messages = capturedPrompt.getInstructions();

        // 验证消息中包含系统消息
        boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> msg instanceof SystemMessage);
        assertTrue(hasSystemMessage);
    }

    @Test
    void testRunWithPreviousMessageHistory() {
        // 准备历史消息
        String threadId = "test-thread-4";
        UserMessage previousUserMessage = new UserMessage("我的名字是小明");
        AssistantMessage previousAssistantMessage = new AssistantMessage("你好小明，很高兴认识你！");

        BranchMessageItem userMessageItem = new BranchMessageItem(previousUserMessage, "msg1", null, Map.of());
        BranchMessageItem assistantMessageItem = new BranchMessageItem(previousAssistantMessage, "msg2", "msg1",
                Map.of());

        // Mock历史消息获取
        when(branchMessageSaver.getLatestMessageId(threadId)).thenReturn("msg2");
        when(branchMessageSaver.getLatestMessages(eq(threadId), anyInt(), isNull()))
                .thenReturn(List.of(previousUserMessage, previousAssistantMessage));

        // 准备Mock响应
        String expectedResponse = "是的，我记得你是小明。";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .build();

        // Mock ChatModel的call方法
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 准备测试参数
        String userMessage = "还记得我的名字吗？";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(false)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);

        // 收集所有事件
        List<ReActAgentEvent> events = eventFlux.collectList().block();

        // 验证事件数量
        assertEquals(2, events.size());

        // 验证ChatModel被调用时包含了历史消息
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(1)).call(promptCaptor.capture());

        Prompt capturedPrompt = promptCaptor.getValue();
        List<Message> messages = capturedPrompt.getInstructions();

        // 验证消息中包含历史消息
        assertTrue(messages.size() >= 3); // 至少包含：系统消息、历史消息、新用户消息

        // 验证历史消息获取被调用了
        verify(branchMessageSaver, times(1)).getLatestMessages(eq(threadId), anyInt(), isNull());
    }

    @Test
    void testGetBranchMessages() {
        // 准备测试数据
        String threadId = "test-thread-5";
        UserMessage userMessage = new UserMessage("测试消息");
        AssistantMessage assistantMessage = new AssistantMessage("测试回复");

        BranchMessageItem userMessageItem = new BranchMessageItem(userMessage, "msg1", null, Map.of());
        BranchMessageItem assistantMessageItem = new BranchMessageItem(assistantMessage, "msg2", "msg1", Map.of());

        List<BranchMessageItem> expectedMessages = List.of(userMessageItem, assistantMessageItem);

        // Mock BranchMessageSaver
        when(branchMessageSaver.getAllMessages(threadId)).thenReturn(expectedMessages);

        // 执行测试
        List<BranchMessageItem> actualMessages = reActAgent.getBranchMessages(threadId);

        // 验证结果
        assertEquals(expectedMessages.size(), actualMessages.size());
        assertEquals(expectedMessages.get(0).id(), actualMessages.get(0).id());
        assertEquals(expectedMessages.get(1).id(), actualMessages.get(1).id());

        // 验证方法被调用了
        verify(branchMessageSaver, times(1)).getAllMessages(threadId);
    }

    @Test
    void testRunWithoutMessageSaver() {
        // 创建没有messageSaver的ReActAgent
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        ReActAgent agentWithoutSaver = ReActAgent.builder(chatClient)
                .systemPrompt("你是一个智能助手")
                .build();

        // 准备Mock响应
        String expectedResponse = "你好！";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .build();

        // Mock ChatModel的call方法
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 准备测试参数
        String threadId = "test-thread-6";
        String userMessage = "你好！";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(false)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = agentWithoutSaver.run(options);

        // 验证事件序列
        StepVerifier.create(eventFlux)
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof UserMessage &&
                                llmEvent.message().getText().equals(userMessage);
                    }
                    return false;
                })
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof AssistantMessage &&
                                llmEvent.message().getText().equals(expectedResponse);
                    }
                    return false;
                })
                .verifyComplete();

        // 验证ChatModel被调用了
        verify(chatModel, times(1)).call(any(Prompt.class));

        // 验证branchMessageSaver没有被调用（因为没有设置）
        verifyNoInteractions(branchMessageSaver);
    }

    @Test
    void testRunWithMaxIterations() {
        // 准备Mock响应 - 简单的响应，不涉及工具调用
        String expectedResponse = "这是一个简单的响应";
        AssistantMessage assistantMessage = new AssistantMessage(expectedResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(generation))
                .build();

        // Mock ChatModel的call方法
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // 准备测试参数 - 设置最大迭代次数为1
        String threadId = "test-thread-7";
        String userMessage = "请回答问题";

        RunAgentOptions options = RunAgentOptions.builder()
                .threadId(threadId)
                .newUserMessage(userMessage)
                .enableStream(false)
                .maxIterations(1)
                .build();

        // 执行测试
        Flux<ReActAgentEvent> eventFlux = reActAgent.run(options);

        // 验证事件序列
        StepVerifier.create(eventFlux)
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof UserMessage &&
                                llmEvent.message().getText().equals(userMessage);
                    }
                    return false;
                })
                .expectNextMatches(event -> {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        return llmEvent.message() instanceof AssistantMessage &&
                                llmEvent.message().getText().equals(expectedResponse);
                    }
                    return false;
                })
                .verifyComplete();
    }

    @Test
    void testBuilderConfiguration() {
        // 准备组件
        BranchMessageSaver customMessageSaver = new MemoryBranchMessageSaver();
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String customSystemPrompt = "你是一个自定义的智能助手";

        // 使用Builder构建ReActAgent
        ReActAgent customAgent = ReActAgent.builder(chatClient)
                .messageSaver(customMessageSaver)
                .systemPrompt(customSystemPrompt)
                .build();

        // 验证构建成功
        assertNotNull(customAgent);
        assertTrue(customAgent instanceof DefaultReActAgent);

        // 验证可以获取消息
        List<BranchMessageItem> messages = customAgent.getBranchMessages("test-thread");
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }
}