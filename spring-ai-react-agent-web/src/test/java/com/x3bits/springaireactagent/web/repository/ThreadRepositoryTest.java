package com.x3bits.springaireactagent.web.repository;

import com.x3bits.springaireactagent.web.entity.Thread;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreadRepository单元测试
 */
@JdbcTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = { "classpath:schema.sql", "classpath:data.sql" })
public class ThreadRepositoryTest {

    @Autowired
    private ThreadRepository threadRepository;

    @Configuration
    static class TestConfig {
        @Bean
        public ThreadRepository threadRepository() {
            return new MysqlThreadRepository();
        }
    }

    @Test
    public void testFindByUserIdAndAgent() {
        // 测试根据用户ID和代理查询线程
        List<Thread> threads = threadRepository.findByUserIdAndAgent("user1", "agent1");

        assertNotNull(threads);
        assertEquals(2, threads.size());

        // 验证第一个线程的信息
        Thread firstThread = threads.get(0);
        assertNotNull(firstThread.getId());
        assertEquals("user1", firstThread.getUserId());
        assertEquals("agent1", firstThread.getAgent());
        assertNotNull(firstThread.getThreadId());
        assertNotNull(firstThread.getTitle());
        assertNotNull(firstThread.getCreatedAt());
        assertNotNull(firstThread.getUpdatedAt());
    }

    @Test
    public void testFindByUserIdAndAgent_NotFound() {
        // 测试查询不存在的用户ID和代理
        List<Thread> threads = threadRepository.findByUserIdAndAgent("nonexistent_user", "nonexistent_agent");

        assertNotNull(threads);
        assertTrue(threads.isEmpty());
    }

    @Test
    public void testFindByThreadId() {
        // 测试根据线程ID查询线程
        Thread thread = threadRepository.findByThreadId("thread-001");

        assertNotNull(thread);
        assertEquals("user1", thread.getUserId());
        assertEquals("thread-001", thread.getThreadId());
        assertEquals("第一个测试线程", thread.getTitle());
        assertNotNull(thread.getCreatedAt());
        assertNotNull(thread.getUpdatedAt());
    }

    @Test
    public void testFindByThreadId_NotFound() {
        // 测试查询不存在的线程ID
        Thread thread = threadRepository.findByThreadId("nonexistent_thread");

        assertNull(thread);
    }

    @Test
    public void testInsert() {
        // 测试插入新线程
        Thread thread = new Thread("user2", "new-thread-id", "New Thread", "test-agent");
        int result = threadRepository.insert(thread);

        assertEquals(1, result);
        assertNotNull(thread.getId());

        // 验证插入的线程能够被查询到
        Thread savedThread = threadRepository.findByThreadId("new-thread-id");
        assertNotNull(savedThread);
        assertEquals("user2", savedThread.getUserId());
        assertEquals("new-thread-id", savedThread.getThreadId());
        assertEquals("New Thread", savedThread.getTitle());
        assertEquals("test-agent", savedThread.getAgent());
    }

    @Test
    public void testUpdateTitle() {
        // 测试更新线程标题
        String threadId = "thread-001";
        String newTitle = "Updated Thread Title";

        int result = threadRepository.updateTitle(threadId, newTitle);
        assertEquals(1, result);

        // 验证更新是否成功
        Thread updatedThread = threadRepository.findByThreadId(threadId);
        assertNotNull(updatedThread);
        assertEquals(newTitle, updatedThread.getTitle());
    }

    @Test
    public void testUpdateTitle_NotFound() {
        // 测试更新不存在的线程标题
        int result = threadRepository.updateTitle("nonexistent_thread", "New Title");
        assertEquals(0, result);
    }

    @Test
    public void testDeleteById() {
        // 先查询一个线程获取其ID
        Thread thread = threadRepository.findByThreadId("thread-001");
        assertNotNull(thread);
        Long threadIdToDelete = thread.getId();

        // 删除线程
        int result = threadRepository.deleteById(threadIdToDelete);
        assertEquals(1, result);

        // 验证删除是否成功
        Thread deletedThread = threadRepository.findByThreadId("thread-001");
        assertNull(deletedThread);
    }

    @Test
    public void testDeleteById_NotFound() {
        // 测试删除不存在的线程ID
        int result = threadRepository.deleteById(9999L);
        assertEquals(0, result);
    }

    @Test
    public void testDeleteByThreadId() {
        // 测试根据线程ID删除线程
        int result = threadRepository.deleteByThreadId("thread-001");
        assertEquals(1, result);

        // 验证删除是否成功
        Thread deletedThread = threadRepository.findByThreadId("thread-001");
        assertNull(deletedThread);
    }

    @Test
    public void testDeleteByThreadId_NotFound() {
        // 测试删除不存在的线程ID
        int result = threadRepository.deleteByThreadId("nonexistent_thread");
        assertEquals(0, result);
    }

    @Test
    public void testCompleteWorkflow() {
        // 测试完整的工作流程：创建、查询、更新、删除
        String userId = "test_user";
        String threadId = "test_thread";
        String title = "Test Thread";

        // 1. 创建线程
        Thread thread = new Thread(userId, threadId, title, "test-agent");
        int insertResult = threadRepository.insert(thread);
        assertEquals(1, insertResult);
        assertNotNull(thread.getId());

        // 2. 查询线程
        Thread retrievedThread = threadRepository.findByThreadId(threadId);
        assertNotNull(retrievedThread);
        assertEquals(userId, retrievedThread.getUserId());
        assertEquals(threadId, retrievedThread.getThreadId());
        assertEquals(title, retrievedThread.getTitle());

        // 3. 更新线程标题
        String newTitle = "Updated Test Thread";
        int updateResult = threadRepository.updateTitle(threadId, newTitle);
        assertEquals(1, updateResult);

        Thread updatedThread = threadRepository.findByThreadId(threadId);
        assertNotNull(updatedThread);
        assertEquals(newTitle, updatedThread.getTitle());

        // 4. 删除线程
        int deleteResult = threadRepository.deleteByThreadId(threadId);
        assertEquals(1, deleteResult);

        Thread deletedThread = threadRepository.findByThreadId(threadId);
        assertNull(deletedThread);
    }

    @Test
    public void testFindByUserIdAndAgent_OrderByUpdatedAt() {
        // 测试结果是否按照 updated_at 降序排列
        List<Thread> threads = threadRepository.findByUserIdAndAgent("user1", "agent1");

        assertNotNull(threads);
        assertTrue(threads.size() > 1);

        // 验证排序（最新的在前面）
        for (int i = 1; i < threads.size(); i++) {
            Thread current = threads.get(i);
            Thread previous = threads.get(i - 1);

            // 当前线程的更新时间应该早于或等于前一个线程的更新时间
            assertTrue(current.getUpdatedAt().compareTo(previous.getUpdatedAt()) <= 0);
        }
    }
}