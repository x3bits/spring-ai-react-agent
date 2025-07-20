package com.x3bits.springaireactagent.web.repository;

import com.x3bits.springaireactagent.web.entity.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

/**
 * Thread数据访问层MySQL实现类
 */
@Repository
public class MysqlThreadRepository implements ThreadRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Thread结果映射器
     */
    private final RowMapper<Thread> threadRowMapper = new RowMapper<Thread>() {
        @Override
        public Thread mapRow(ResultSet rs, int rowNum) throws SQLException {
            Thread thread = new Thread();
            thread.setId(rs.getLong("id"));
            thread.setUserId(rs.getString("user_id"));
            thread.setThreadId(rs.getString("thread_id"));
            thread.setTitle(rs.getString("title"));
            thread.setAgent(rs.getString("agent"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                thread.setCreatedAt(createdAt.toLocalDateTime());
            }

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                thread.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            return thread;
        }
    };

    /**
     * 根据用户ID和代理查询所有线程
     *
     * @param userId 用户ID
     * @param agent 代理
     * @return 线程列表
     */
    @Override
    public List<Thread> findByUserIdAndAgent(String userId, String agent) {
        String sql = """
                SELECT id, user_id, thread_id, title, agent, created_at, updated_at
                FROM threads
                WHERE user_id = ? AND agent = ?
                ORDER BY updated_at DESC
                """;
        return jdbcTemplate.query(sql, threadRowMapper, userId, agent);
    }

    /**
     * 根据线程ID查询线程
     * 
     * @param threadId 线程ID
     * @return 线程对象
     */
    @Override
    public Thread findByThreadId(String threadId) {
        String sql = """
                SELECT id, user_id, thread_id, title, agent, created_at, updated_at
                FROM threads
                WHERE thread_id = ?
                """;
        List<Thread> threads = jdbcTemplate.query(sql, threadRowMapper, threadId);
        return threads.isEmpty() ? null : threads.get(0);
    }

    /**
     * 插入新线程
     * 
     * @param thread 线程对象
     * @return 影响行数
     */
    @Override
    public int insert(Thread thread) {
        String sql = """
                INSERT INTO threads (user_id, thread_id, title, agent, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, thread.getUserId());
            ps.setString(2, thread.getThreadId());
            ps.setString(3, thread.getTitle());
            ps.setString(4, thread.getAgent());
            return ps;
        }, keyHolder);

        // 设置生成的主键
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            Object id = keyHolder.getKeys().get("ID");
            if (id != null) {
                thread.setId(((Number) id).longValue());
            }
        }

        return affectedRows;
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
        String sql = """
                UPDATE threads
                SET title = ?, updated_at = CURRENT_TIMESTAMP
                WHERE thread_id = ?
                """;
        return jdbcTemplate.update(sql, title, threadId);
    }

    /**
     * 根据ID删除线程
     * 
     * @param id 主键ID
     * @return 影响行数
     */
    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM threads WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    /**
     * 根据线程ID删除线程
     * 
     * @param threadId 线程ID
     * @return 影响行数
     */
    @Override
    public int deleteByThreadId(String threadId) {
        String sql = "DELETE FROM threads WHERE thread_id = ?";
        return jdbcTemplate.update(sql, threadId);
    }
}