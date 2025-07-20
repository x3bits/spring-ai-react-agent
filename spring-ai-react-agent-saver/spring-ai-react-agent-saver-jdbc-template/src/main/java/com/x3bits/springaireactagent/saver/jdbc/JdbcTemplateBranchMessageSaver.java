package com.x3bits.springaireactagent.saver.jdbc;

import com.x3bits.springaireactagent.core.memory.BranchMessageSaver;
import com.x3bits.springaireactagent.core.message.BranchMessageItem;
import com.x3bits.springaireactagent.serializer.MessageSerializer;
import org.springframework.ai.chat.messages.Message;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC 实现的分支消息保存器
 * <p>
 * 使用关系数据库存储消息分支，支持高效的消息链查询。
 * 采用祖先路径存储方案来优化深度查询性能。
 * </p>
 */
public class JdbcTemplateBranchMessageSaver implements BranchMessageSaver {

    private final JdbcTemplate jdbcTemplate;
    private final MessageSerializer messageSerializer;

    public JdbcTemplateBranchMessageSaver(JdbcTemplate jdbcTemplate, MessageSerializer messageSerializer) {
        this.jdbcTemplate = jdbcTemplate;
        this.messageSerializer = messageSerializer;
    }

    @Override
    public void save(String threadId, BranchMessageItem branchMessageItem) {
        if (threadId == null || branchMessageItem == null) {
            throw new IllegalArgumentException("threadId and branchMessageItem cannot be null");
        }

        String messageId = branchMessageItem.id();
        String previousId = branchMessageItem.previousId();
        Message message = branchMessageItem.message();

        // 序列化消息内容
        String messageContent = messageSerializer.serialize(message);

        // 计算深度和祖先路径
        int depth = 0;
        String ancestorPath = "";

        if (previousId != null && !previousId.isEmpty()) {
            try {
                Map<String, Object> parentInfo = jdbcTemplate.queryForMap(
                        "SELECT depth, ancestor_path FROM message_branch WHERE message_id = ?",
                        previousId);

                depth = (Integer) parentInfo.get("depth") + 1;
                String parentPath = (String) parentInfo.get("ancestor_path");

                // 构建祖先路径
                ancestorPath = parentPath != null && !parentPath.isEmpty()
                        ? parentPath + "," + previousId
                        : previousId;
            } catch (EmptyResultDataAccessException e) {
                // 如果父消息不存在，当作根消息处理
                depth = 0;
                ancestorPath = "";
            }
        }

        // 插入消息
        jdbcTemplate.update(
                "INSERT INTO message_branch (message_id, thread_id, previous_id, message_type, message_content, depth, ancestor_path) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                messageId, threadId, previousId,
                message.getMessageType().name(),
                messageContent,
                depth, ancestorPath);
    }

    @Override
    public String getLatestMessageId(String threadId) {
        if (threadId == null) {
            return null;
        }

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT message_id FROM message_branch WHERE thread_id = ? ORDER BY id DESC LIMIT 1",
                    String.class, threadId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<BranchMessageItem> getAllMessages(String threadId) {
        if (threadId == null) {
            return List.of();
        }

        return jdbcTemplate.query(
                "SELECT message_id, thread_id, previous_id, message_type, message_content, depth, ancestor_path " +
                        "FROM message_branch WHERE thread_id = ? ORDER BY id ASC",
                new BranchMessageItemRowMapper(),
                threadId);
    }

    @Override
    public List<Message> getLatestMessages(String threadId, int count, String lastMessageId) {
        if (threadId == null || count <= 0) {
            return List.of();
        }

        // 如果没有指定 lastMessageId，使用最新消息
        String startMessageId = lastMessageId;
        if (startMessageId == null) {
            startMessageId = getLatestMessageId(threadId);
        }

        if (startMessageId == null) {
            return List.of();
        }

        // 获取起始消息的祖先路径
        String ancestorPath;
        try {
            ancestorPath = jdbcTemplate.queryForObject(
                    "SELECT ancestor_path FROM message_branch WHERE message_id = ? AND thread_id = ?",
                    String.class, startMessageId, threadId);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }

        // 解析路径并获取需要的消息ID
        List<String> messageIds = parseAncestorPath(ancestorPath, startMessageId, count);

        if (messageIds.isEmpty()) {
            return List.of();
        }

        // 批量查询所有相关消息
        String inClause = String.join(",", Collections.nCopies(messageIds.size(), "?"));
        List<BranchMessageItem> messages = jdbcTemplate.query(
                "SELECT message_id, thread_id, previous_id, message_type, message_content, depth, ancestor_path " +
                        "FROM message_branch WHERE message_id IN (" + inClause + ") ORDER BY depth ASC",
                new BranchMessageItemRowMapper(),
                messageIds.toArray());

        return messages.stream()
                .map(BranchMessageItem::message)
                .collect(Collectors.toList());
    }

    /**
     * 解析祖先路径，获取最近的N个祖先ID
     */
    private List<String> parseAncestorPath(String ancestorPath, String currentId, int count) {
        List<String> result = new ArrayList<>();
        result.add(currentId);

        if (ancestorPath != null && !ancestorPath.isEmpty()) {
            String[] ancestors = ancestorPath.split(",");
            // 从最近的祖先开始取
            int startIndex = Math.max(0, ancestors.length - count + 1);
            for (int i = startIndex; i < ancestors.length; i++) {
                result.add(ancestors[i]);
            }
        }

        return result;
    }

    /**
     * 用于将数据库行映射为 BranchMessageItem 的 RowMapper
     */
    private class BranchMessageItemRowMapper implements RowMapper<BranchMessageItem> {
        @Override
        public BranchMessageItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            String messageId = rs.getString("message_id");
            String previousId = rs.getString("previous_id");
            String messageType = rs.getString("message_type");
            String messageContent = rs.getString("message_content");

            // 反序列化消息
            Message message = messageSerializer.deserialize(
                    org.springframework.ai.chat.messages.MessageType.valueOf(messageType),
                    messageContent);

            // 忽略 metadata 字段，使用空 Map
            return new BranchMessageItem(message, messageId, previousId, Map.of());
        }
    }
}