package com.x3bits.springaireactagent.core;

import com.x3bits.springaireactagent.core.event.ReActAgentEvent;
import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import com.x3bits.springaireactagent.core.prompt.SystemPromptProvider;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public interface ReActAgent {
    Flux<ReActAgentEvent> run(RunAgentOptions options);

    List<BranchMessageItem> getBranchMessages(String threadId);

    static Builder builder(ChatClient chatClient) {
        return new DefaultReActAgent.Builder(chatClient);
    }

    interface Builder {
        DefaultReActAgent.Builder messageSaver(BranchMessageSaver branchMessageSaver);

        DefaultReActAgent.Builder systemPromptProvider(SystemPromptProvider systemPromptProvider);

        DefaultReActAgent.Builder systemPrompt(String systemPrompt);

        ReActAgent build();
    }
}
