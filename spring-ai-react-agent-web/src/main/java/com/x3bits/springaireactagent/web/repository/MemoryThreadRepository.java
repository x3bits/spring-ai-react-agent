package com.x3bits.springaireactagent.web.repository;

import com.x3bits.springaireactagent.web.entity.Thread;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread数据访问层内存实现类
 */
@Repository
public class MemoryThreadRepository implements ThreadRepository {

    /**
     * 内存存储，使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();

    /**
     * 主键生成器，使用 AtomicLong 保证原子性
     */
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 根据用户ID和代理查询所有线程
     *
     * @param userId 用户ID
     * @param agent 代理
     * @return 线程列表
     */
    @Override
    public List<Thread> findByUserIdAndAgent(String userId, String agent) {
        return threads.values().stream()
                .filter(thread -> userId.equals(thread.getUserId()) && agent.equals(thread.getAgent()))
                .sorted(Comparator.comparing(Thread::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 根据线程ID查询线程
     * 
     * @param threadId 线程ID
     * @return 线程对象
     */
    @Override
    public Thread findByThreadId(String threadId) {
        return threads.get(threadId);
    }

    /**
     * 插入新线程
     * 
     * @param thread 线程对象
     * @return 影响行数
     */
    @Override
    public int insert(Thread thread) {
        if (thread == null) {
            return 0;
        }

        // 生成主键ID
        Long id = idGenerator.getAndIncrement();
        thread.setId(id);

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        thread.setCreatedAt(now);
        thread.setUpdatedAt(now);

        // 存储到内存中
        threads.put(thread.getThreadId(), thread);

        return 1;
    }

    /**
     * 更新线程标题
     * 
     * @param threadId 线程ID
     * @param title    新标题
     * @return 影响行数
     */
    @Override
    public int updateTitle(String threadId, String title) {
        Thread thread = threads.get(threadId);
        if (thread == null) {
            return 0;
        }

        thread.setTitle(title);
        thread.setUpdatedAt(LocalDateTime.now());

        return 1;
    }

    /**
     * 根据ID删除线程
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    public int deleteById(Long id) {
        Thread threadToRemove = threads.values().stream()
                .filter(thread -> id.equals(thread.getId()))
                .findFirst()
                .orElse(null);

        if (threadToRemove != null) {
            threads.remove(threadToRemove.getThreadId());
            return 1;
        }

        return 0;
    }

    /**
     * 根据线程ID删除线程
     * 
     * @param threadId 线程ID
     * @return 影响行数
     */
    @Override
    public int deleteByThreadId(String threadId) {
        Thread removed = threads.remove(threadId);
        return removed != null ? 1 : 0;
    }

    /**
     * 清空所有数据（仅用于测试）
     */
    public void clear() {
        threads.clear();
        idGenerator.set(1);
    }

    /**
     * 获取所有线程数量（仅用于测试）
     */
    public int size() {
        return threads.size();
    }
}