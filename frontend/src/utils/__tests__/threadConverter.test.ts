import { convertThreadItemsToMessages, selectBranchPathToTarget } from '../threadConverter';
import { ThreadItem } from '../../api/agentService';

describe('convertThreadItemsToMessages', () => {
    // 测试用例1：空数组
    test('should return empty result for empty array', () => {
        const result = convertThreadItemsToMessages([]);
        expect(result.rootMessage).toBeUndefined();
        expect(result.lastMessage).toBeUndefined();
        expect(result.rootCheckpointId).toBeUndefined();
    });

    // 测试用例2：简单的单消息对话
    test('should handle simple single message conversation', () => {
        const threadItems: ThreadItem[] = [
            {
                threadId: "test-thread",
                checkpointId: "user-1",
                previousCheckpointId: "root",
                type: "user",
                content: [
                    {
                        id: "user-1",
                        type: "userEvent",
                        content: "你好"
                    }
                ]
            }
        ];

        const result = convertThreadItemsToMessages(threadItems);

        expect(result.rootMessage).toBeDefined();
        expect(result.lastMessage).toBeDefined();
        expect(result.rootCheckpointId).toBe("root");

        // 检查消息结构
        const rootMessage = result.rootMessage!;
        expect(rootMessage.branches).toHaveLength(1);
        expect(rootMessage.branches[0].role).toBe("user");
        expect(rootMessage.branches[0].parts).toHaveLength(1);
        expect(rootMessage.branches[0].parts[0]).toEqual({
            type: "text",
            content: "你好"
        });
    });

    // 测试用例3：用户-助手对话
    test('should handle user-assistant conversation', () => {
        const threadItems: ThreadItem[] = [
            {
                threadId: "test-thread",
                checkpointId: "user-1",
                previousCheckpointId: "root",
                type: "user",
                content: [
                    {
                        id: "user-1",
                        type: "userEvent",
                        content: "你好"
                    }
                ]
            },
            {
                threadId: "test-thread",
                checkpointId: "assistant-1",
                previousCheckpointId: "user-1",
                type: "assistant",
                content: [
                    {
                        id: "assistant-1",
                        type: "assistantContent",
                        data: {
                            type: "text",
                            content: "你好！有什么可以帮助你的吗？"
                        }
                    }
                ]
            }
        ];

        const result = convertThreadItemsToMessages(threadItems);

        expect(result.rootMessage).toBeDefined();
        expect(result.lastMessage).toBeDefined();

        // 检查消息链
        const rootMessage = result.rootMessage!;
        expect(rootMessage.branches[0].role).toBe("user");
        expect(rootMessage.branches[0].nextMessage).toBeDefined();

        const nextMessage = rootMessage.branches[0].nextMessage!;
        expect(nextMessage.branches[0].role).toBe("assistant");
        expect(nextMessage.branches[0].parts[0]).toEqual({
            type: "text",
            content: "你好！有什么可以帮助你的吗？"
        });
    });

    // 测试用例4：带分支的复杂对话（你提供的示例）
    test('should handle complex conversation with branches', () => {
        const threadItems: ThreadItem[] = [
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054ec8-10de-6f12-8000-2107ada45553",
                previousCheckpointId: "1f054ec8-10d7-6096-bfff-586f22ee512b",
                type: "user",
                content: [
                    {
                        id: "1f054ec8-10de-6f12-8000-2107ada45553",
                        type: "userEvent",
                        content: "你好！"
                    }
                ]
            },
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054ec8-1640-6f64-8001-d206a7ae3500",
                previousCheckpointId: "1f054ec8-10de-6f12-8000-2107ada45553",
                type: "assistant",
                content: [
                    {
                        type: "assistantContent",
                        data: {
                            type: "text",
                            content: "你好！有什么可以帮助你的吗？"
                        }
                    }
                ]
            },
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054ec8-7ab4-67f2-8001-d5155a7c31c3",
                previousCheckpointId: "1f054ec8-10de-6f12-8000-2107ada45553",
                type: "assistant",
                content: [
                    {
                        type: "assistantContent",
                        data: {
                            type: "text",
                            content: "你好！有什么可以帮助你的吗？"
                        }
                    }
                ]
            },
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054f00-abd4-6c96-8001-6c11c1dc2e38",
                previousCheckpointId: "1f054ec8-10d7-6096-bfff-586f22ee512b",
                type: "user",
                content: [
                    {
                        id: "1f054f00-abd4-6c96-8001-6c11c1dc2e38",
                        type: "userEvent",
                        content: "我叫小明"
                    }
                ]
            },
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054f00-b44e-6462-8002-234ce60ccc72",
                previousCheckpointId: "1f054f00-abd4-6c96-8001-6c11c1dc2e38",
                type: "assistant",
                content: [
                    {
                        type: "assistantContent",
                        data: {
                            type: "text",
                            content: "你好，小明！有什么我可以帮你的吗？"
                        }
                    }
                ]
            },
            {
                threadId: "6bfe70b5-6c3e-47e7-b88e-73013c99f347",
                checkpointId: "1f054f00-d1d2-6da8-8002-6f22e323512e",
                previousCheckpointId: "1f054f00-abd4-6c96-8001-6c11c1dc2e38",
                type: "assistant",
                content: [
                    {
                        type: "assistantContent",
                        data: {
                            type: "text",
                            content: "你好，小明！有什么我可以帮助你的吗？"
                        }
                    }
                ]
            }
        ];

        const result = convertThreadItemsToMessages(threadItems);

        expect(result.rootMessage).toBeDefined();
        expect(result.lastMessage).toBeDefined();

        const rootMessage = result.rootMessage!;

        // 应该有2个分支：「你好！」和「我叫小明」
        expect(rootMessage.branches).toHaveLength(2);

        // 检查第一个分支 - "你好！"
        const helloBranch = rootMessage.branches.find(b =>
            b.parts.some(p => p.type === 'text' && p.content === '你好！')
        );
        expect(helloBranch).toBeDefined();
        expect(helloBranch!.nextMessage).toBeDefined();
        expect(helloBranch!.nextMessage!.branches).toHaveLength(2); // 有2个助手回复分支

        // 检查第二个分支 - "我叫小明"
        const nameBranch = rootMessage.branches.find(b =>
            b.parts.some(p => p.type === 'text' && p.content === '我叫小明')
        );
        expect(nameBranch).toBeDefined();
        expect(nameBranch!.nextMessage).toBeDefined();
        expect(nameBranch!.nextMessage!.branches).toHaveLength(2); // 有2个助手回复分支

        // 验证不同分支链接到不同的下一个消息
        expect(helloBranch!.nextMessage).not.toBe(nameBranch!.nextMessage);

        // 检查最后一个ThreadItem被选中的路径
        // 最后一个ThreadItem的checkpoint_id是 "1f054f00-d1d2-6da8-8002-6f22e323512e"
        // 应该选择 "我叫小明" 分支
        expect(rootMessage.currentBranch).toBe(1); // 第二个分支
    });

    // 测试用例5：工具调用
    test('should handle tool calls', () => {
        const threadItems: ThreadItem[] = [
            {
                threadId: "test-thread",
                checkpointId: "user-1",
                previousCheckpointId: "root",
                type: "user",
                content: [
                    {
                        id: "user-1",
                        type: "userEvent",
                        content: "123+456等于多少？"
                    }
                ]
            },
            {
                threadId: "test-thread",
                checkpointId: "assistant-1",
                previousCheckpointId: "user-1",
                type: "assistant",
                content: [
                    {
                        type: "assistantContent",
                        data: {
                            type: "toolCall",
                            id: "call_123",
                            name: "add_integers",
                            args: { a: 123, b: 456 }
                        }
                    }
                ]
            },
            {
                threadId: "test-thread",
                checkpointId: "tool-result-1",
                previousCheckpointId: "assistant-1",
                type: "assistant",
                content: [
                    {
                        type: "toolResult",
                        content: "579",
                        callId: "call_123"
                    }
                ]
            }
        ];

        const result = convertThreadItemsToMessages(threadItems);

        expect(result.rootMessage).toBeDefined();

        const rootMessage = result.rootMessage!;
        const assistantMessage = rootMessage.branches[0].nextMessage!;
        const toolResultMessage = assistantMessage.branches[0].nextMessage!;

        // 检查工具调用
        expect(assistantMessage.branches[0].parts[0]).toEqual({
            type: "toolCall",
            toolName: "add_integers",
            parameters: { a: 123, b: 456 },
            toolCallId: "call_123"
        });

        // 检查工具结果
        expect(toolResultMessage.branches[0].parts[0]).toEqual({
            type: "toolCallResponse",
            data: "579",
            toolCallId: "call_123"
        });
    });
});

describe('selectBranchPathToTarget', () => {
    test('should select correct branch path to target', () => {
        // 创建一个简单的消息树用于测试
        const message = {
            branches: [
                {
                    id: "branch-1",
                    role: "user" as const,
                    parts: [],
                    nextMessage: {
                        branches: [
                            { id: "target-checkpoint", role: "assistant" as const, parts: [] }
                        ],
                        currentBranch: 0
                    }
                },
                {
                    id: "branch-2",
                    role: "user" as const,
                    parts: [],
                    nextMessage: {
                        branches: [
                            { id: "other-checkpoint", role: "assistant" as const, parts: [] }
                        ],
                        currentBranch: 0
                    }
                }
            ],
            currentBranch: 0
        };

        const found = selectBranchPathToTarget(message, "target-checkpoint");

        expect(found).toBe(true);
        expect(message.currentBranch).toBe(0); // 应该选择第一个分支
        expect(message.branches[0].nextMessage!.currentBranch).toBe(0);
    });

    test('should return false if target not found', () => {
        const message = {
            branches: [
                { id: "branch-1", role: "user" as const, parts: [] }
            ],
            currentBranch: 0
        };

        const found = selectBranchPathToTarget(message, "non-existent");
        expect(found).toBe(false);
    });
}); 