package com.x3bits.springaireactagent.web.service;

import com.x3bits.springaireactagent.web.entity.Thread;
import com.x3bits.springaireactagent.web.repository.ThreadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Thread服务类
 */
@Service
public class ThreadService {

    @Autowired
    private ThreadRepository threadRepository;

    /**
     * 根据用户ID和代理获取所有线程
     *
     * @param userId 用户ID
     * @param agent 代理
     * @return 线程列表
     */
    public List<Thread> getThreadsByUserIdAndAgent(String userId, String agent) {
        return threadRepository.findByUserIdAndAgent(userId, agent);
    }

    /**
     * 根据线程ID获取线程
     * 
     * @param threadId 线程ID
     * @return 线程对象
     */
    public Thread getThreadByThreadId(String threadId) {
        return threadRepository.findByThreadId(threadId);
    }

    /**
     * 创建新线程
     *
     * @param userId 用户ID
     * @param title  线程标题
     * @param agent  代理
     * @return 创建的线程对象
     */
    @Transactional
    public Thread createThread(String userId, String title, String agent) {
        String threadId = UUID.randomUUID().toString();
        Thread thread = new Thread(userId, threadId, title, agent);

        int result = threadRepository.insert(thread);
        if (result > 0) {
            return thread;
        } else {
            throw new RuntimeException("Failed to create thread");
        }
    }

    /**
     * 更新线程标题
     * 
     * @param threadId 线程ID
     * @param newTitle 新标题
     * @return 是否更新成功
     */
    @Transactional
    public boolean updateThreadTitle(String threadId, String newTitle) {
        int result = threadRepository.updateTitle(threadId, newTitle);
        return result > 0;
    }

    /**
     * 删除线程
     * 
     * @param threadId 线程ID
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteThread(String threadId) {
        int result = threadRepository.deleteByThreadId(threadId);
        return result > 0;
    }

    /**
     * 检查线程是否存在
     * 
     * @param threadId 线程ID
     * @return 是否存在
     */
    public boolean threadExists(String threadId) {
        Thread thread = threadRepository.findByThreadId(threadId);
        return thread != null;
    }
}