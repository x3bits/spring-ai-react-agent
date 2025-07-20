package com.x3bits.springaireactagent.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.x3bits.springaireactagent.core.ReActAgent;
import com.x3bits.springaireactagent.core.RunAgentOptions;
import com.x3bits.springaireactagent.core.event.AssistantTextPartEvent;
import com.x3bits.springaireactagent.core.event.LlmMessageEvent;
import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import com.x3bits.springaireactagent.web.dto.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/springAiReactAgent/api")
public class ChatController {

    private final ApplicationContext applicationContext;

    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ChatController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取指定名称的ReActAgent Bean
     *
     * @param agentBeanName Bean名称，如果为null则获取默认的ReActAgent
     * @return ReActAgent实例
     * @throws IllegalArgumentException 如果指定的Bean不存在或不是ReActAgent类型
     */
    private ReActAgent getReActAgent(String agentBeanName) {
        if (agentBeanName == null || agentBeanName.trim().isEmpty()) {
            // 如果没有指定Bean名称，尝试获取默认的ReActAgent
            Map<String, ReActAgent> agents = applicationContext.getBeansOfType(ReActAgent.class);
            if (agents.isEmpty()) {
                throw new IllegalStateException("No ReActAgent beans found in the application context");
            }
            if (agents.size() == 1) {
                return agents.values().iterator().next();
            }
            // 如果有多个Bean，尝试获取名为"reActAgent"的Bean
            ReActAgent defaultAgent = agents.get("reActAgent");
            if (defaultAgent != null) {
                return defaultAgent;
            }
            throw new IllegalArgumentException("Multiple ReActAgent beans found, please specify agentBeanName. Available beans: " + agents.keySet());
        }

        try {
            Object bean = applicationContext.getBean(agentBeanName);
            if (!(bean instanceof ReActAgent)) {
                throw new IllegalArgumentException("Bean '" + agentBeanName + "' is not a ReActAgent instance");
            }
            return (ReActAgent) bean;
        } catch (Exception e) {
            throw new IllegalArgumentException("ReActAgent bean '" + agentBeanName + "' not found", e);
        }
    }

    /**
     * 获取所有可用的ReActAgent Bean名称
     * GET /agents/list
     */
    @GetMapping("/agents/list")
    public Map<String, Object> listAvailableAgents() {
        Map<String, ReActAgent> agents = applicationContext.getBeansOfType(ReActAgent.class);
        Set<String> agentNames = agents.keySet();

        Map<String, Object> response = new HashMap<>();
        response.put("agents", agentNames);

        return response;
    }

    @PostMapping("/chat/stream")
    public Flux<ServerSentEvent<SseResponse>> streamChat(@RequestBody ChatRequest request) {
        // 获取指定的ReActAgent
        ReActAgent reActAgent = getReActAgent(request.agentBeanName());

        // 构建运行选项
        RunAgentOptions.Builder optionsBuilder = RunAgentOptions.builder()
                .threadId(request.threadId() != null ? request.threadId() : UUID.randomUUID().toString())
                .enableStream(true)
                .maxIterations(25)
                .previousMessageId(request.checkpointId());
        if (request.userMessage() != null) {
            optionsBuilder = optionsBuilder.newUserMessage(request.userMessage());
        }
        RunAgentOptions options = optionsBuilder.build();

        // 调用ReActAgent并转换事件
        return convertEventToSse(reActAgent.run(options));
    }

    @GetMapping("/thread/items/{threadId}")
    public List<ThreadItem> listThreadItems(@PathVariable("threadId") String threadId,
                                           @RequestParam(value = "agentBeanName", required = false) String agentBeanName) {
        // 获取指定的ReActAgent
        ReActAgent reActAgent = getReActAgent(agentBeanName);

        List<BranchMessageItem> branchMessages = reActAgent.getBranchMessages(threadId);
        return branchMessages.stream().map(
                item -> new ThreadItem(item.id(), item.previousId(), parseMessageType(item),
                        parseContent(item)))
                .toList();
    }

