import { ThreadItem } from '../api/agentService';
import { Message, MessageBranch, Part } from '../store/chatStore';

// 将服务端的 ThreadItem 转换为客户端的 Message 结构
export const convertThreadItemsToMessages = (threadItems: ThreadItem[]): { rootMessage?: Message, lastMessage?: Message, rootCheckpointId?: string } => {
    if (threadItems.length === 0) {
        return {};
    }

    // 按 previousCheckpointId 分组，构建消息链
    const messageGroups: { [key: string]: ThreadItem[] } = {};

    for (const item of threadItems) {
        const groupKey = item.previousCheckpointId;
        if (!messageGroups[groupKey]) {
            messageGroups[groupKey] = [];
        }
        messageGroups[groupKey].push(item);
    }

    // 构建所有消息
    const messages: { [key: string]: Message } = {};
    for (const [previousCheckpointId, items] of Object.entries(messageGroups)) {
        messages[previousCheckpointId] = createMessageFromThreadItems(items);
    }

    // 找到根消息（没有 previousCheckpointId 在 threadItems 中的）
    const allCheckpointIds = new Set(threadItems.map(item => item.checkpointId));
    let rootCheckpointId: string | undefined;
    let rootMessage: Message | undefined;

    for (const [previousCheckpointId] of Object.entries(messageGroups)) {
        if (!allCheckpointIds.has(previousCheckpointId)) {
            rootCheckpointId = previousCheckpointId;
            rootMessage = messages[previousCheckpointId];
            break;
        }
    }

    if (!rootMessage) {
        console.error('无法找到根消息');
        return {};
    }

    // 为每个消息的每个分支链接下一个消息
    for (const message of Object.values(messages)) {
        for (const branch of message.branches) {
            const nextMessage = messages[branch.id];
            if (nextMessage) {
                branch.nextMessage = nextMessage;
            }
        }
    }

    // 设置正确的分支路径，使最后一个 ThreadItem 能够显示
    if (rootMessage && threadItems.length > 0) {
        const lastThreadItem = threadItems[threadItems.length - 1];
        selectBranchPathToTarget(rootMessage, lastThreadItem.checkpointId);
    }

    // 找到最后一个消息（沿着选定的分支路径）
    let lastMessage = rootMessage;
    if (lastMessage) {
        while (true) {
            const currentBranch = lastMessage.branches[lastMessage.currentBranch];
            if (currentBranch.nextMessage) {
                lastMessage = currentBranch.nextMessage;
            } else {
                break;
            }
        }
    }

    return { rootMessage, lastMessage, rootCheckpointId };
};

// 选择分支路径以显示目标 checkpoint
export const selectBranchPathToTarget = (message: Message, targetCheckpointId: string): boolean => {
    // 检查当前消息的分支中是否包含目标 checkpoint
    for (let i = 0; i < message.branches.length; i++) {
        const branch = message.branches[i];

        if (branch.id === targetCheckpointId) {
            // 找到目标，设置当前分支
            message.currentBranch = i;
            return true;
        }

        // 递归检查下一个消息
        if (branch.nextMessage && selectBranchPathToTarget(branch.nextMessage, targetCheckpointId)) {
            // 如果下级消息中找到了目标，设置当前分支
            message.currentBranch = i;
            return true;
        }
    }

    return false;
};

// 从 ThreadItem 数组创建 Message（处理分支）
export const createMessageFromThreadItems = (items: ThreadItem[]): Message => {
    const branches: MessageBranch[] = [];

    for (const item of items) {
        const parts: Part[] = [];

        // 转换内容为 Part 数组
        for (const content of item.content) {
            switch (content.type) {
                case 'userEvent':
                    if (content.content) {
                        parts.push({
                            type: 'text',
                            content: content.content
                        });
                    }
                    break;
                case 'assistantContent':
                    if (content.data?.type === 'text' && content.data.content) {
                        parts.push({
                            type: 'text',
                            content: content.data.content
                        });
                    } else if (content.data?.type === 'toolCall') {
                        parts.push({
                            type: 'toolCall',
                            toolName: content.data.name || '',
                            parameters: content.data.args || {},
                            toolCallId: content.data.id
                        });
                    }
                    break;
                case 'toolResult':
                    parts.push({
                        type: 'toolCallResponse',
                        data: content.content || '',
                        toolCallId: content.callId
                    });
                    break;
                default:
                    console.warn('未知的内容类型:', content.type);
                    break;
            }
        }

        // 创建分支
        branches.push({
            id: item.checkpointId,
            role: item.type,
            parts: parts,
        });
    }

    return {
        branches: branches,
        currentBranch: 0 // 默认选择第一个分支
    };
}; 