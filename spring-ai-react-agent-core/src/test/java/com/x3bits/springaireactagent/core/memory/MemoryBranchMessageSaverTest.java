package com.x3bits.springaireactagent.core.memory;

import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryBranchMessageSaverTest {

    private MemoryBranchMessageSaver messageSaver;

    @BeforeEach
    void setUp() {
        messageSaver = new MemoryBranchMessageSaver();
    }

    @Test
    void testSaveAndGetLatestMessageId() {
        // 测试保存消息和获取最新消息ID
        String threadId = "thread1";
        Message message = new UserMessage("Hello");
        BranchMessageItem branchMessageItem = new BranchMessageItem(message, "msg1", null, Map.of());

        // 初始状态应该没有消息
        assertNull(messageSaver.getLatestMessageId(threadId));

        // 保存消息
        messageSaver.save(threadId, branchMessageItem);

        // 验证最新消息ID
        assertEquals("msg1", messageSaver.getLatestMessageId(threadId));
    }

    @Test
    void testSaveWithNullArguments() {
        // 测试使用null参数保存消息
        String threadId = "thread1";
        Message message = new UserMessage("Hello");
        BranchMessageItem branchMessageItem = new BranchMessageItem(message, "msg1", null, Map.of());

        // 测试null threadId
        assertThrows(IllegalArgumentException.class, () -> {
            messageSaver.save(null, branchMessageItem);
        });

        // 测试null branchMessageItem
        assertThrows(IllegalArgumentException.class, () -> {
            messageSaver.save(threadId, null);
        });
    }

    @Test
    void testGetLatestMessageIdWithNullThreadId() {
        // 测试使用null线程ID获取最新消息ID
        assertNull(messageSaver.getLatestMessageId(null));
    }

    @Test
    void testSaveMessageChain() {
        // 测试保存消息链
        String threadId = "thread1";

        Message msg1 = new UserMessage("Hello");
        Message msg2 = new AssistantMessage("Hi there");
        Message msg3 = new UserMessage("How are you?");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", "msg1", Map.of());
        BranchMessageItem branchMessageItem3 = new BranchMessageItem(msg3, "msg3", "msg2", Map.of());

        // 保存消息链
        messageSaver.save(threadId, branchMessageItem1);
        messageSaver.save(threadId, branchMessageItem2);
        messageSaver.save(threadId, branchMessageItem3);

        // 验证最新消息ID
        assertEquals("msg3", messageSaver.getLatestMessageId(threadId));

        // 验证消息数量
        assertEquals(3, messageSaver.getMessageCount(threadId));
    }

    @Test
    void testGetAllMessages() {
        // 测试获取所有消息
        String threadId = "thread1";

        Message msg1 = new SystemMessage("You are a helpful assistant");
        Message msg2 = new UserMessage("Hello");
        Message msg3 = new AssistantMessage("Hi there");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", "msg1", Map.of());
        BranchMessageItem branchMessageItem3 = new BranchMessageItem(msg3, "msg3", "msg2", Map.of());

        // 保存消息
        messageSaver.save(threadId, branchMessageItem1);
        messageSaver.save(threadId, branchMessageItem2);
        messageSaver.save(threadId, branchMessageItem3);

        // 获取所有消息
        List<BranchMessageItem> allMessages = messageSaver.getAllMessages(threadId);

        assertEquals(3, allMessages.size());

        // 验证第一个消息
        BranchMessageItem item1 = allMessages.get(0);
        assertEquals("msg1", item1.id());
        assertTrue(item1.message() instanceof SystemMessage);
        assertNull(item1.previousId());

        // 验证第二个消息
        BranchMessageItem item2 = allMessages.get(1);
        assertEquals("msg2", item2.id());
        assertTrue(item2.message() instanceof UserMessage);
        assertEquals("msg1", item2.previousId());

        // 验证第三个消息
        BranchMessageItem item3 = allMessages.get(2);
        assertEquals("msg3", item3.id());
        assertTrue(item3.message() instanceof AssistantMessage);
        assertEquals("msg2", item3.previousId());
    }

    @Test
    void testGetAllMessagesWithNullThreadId() {
        // 测试使用null线程ID获取所有消息
        List<BranchMessageItem> allMessages = messageSaver.getAllMessages(null);
        assertTrue(allMessages.isEmpty());
    }

    @Test
    void testGetAllMessagesWithNonExistentThreadId() {
        // 测试获取不存在的线程的所有消息
        List<BranchMessageItem> allMessages = messageSaver.getAllMessages("nonexistent");
        assertTrue(allMessages.isEmpty());
    }

    @Test
    void testGetLatestMessages() {
        // 测试获取最新消息
        String threadId = "thread1";

        Message msg1 = new SystemMessage("System prompt");
        Message msg2 = new UserMessage("Hello");
        Message msg3 = new AssistantMessage("Hi");
        Message msg4 = new UserMessage("How are you?");
        Message msg5 = new AssistantMessage("I'm good");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", "msg1", Map.of());
        BranchMessageItem branchMessageItem3 = new BranchMessageItem(msg3, "msg3", "msg2", Map.of());
        BranchMessageItem branchMessageItem4 = new BranchMessageItem(msg4, "msg4", "msg3", Map.of());
        BranchMessageItem branchMessageItem5 = new BranchMessageItem(msg5, "msg5", "msg4", Map.of());

        // 保存消息链
        messageSaver.save(threadId, branchMessageItem1);
        messageSaver.save(threadId, branchMessageItem2);
        messageSaver.save(threadId, branchMessageItem3);
        messageSaver.save(threadId, branchMessageItem4);
        messageSaver.save(threadId, branchMessageItem5);

        // 获取最新3条消息
        List<Message> latestMessages = messageSaver.getLatestMessages(threadId, 3, null);

        assertEquals(3, latestMessages.size());

        // 验证消息顺序（应该是按时间顺序，最早的在前面）
        assertTrue(latestMessages.get(0) instanceof AssistantMessage);
        assertTrue(latestMessages.get(1) instanceof UserMessage);
        assertTrue(latestMessages.get(2) instanceof AssistantMessage);
    }

    @Test
    void testGetLatestMessagesWithSpecificStartPoint() {
        // 测试从特定消息开始获取最新消息
        String threadId = "thread1";

        Message msg1 = new UserMessage("Message 1");
        Message msg2 = new UserMessage("Message 2");
        Message msg3 = new UserMessage("Message 3");
        Message msg4 = new UserMessage("Message 4");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", "msg1", Map.of());
        BranchMessageItem branchMessageItem3 = new BranchMessageItem(msg3, "msg3", "msg2", Map.of());
        BranchMessageItem branchMessageItem4 = new BranchMessageItem(msg4, "msg4", "msg3", Map.of());

        // 保存消息链
        messageSaver.save(threadId, branchMessageItem1);
        messageSaver.save(threadId, branchMessageItem2);
        messageSaver.save(threadId, branchMessageItem3);
        messageSaver.save(threadId, branchMessageItem4);

        // 从msg3开始获取2条消息
        List<Message> latestMessages = messageSaver.getLatestMessages(threadId, 2, "msg3");

        assertEquals(2, latestMessages.size());
        assertTrue(latestMessages.get(0) instanceof UserMessage);
        assertTrue(latestMessages.get(1) instanceof UserMessage);
    }

    @Test
    void testGetLatestMessagesWithInvalidParameters() {
        // 测试使用无效参数获取最新消息
        String threadId = "thread1";

        // null threadId
        List<Message> messages1 = messageSaver.getLatestMessages(null, 5, null);
        assertTrue(messages1.isEmpty());

        // count <= 0
        List<Message> messages2 = messageSaver.getLatestMessages(threadId, 0, null);
        assertTrue(messages2.isEmpty());

        List<Message> messages3 = messageSaver.getLatestMessages(threadId, -1, null);
        assertTrue(messages3.isEmpty());

        // 不存在的线程ID
        List<Message> messages4 = messageSaver.getLatestMessages("nonexistent", 5, null);
        assertTrue(messages4.isEmpty());
    }

    @Test
    void testMultipleThreads() {
        // 测试多个线程的消息管理
        String threadId1 = "thread1";
        String threadId2 = "thread2";

        Message msg1 = new UserMessage("Thread 1 message");
        Message msg2 = new UserMessage("Thread 2 message");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", null, Map.of());

        // 保存到不同线程
        messageSaver.save(threadId1, branchMessageItem1);
        messageSaver.save(threadId2, branchMessageItem2);

        // 验证各线程的最新消息ID
        assertEquals("msg1", messageSaver.getLatestMessageId(threadId1));
        assertEquals("msg2", messageSaver.getLatestMessageId(threadId2));

        // 验证各线程的消息数量
        assertEquals(1, messageSaver.getMessageCount(threadId1));
        assertEquals(1, messageSaver.getMessageCount(threadId2));
    }

    @Test
    void testClearThread() {
        // 测试清空线程
        String threadId = "thread1";

        Message msg1 = new UserMessage("Message 1");
        Message msg2 = new UserMessage("Message 2");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", "msg1", Map.of());

        // 保存消息
        messageSaver.save(threadId, branchMessageItem1);
        messageSaver.save(threadId, branchMessageItem2);

        // 验证消息已保存
        assertEquals(2, messageSaver.getMessageCount(threadId));
        assertEquals("msg2", messageSaver.getLatestMessageId(threadId));

        // 清空线程
        messageSaver.clearThread(threadId);

        // 验证消息已清空
        assertEquals(0, messageSaver.getMessageCount(threadId));
        assertNull(messageSaver.getLatestMessageId(threadId));
        assertTrue(messageSaver.getAllMessages(threadId).isEmpty());
    }

    @Test
    void testClearThreadWithNullThreadId() {
        // 测试使用null线程ID清空线程
        messageSaver.clearThread(null);
        // 应该不会抛出异常
    }

    @Test
    void testClearAll() {
        // 测试清空所有数据
        String threadId1 = "thread1";
        String threadId2 = "thread2";

        Message msg1 = new UserMessage("Message 1");
        Message msg2 = new UserMessage("Message 2");

        BranchMessageItem branchMessageItem1 = new BranchMessageItem(msg1, "msg1", null, Map.of());
        BranchMessageItem branchMessageItem2 = new BranchMessageItem(msg2, "msg2", null, Map.of());

        // 保存到不同线程
        messageSaver.save(threadId1, branchMessageItem1);
        messageSaver.save(threadId2, branchMessageItem2);

        // 验证消息已保存
        assertEquals(1, messageSaver.getMessageCount(threadId1));
        assertEquals(1, messageSaver.getMessageCount(threadId2));

        // 清空所有数据
        messageSaver.clearAll();

        // 验证所有数据已清空
        assertEquals(0, messageSaver.getMessageCount(threadId1));
        assertEquals(0, messageSaver.getMessageCount(threadId2));
        assertNull(messageSaver.getLatestMessageId(threadId1));
        assertNull(messageSaver.getLatestMessageId(threadId2));
        assertTrue(messageSaver.getAllMessages(threadId1).isEmpty());
        assertTrue(messageSaver.getAllMessages(threadId2).isEmpty());
    }

    @Test
    void testGetMessageCountWithNullThreadId() {
        // 测试使用null线程ID获取消息数量
        assertEquals(0, messageSaver.getMessageCount(null));
    }

    @Test
    void testBranchMessages() {
        // 测试分支消息
        String threadId = "thread1";

        Message rootMsg = new UserMessage("Root message");
        Message branch1Msg = new AssistantMessage("Branch 1 message");
        Message branch2Msg = new AssistantMessage("Branch 2 message");

        BranchMessageItem rootBranchMessageItem = new BranchMessageItem(rootMsg, "root", null, Map.of());
        BranchMessageItem branch1BranchMessageItem = new BranchMessageItem(branch1Msg, "branch1", "root",
                Map.of("branch", "branch1"));
        BranchMessageItem branch2BranchMessageItem = new BranchMessageItem(branch2Msg, "branch2", "root",
                Map.of("branch", "branch2"));

        // 保存根消息
        messageSaver.save(threadId, rootBranchMessageItem);

        // 创建两个分支
        messageSaver.save(threadId, branch1BranchMessageItem);
        messageSaver.save(threadId, branch2BranchMessageItem);

        // 验证消息数量
        assertEquals(3, messageSaver.getMessageCount(threadId));

        // 验证最新消息ID（应该是最后保存的）
        assertEquals("branch2", messageSaver.getLatestMessageId(threadId));

        // 验证所有消息
        List<BranchMessageItem> allMessages = messageSaver.getAllMessages(threadId);
        assertEquals(3, allMessages.size());
    }
}