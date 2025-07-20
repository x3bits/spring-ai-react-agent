-- Test Data for H2 Database
INSERT INTO
    threads (
        user_id,
        thread_id,
        title,
        agent,
        created_at,
        updated_at
    )
VALUES
    (
        'user1',
        'thread-001',
        '第一个测试线程',
        'agent1',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'user1',
        'thread-002',
        '关于Spring Boot的讨论',
        'agent1',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'user2',
        'thread-003',
        'MyBatis集成测试',
        'agent2',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'user1',
        'thread-004',
        'AI Agent开发测试',
        'agent2',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'user3',
        'thread-005',
        'H2数据库测试',
        'agent1',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );