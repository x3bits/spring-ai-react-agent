package com.x3bits.springaireactagent.saver.jdbc;

import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import com.x3bits.springaireactagent.serializer.MessageSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTemplateBranchMessageSaverTest {

    private JdbcTemplate jdbcTemplate;
    private MessageSerializer messageSerializer;
    private JdbcTemplateBranchMessageSaver saver;

    @BeforeEach
    void setUp() {
        // 创建内存数据库
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();

        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建简单的 MessageSerializer Mock
        messageSerializer = new MessageSerializer() {
            @Override
            public String serialize(Message message) {
                return message.getText();
            }

            @Override
            public Message deserialize(MessageType messageType, String str) {
                return switch (messageType) {
                    case USER -> new UserMessage(str);
                    case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(str);
                    case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(str);
                    case TOOL -> new org.springframework.ai.chat.messages.ToolResponseMessage(
                            List.of(new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse("id",
                                    "name", str)));
                };
            }
        };

        saver = new JdbcTemplateBranchMessageSaver(jdbcTemplate, messageSerializer);
    }

    @Test
    void testSaveAndGetLatestMessageId() {
        String threadId = "test-thread-" + UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();
        Message message = new UserMessage("Hello, world!");

        BranchMessageItem item = new BranchMessageItem(message, messageId, null, Map.of());

        // 保存消息
        saver.save(threadId, item);

        // 获取最新消息ID
        String latestId = saver.getLatestMessageId(threadId);
        assertEquals(messageId, latestId);
    }

    @Test
    void testGetAllMessages() {
        String threadId = "test-thread-" + UUID.randomUUID().toString();
        String messageId1 = UUID.randomUUID().toString();
        String messageId2 = UUID.randomUUID().toString();

        Message message1 = new UserMessage("First message");
        Message message2 = new UserMessage("Second message");

        BranchMessageItem item1 = new BranchMessageItem(message1, messageId1, null, Map.of());
        BranchMessageItem item2 = new BranchMessageItem(message2, messageId2, messageId1, Map.of());

        // 保存消息
        saver.save(threadId, item1);
        saver.save(threadId, item2);

        // 获取所有消息
        List<BranchMessageItem> messages = saver.getAllMessages(threadId);
        assertEquals(2, messages.size());
        assertEquals("First message", messages.get(0).message().getText());
        assertEquals("Second message", messages.get(1).message().getText());
    }

    @Test
    void testGetLatestMessages() {
        String threadId = "test-thread-" + UUID.randomUUID().toString();
        String messageId1 = UUID.randomUUID().toString();
        String messageId2 = UUID.randomUUID().toString();
        String messageId3 = UUID.randomUUID().toString();

        Message message1 = new UserMessage("First message");
        Message message2 = new UserMessage("Second message");
        Message message3 = new UserMessage("Third message");

        BranchMessageItem item1 = new BranchMessageItem(message1, messageId1, null, Map.of());
        BranchMessageItem item2 = new BranchMessageItem(message2, messageId2, messageId1, Map.of());
        BranchMessageItem item3 = new BranchMessageItem(message3, messageId3, messageId2, Map.of());

        // 保存消息
        saver.save(threadId, item1);
        saver.save(threadId, item2);
        saver.save(threadId, item3);

        // 获取最新的2条消息
        List<Message> latestMessages = saver.getLatestMessages(threadId, 2, null);
        assertEquals(2, latestMessages.size());
        assertEquals("Second message", latestMessages.get(0).getText());
        assertEquals("Third message", latestMessages.get(1).getText());
    }

    @Test
    void testEmptyResults() {
        String threadId = "non-existent-thread-" + UUID.randomUUID().toString();

        // 测试空结果
        assertNull(saver.getLatestMessageId(threadId));
        assertTrue(saver.getAllMessages(threadId).isEmpty());
        assertTrue(saver.getLatestMessages(threadId, 10, null).isEmpty());
    }

    @Test
    void testNullParameters() {
        // 测试空参数
        assertThrows(IllegalArgumentException.class,
                () -> saver.save(null, new BranchMessageItem(new UserMessage("test"), "id", null, Map.of())));

        assertThrows(IllegalArgumentException.class, () -> saver.save("thread", null));

        assertNull(saver.getLatestMessageId(null));
        assertTrue(saver.getAllMessages(null).isEmpty());
        assertTrue(saver.getLatestMessages(null, 10, null).isEmpty());
    }
}