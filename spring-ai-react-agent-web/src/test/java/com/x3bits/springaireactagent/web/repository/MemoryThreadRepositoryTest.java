package com.x3bits.springaireactagent.web.repository;

import com.x3bits.springaireactagent.web.entity.Thread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryThreadRepository单元测试
 */
public class MemoryThreadRepositoryTest {

    private MemoryThreadRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MemoryThreadRepository();
    }

    @Test
    void testInsert() {
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("测试线程");
        thread.setAgent("test-agent");

        int result = repository.insert(thread);

        assertEquals(1, result);
        assertNotNull(thread.getId());
        assertNotNull(thread.getCreatedAt());
        assertNotNull(thread.getUpdatedAt());
        assertEquals(1, repository.size());
    }

    @Test
    void testFindByThreadId() {
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("测试线程");
        thread.setAgent("test-agent");

        repository.insert(thread);

        Thread found = repository.findByThreadId("thread-001");
        assertNotNull(found);
        assertEquals("user1", found.getUserId());
        assertEquals("thread-001", found.getThreadId());
        assertEquals("测试线程", found.getTitle());
        assertEquals("test-agent", found.getAgent());
    }

    @Test
    void testFindByThreadId_NotFound() {
        Thread found = repository.findByThreadId("nonexistent");
        assertNull(found);
    }

    @Test
    void testFindByUserIdAndAgent() {
        // 插入多个线程
        Thread thread1 = new Thread();
        thread1.setUserId("user1");
        thread1.setThreadId("thread-001");
        thread1.setTitle("第一个线程");
        thread1.setAgent("agent1");
        repository.insert(thread1);

        Thread thread2 = new Thread();
        thread2.setUserId("user1");
        thread2.setThreadId("thread-002");
        thread2.setTitle("第二个线程");
        thread2.setAgent("agent1");
        repository.insert(thread2);

        Thread thread3 = new Thread();
        thread3.setUserId("user2");
        thread3.setThreadId("thread-003");
        thread3.setTitle("其他用户的线程");
        thread3.setAgent("agent2");
        repository.insert(thread3);

        Thread thread4 = new Thread();
        thread4.setUserId("user1");
        thread4.setThreadId("thread-004");
        thread4.setTitle("不同代理的线程");
        thread4.setAgent("agent2");
        repository.insert(thread4);

        List<Thread> user1Agent1Threads = repository.findByUserIdAndAgent("user1", "agent1");
        assertEquals(2, user1Agent1Threads.size());
        assertTrue(user1Agent1Threads.stream().allMatch(t -> "user1".equals(t.getUserId()) && "agent1".equals(t.getAgent())));

        List<Thread> user1Agent2Threads = repository.findByUserIdAndAgent("user1", "agent2");
        assertEquals(1, user1Agent2Threads.size());
        assertEquals("user1", user1Agent2Threads.get(0).getUserId());
        assertEquals("agent2", user1Agent2Threads.get(0).getAgent());

        List<Thread> user2Agent2Threads = repository.findByUserIdAndAgent("user2", "agent2");
        assertEquals(1, user2Agent2Threads.size());
        assertEquals("user2", user2Agent2Threads.get(0).getUserId());
        assertEquals("agent2", user2Agent2Threads.get(0).getAgent());
    }

    @Test
    void testFindByUserIdAndAgent_OrderByUpdatedAt() {
        // 插入多个线程，并修改其中一个
        Thread thread1 = new Thread();
        thread1.setUserId("user1");
        thread1.setThreadId("thread-001");
        thread1.setTitle("第一个线程");
        thread1.setAgent("agent1");
        repository.insert(thread1);

        Thread thread2 = new Thread();
        thread2.setUserId("user1");
        thread2.setThreadId("thread-002");
        thread2.setTitle("第二个线程");
        thread2.setAgent("agent1");
        repository.insert(thread2);

        // 更新第一个线程的标题，使其更新时间更晚
        repository.updateTitle("thread-001", "更新后的标题");

        List<Thread> threads = repository.findByUserIdAndAgent("user1", "agent1");
        assertEquals(2, threads.size());
        // 最新更新的应该在前面
        assertEquals("thread-001", threads.get(0).getThreadId());
        assertEquals("更新后的标题", threads.get(0).getTitle());
    }

    @Test
    void testUpdateTitle() {
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("原标题");
        thread.setAgent("test-agent");
        repository.insert(thread);

        LocalDateTime originalUpdateTime = thread.getUpdatedAt();

        // 稍作延迟确保时间不同
        try {
            java.lang.Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int result = repository.updateTitle("thread-001", "新标题");
        assertEquals(1, result);

        Thread updated = repository.findByThreadId("thread-001");
        assertEquals("新标题", updated.getTitle());
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdateTime));
    }

    @Test
    void testUpdateTitle_NotFound() {
        int result = repository.updateTitle("nonexistent", "新标题");
        assertEquals(0, result);
    }

    @Test
    void testDeleteById() {
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("测试线程");
        thread.setAgent("test-agent");
        repository.insert(thread);

        Long threadId = thread.getId();

        int result = repository.deleteById(threadId);
        assertEquals(1, result);
        assertEquals(0, repository.size());
        assertNull(repository.findByThreadId("thread-001"));
    }

    @Test
    void testDeleteById_NotFound() {
        int result = repository.deleteById(9999L);
        assertEquals(0, result);
    }

    @Test
    void testDeleteByThreadId() {
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("测试线程");
        thread.setAgent("test-agent");
        repository.insert(thread);

        int result = repository.deleteByThreadId("thread-001");
        assertEquals(1, result);
        assertEquals(0, repository.size());
        assertNull(repository.findByThreadId("thread-001"));
    }

    @Test
    void testDeleteByThreadId_NotFound() {
        int result = repository.deleteByThreadId("nonexistent");
        assertEquals(0, result);
    }

    @Test
    void testClear() {
        Thread thread1 = new Thread();
        thread1.setUserId("user1");
        thread1.setThreadId("thread-001");
        thread1.setTitle("第一个线程");
        thread1.setAgent("test-agent");
        repository.insert(thread1);

        Thread thread2 = new Thread();
        thread2.setUserId("user1");
        thread2.setThreadId("thread-002");
        thread2.setTitle("第二个线程");
        thread2.setAgent("test-agent");
        repository.insert(thread2);

        assertEquals(2, repository.size());

        repository.clear();
        assertEquals(0, repository.size());
    }

    @Test
    void testCompleteWorkflow() {
        // 插入
        Thread thread = new Thread();
        thread.setUserId("user1");
        thread.setThreadId("thread-001");
        thread.setTitle("测试线程");
        thread.setAgent("test-agent");

        int insertResult = repository.insert(thread);
        assertEquals(1, insertResult);
        assertNotNull(thread.getId());

        // 查询
        Thread retrievedThread = repository.findByThreadId("thread-001");
        assertNotNull(retrievedThread);
        assertEquals("user1", retrievedThread.getUserId());
        assertEquals("thread-001", retrievedThread.getThreadId());
        assertEquals("测试线程", retrievedThread.getTitle());

        // 更新
        int updateResult = repository.updateTitle("thread-001", "更新后的标题");
        assertEquals(1, updateResult);

        Thread updatedThread = repository.findByThreadId("thread-001");
        assertEquals("更新后的标题", updatedThread.getTitle());

        // 删除
        int deleteResult = repository.deleteByThreadId("thread-001");
        assertEquals(1, deleteResult);

        Thread deletedThread = repository.findByThreadId("thread-001");
        assertNull(deletedThread);
    }
}