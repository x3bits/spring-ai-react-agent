package com.x3bits.springaireactagent.web.repository;

import com.x3bits.springaireactagent.web.entity.Thread;

import java.util.List;

/**
 * Thread数据访问层接口
 */
public interface ThreadRepository {

    /**
     * 根据用户ID和代理查询所有线程
     *
     * @param userId 用户ID
     * @param agent 代理
     * @return 线程列表
     */
    List<Thread> findByUserIdAndAgent(String userId, String agent);

    /**
     * 根据线程ID查询线程
     * 
     * @param threadId 线程ID
     * @return 线程对象
     */
    Thread findByThreadId(String threadId);

    /**
     * 插入新线程
     * 
     * @param thread 线程对象
     * @return 影响行数
     */
    int insert(Thread thread);

    /**
     * 更新线程标题
     * 
     * @param threadId 线程ID
     * @param title    新标题
     * @return 影响行数
     */
    int updateTitle(String threadId, String title);

    /**
     * 根据ID删除线程
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据线程ID删除线程
     * 
     * @param threadId 线程ID
     * @return 影响行数
     */
    int deleteByThreadId(String threadId);
}