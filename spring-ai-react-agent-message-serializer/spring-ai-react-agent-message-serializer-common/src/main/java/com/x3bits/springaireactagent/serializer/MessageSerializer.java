package com.x3bits.springaireactagent.serializer;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

public interface MessageSerializer {
    String serialize(Message message);

    Message deserialize(MessageType messageType, String str);
}