    private List<Map<String, Object>> parseContent(BranchMessageItem item) {
        List<Map<String, Object>> content = new ArrayList<>();
        org.springframework.ai.chat.messages.Message message = item.message();

        if (message instanceof UserMessage userMessage) {
            // 用户消息
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("type", "userEvent");
            userContent.put("content", userMessage.getText());
            content.add(userContent);

        } else if (message instanceof AssistantMessage assistantMessage) {
            // 助手消息 - 处理工具调用
            if (assistantMessage.hasToolCalls()) {
                for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                    Map<String, Object> toolCallContent = new HashMap<>();
                    toolCallContent.put("type", "assistantContent");

                    Map<String, Object> toolCallData = new HashMap<>();
                    toolCallData.put("type", "toolCall");
                    toolCallData.put("id", toolCall.id());
                    toolCallData.put("name", toolCall.name());

                    // 解析工具调用参数
                    String arguments = toolCall.arguments();
                    Object args = new HashMap<>();
                    try {
                        if (!arguments.isEmpty()) {
                            // 使用 Jackson ObjectMapper 解析 JSON
                            args = objectMapper.readValue(arguments, new TypeReference<>() {});
                        }
                    } catch (Exception e) {
                        // 解析失败时使用原始字符串
                        args = arguments;
                    }

                    toolCallData.put("args", args);
                    toolCallContent.put("data", toolCallData);
                    content.add(toolCallContent);
                }
            }

            // 助手消息 - 处理文本内容
            String text = assistantMessage.getText();
            if (text != null && !text.trim().isEmpty()) {
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "assistantContent");

                Map<String, Object> textData = new HashMap<>();
                textData.put("type", "text");
                textData.put("content", text);

                textContent.put("data", textData);
                content.add(textContent);
            }

        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            // 工具响应消息
            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                Map<String, Object> toolResultContent = new HashMap<>();
                toolResultContent.put("id", item.id());
                toolResultContent.put("type", "toolResult");
                toolResultContent.put("callId", toolResponse.id());
                toolResultContent.put("content", toolResponse.responseData());
                content.add(toolResultContent);
            }
        }

        return content;
    }

    private static String parseMessageType(BranchMessageItem item) {
        return item.message() != null && MessageType.USER.equals(item.message().getMessageType()) ? "user"
                : "assistant";
    }

    public record ThreadItem(
            String checkpointId,
            String previousCheckpointId,
            String type,
            List<Map<String, Object>> content) {
    }

    private Flux<ServerSentEvent<SseResponse>> convertEventToSse(Flux<ReActAgentEvent> events) {
        return Flux.create(sink -> executor.execute(() -> {
            events.doOnNext(event -> {
                try {
                    if (event instanceof LlmMessageEvent llmEvent) {
                        handleLlmMessageEvent(llmEvent, sink);
                    } else if (event instanceof AssistantTextPartEvent textPartEvent) {
                        handleAssistantTextPartEvent(textPartEvent, sink);
                    }
                } catch (Exception e) {
                    sink.error(e);
                }
            }).blockLast();
            sink.complete();
        }));
    }

    private void handleLlmMessageEvent(LlmMessageEvent llmEvent,
            reactor.core.publisher.FluxSink<ServerSentEvent<SseResponse>> sink) {

        org.springframework.ai.chat.messages.Message message = llmEvent.message();
        String id = llmEvent.id();

        if (message instanceof UserMessage) {
            // 用户消息只返回ID
            SseResponse response = SseResponse.userEventId(id);
            ServerSentEvent<SseResponse> event = ServerSentEvent.<SseResponse>builder()
                    .data(response)
                    .build();
            sink.next(event);

        } else if (message instanceof AssistantMessage assistantMessage) {
            // 助手开始事件
            SseResponse startResponse = SseResponse.assistantStart(id);
            ServerSentEvent<SseResponse> startEvent = ServerSentEvent.<SseResponse>builder()
                    .data(startResponse)
                    .build();
            sink.next(startEvent);

            // 处理工具调用
            if (assistantMessage.hasToolCalls()) {
                for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                    Object args;
                    try {
                        args = objectMapper.readValue(toolCall.arguments(), new TypeReference<>() {
                        });
                    } catch (JsonProcessingException e) {
                        args = toolCall.arguments();
                    }
                    ToolCallData toolCallData = ToolCallData.of(
                            toolCall.id(),
                            toolCall.name(),
                            args);
                    SseResponse toolCallResponse = SseResponse.assistantContent(toolCallData);
                    ServerSentEvent<SseResponse> toolCallEvent = ServerSentEvent.<SseResponse>builder()
                            .data(toolCallResponse)
                            .build();
                    sink.next(toolCallEvent);
                }
            }

            // 处理文本内容
            String text = assistantMessage.getText();
            if (text != null && !text.trim().isEmpty()) {
                TextContentData textContentData = TextContentData.of(text);
                SseResponse textResponse = SseResponse.assistantContent(textContentData);
                ServerSentEvent<SseResponse> textEvent = ServerSentEvent.<SseResponse>builder()
                        .data(textResponse)
                        .build();
                sink.next(textEvent);
            }

        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            // 工具结果 - 为每个工具响应创建单独的事件
            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                SseResponse toolResultResponse = SseResponse.toolResult(
                        id,
                        toolResponse.id(),
                        toolResponse.responseData());
                ServerSentEvent<SseResponse> toolResultEvent = ServerSentEvent.<SseResponse>builder()
                        .data(toolResultResponse)
                        .build();
                sink.next(toolResultEvent);
            }
        }
    }

    private void handleAssistantTextPartEvent(AssistantTextPartEvent textPartEvent,
            reactor.core.publisher.FluxSink<ServerSentEvent<SseResponse>> sink) {

        SseResponse partialTextResponse = SseResponse.assistantPartialText(textPartEvent.text());
        ServerSentEvent<SseResponse> partialTextEvent = ServerSentEvent.<SseResponse>builder()
                .data(partialTextResponse)
                .build();
        sink.next(partialTextEvent);
    }
}