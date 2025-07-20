package com.x3bits.springaireactagent.serializer.json.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ToolResponseMessage的序列化Meta类
 */
public class ToolResponseMessageMeta {

    @JsonProperty("responses")
    private final List<ToolResponseMeta> responses;

    public ToolResponseMessageMeta(@JsonProperty("responses") List<ToolResponseMeta> responses) {
        this.responses = responses;
    }

    public static ToolResponseMessageMeta fromToolResponseMessage(ToolResponseMessage toolResponseMessage) {
        List<ToolResponseMeta> responseMetas = toolResponseMessage.getResponses().stream()
                .map(ToolResponseMeta::fromToolResponse)
                .collect(Collectors.toList());
        return new ToolResponseMessageMeta(responseMetas);
    }

    public ToolResponseMessage toToolResponseMessage() {
        List<ToolResponseMessage.ToolResponse> toolResponses = responses.stream()
                .map(ToolResponseMeta::toToolResponse)
                .collect(Collectors.toList());
        return new ToolResponseMessage(toolResponses);
    }

    public List<ToolResponseMeta> getResponses() {
        return responses;
    }

    // 内部静态类，对应ToolResponseMessage.ToolResponse
    public static class ToolResponseMeta {
        @JsonProperty("id")
        private final String id;
        @JsonProperty("name")
        private final String name;
        @JsonProperty("responseData")
        private final String responseData;

        public ToolResponseMeta(@JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("responseData") String responseData) {
            this.id = id;
            this.name = name;
            this.responseData = responseData;
        }

        public static ToolResponseMeta fromToolResponse(ToolResponseMessage.ToolResponse toolResponse) {
            return new ToolResponseMeta(toolResponse.id(), toolResponse.name(), toolResponse.responseData());
        }

        public ToolResponseMessage.ToolResponse toToolResponse() {
            return new ToolResponseMessage.ToolResponse(id, name, responseData);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getResponseData() {
            return responseData;
        }
    }
}