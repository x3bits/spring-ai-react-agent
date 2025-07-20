import { create } from 'zustand';
import { agentService, AgentEvent, ThreadInfo } from '../api/agentService';
import { convertThreadItemsToMessages } from '../utils/threadConverter';

export type MessageRole = 'user' | 'assistant';

export interface TextPart {
    type: 'text';
    content: string;
}

export interface ToolCall {
    type: 'toolCall';
    toolName: string;
    parameters: Record<string, unknown>;
    toolCallId?: string;
}

export interface ToolCallResponse {
    type: 'toolCallResponse';
    data: string;
    toolCallId?: string;
}

export type Part = TextPart | ToolCall | ToolCallResponse;

export interface Message {
    branches: MessageBranch[];
    currentBranch: number;
}

export interface MessageBranch {
    id: string;
    role: MessageRole;
    parts: Part[];
    nextMessage?: Message;
}

export const getMessageCurrentBranch = (message: Message): MessageBranch => {
    return message.branches[message.currentBranch];
}

export const getMessageCurrentContent = (message: Message): string => {
    return getMessageCurrentBranch(message).parts
        .filter((part): part is TextPart => part.type === 'text')
        .slice(-1)[0]?.content ?? ''
}

export const singleTextPart = (content: string): Part[] => {
    return [{
        type: 'text',
        content: content
    }]
}

export const getMessageId = (message: Message): string => {
    return message.branches[message.currentBranch]?.id ?? "";
}

export interface Chat {
    id: string;
    title: string;
    threadId?: string;
    rootCheckpointId?: string;
    rootMessage?: Message;
    lastMessage?: Message;
    isContentLoaded?: boolean;
    isModelGenerating?: boolean;
}

interface ChatState {
    chats: Chat[];
    currentChatId: string | null;
    isLoading: boolean;
    selectedAgent: string | null;
    availableAgents: string[];
    loadChatsFromServer: () => Promise<void>;
    createChat: (title?: string) => Promise<void>;
    deleteChat: (chatId: string) => Promise<void>;
    updateChatTitle: (chatId: string, newTitle: string) => Promise<void>;
    setCurrentChat: (chatId: string) => void;
    getCurrentChat: () => Chat | null;
    getCurrentVisibleMessages: () => Message[];
    switchBranch: (chatId: string, messageId: string, branchId: number) => void;
    invokeUserMessage: (message: string) => Promise<void>;
    replayAssistantMessage: (messageId: string) => Promise<void>;
    editUserMessage: (messageId: string, userMessage: string) => Promise<void>;
    setSelectedAgent: (agentName: string | null) => Promise<void>;
    loadAvailableAgents: () => Promise<void>;
}

const generateId = () => {
    return Math.random().toString(36).substring(2, 15) +
        Math.random().toString(36).substring(2, 15);
};

const createEmptyChat = (threadInfo?: ThreadInfo): Chat => {
    return {
        id: threadInfo?.threadId || generateId(),
        title: threadInfo?.title || "新对话",
        threadId: threadInfo?.threadId,
        rootCheckpointId: "",
        isContentLoaded: false,
        isModelGenerating: false,
    };
};

const findLastMessage = (fromMessage: Message): Message => {
    let message = fromMessage;
    while (true) {
        const nextMessage = message.branches[message.currentBranch].nextMessage;
        if (!nextMessage) {
            return message;
        }
        message = nextMessage;
    }
}

const findMessageById = (chat: Chat, messageId: string): Message | null => {
    let message = chat.rootMessage;
    while (message) {
        if (getMessageId(message) === messageId) {
            return message;
        }
        message = message.branches[message.currentBranch].nextMessage;
    }
    return null;
}

const isChatEmpty = (chat: Chat): boolean => {
    return !chat.rootMessage;
};

export const useChatStore = create<ChatState>((set, get) => {
    // 内部函数定义
    const loadChatContent = async (chatId: string) => {
        const chat = get().chats.find(c => c.id === chatId);
        if (!chat || !chat.threadId || chat.isContentLoaded) {
            return;
        }

        try {
            const selectedAgent = get().selectedAgent;
            const threadItems = await agentService.getThreadItems(chat.threadId, selectedAgent || undefined);
            const { rootMessage, lastMessage, rootCheckpointId } = convertThreadItemsToMessages(threadItems);

            set((state) => ({
                chats: state.chats.map(c =>
                    c.id === chatId
                        ? {
                            ...c,
                            rootMessage,
                            lastMessage,
                            rootCheckpointId,
                            isContentLoaded: true,
                            isModelGenerating: c.isModelGenerating || false
                        }
                        : c
                )
            }));
        } catch (error) {
            console.error(`加载会话 ${chatId} 的内容失败:`, error);
        }
    };

    const addMessage = (chatId: string, branch: MessageBranch) => {
        set((state) => {
            const newChats = state.chats.map((chat) => {
                if (chat.id !== chatId) return chat;

                const newMessage = {
                    branches: [branch],
                    currentBranch: 0
                };

                const newRootMessage = chat.rootMessage ? chat.rootMessage : newMessage;
                if (chat.lastMessage) {
                    chat.lastMessage.branches[chat.lastMessage.currentBranch].nextMessage = newMessage;
                }

                return { ...chat, rootMessage: newRootMessage, lastMessage: newMessage };
            });

            return { chats: newChats };
        });
    };

    const addBranch = (chatId: string, messageId: string, branch: MessageBranch) => {
        set((state) => {
            const newChats = state.chats.map((chat) => {
                if (chat.id !== chatId) return chat;

                const message = findMessageById(chat, messageId);
                if (!message) {
                    throw new Error(`[Bug] Message with id ${messageId} not found in chat ${chatId}`);
                }

                message.branches.push(branch);
                message.currentBranch = message.branches.length - 1;

                return { ...chat, lastMessage: message };
            });
            return { chats: newChats };
        });
    };

    const appendTextPart = (chatId: string, text: string) => {
        set((state) => {
            const chat = state.chats.find(chat => chat.id === chatId);
            if (!chat || !chat.lastMessage) return state;

            const currentBranch = chat.lastMessage.branches[chat.lastMessage.currentBranch];
            const lastPart = currentBranch.parts[currentBranch.parts.length - 1];

            if (lastPart?.type === 'text') {
                lastPart.content += text;
            } else {
                currentBranch.parts.push({
                    type: 'text',
                    content: text
                });
            }

            return { chats: [...state.chats] };
        });
    };

    const addPart = (chatId: string, part: Part) => {
        set((state) => {
            const chat = state.chats.find(chat => chat.id === chatId);
            if (!chat || !chat.lastMessage) return state;

            const currentBranch = chat.lastMessage.branches[chat.lastMessage.currentBranch];
            currentBranch.parts.push(part);

            return { chats: [...state.chats] };
        });
    };

    const changeLastMessageId = (chatId: string, messageId: string) => {
        set((state) => {
            const chat = state.chats.find(chat => chat.id === chatId);
            if (!chat || !chat.lastMessage) return state;

            chat.lastMessage.branches[chat.lastMessage.currentBranch].id = messageId;

            return { chats: [...state.chats] };
        });
    };

    const clearBranchParts = (chatId: string, messageId: string) => {
        set((state) => {
            const chat = state.chats.find(chat => chat.id === chatId);
            if (!chat) return state;
            const message = findMessageById(chat, messageId);
            if (!message) return state;
            message.branches[message.currentBranch].parts = [];

            return { chats: [...state.chats] };
        });
    };

    const setModelGenerating = (chatId: string, isGenerating: boolean) => {
        set((state) => ({
            chats: state.chats.map(c =>
                c.id === chatId
                    ? { ...c, isModelGenerating: isGenerating }
                    : c
            )
        }));
    };

    const getReplayCheckpointId = (messageId: string) => {
        const currentChat = get().getCurrentChat();
        if (!currentChat) return null;
        const rootMessage = currentChat.rootMessage;
        if (!rootMessage) return null;

        if (getMessageId(rootMessage) === messageId) {
            return currentChat.rootCheckpointId ?? null;
        }
        let message: Message | undefined = rootMessage;
        while (message) {
            const currentBranch: MessageBranch = message.branches[message.currentBranch];
            const nextMessage = currentBranch.nextMessage;
            if (nextMessage && getMessageId(nextMessage) === messageId) {
                return currentBranch.id;
            }
            message = nextMessage;
        }
        return null;
    };

    const handleAgentEvent = (chatId: string, event: AgentEvent) => {
        console.log("handleAgentEvent", event);

        switch (event.type) {
            case 'idBeforeInvoke':
                if (!get().getCurrentChat()?.rootCheckpointId) {
                    set((state) => {
                        const chat = state.chats.find(chat => chat.id === chatId);
                        if (!chat) return state;

                        chat.rootCheckpointId = event.id;
                        return { chats: [...state.chats] };
                    });
                }
                break;

            case 'userEventId':
                if (event.id) {
                    changeLastMessageId(chatId, event.id);
                }
                break;

            case 'assistantPartialText':

                const partialTextEvent = event;
                const chat = get().chats.find(chat => chat.id === chatId);
                if (chat && (!chat.lastMessage ||
                    chat.lastMessage.branches[chat.lastMessage.currentBranch].role !== 'assistant' ||
                    chat.lastMessage.branches[chat.lastMessage.currentBranch].id !== "")) {
                    addMessage(chatId, {
                        id: "",
                        parts: [],
                        role: 'assistant',
                    });
                }

                appendTextPart(chatId, partialTextEvent.text);
                break;

            case 'assistantStart':
                setModelGenerating(chatId, true);
                const chat1 = get().chats.find(chat => chat.id === chatId);
                if (chat1 && chat1.lastMessage && chat1.lastMessage.branches[chat1.lastMessage.currentBranch].id === "") {
                    changeLastMessageId(chatId, event.id);
                    clearBranchParts(chatId, event.id);
                } else {
                    addMessage(chatId, {
                        id: event.id,
                        parts: [],
                        role: 'assistant',
                    });
                }
                break;

            case 'assistantContent':
                const assistantContentEvent = event;

                if (assistantContentEvent.data.type === 'toolCall') {
                    addPart(chatId, {
                        type: 'toolCall',
                        toolName: assistantContentEvent.data.name || '',
                        parameters: assistantContentEvent.data.args || {},
                        toolCallId: assistantContentEvent.data.id
                    });
                } else if (assistantContentEvent.data.type === 'text') {
                    addPart(chatId, {
                        type: 'text',
                        content: assistantContentEvent.data.content || ''
                    });
                }
                break;

            case 'toolResult':
                const toolResultEvent = event;
                const chat2 = get().chats.find(chat => chat.id === chatId);
                if (chat2 && chat2.lastMessage && chat2.lastMessage.branches[chat2.lastMessage.currentBranch].id === "") {
                    changeLastMessageId(chatId, event.id);
                    clearBranchParts(chatId, event.id);
                } else {
                    addMessage(chatId, {
                        id: toolResultEvent.id,
                        parts: [],
                        role: 'assistant',
                    });
                }
                addPart(chatId, {
                    type: 'toolCallResponse',
                    data: toolResultEvent.content,
                    toolCallId: toolResultEvent.callId
                });
                break;

            default:
                console.warn('Unknown event type:', event);
                break;
        }
    };

    return {
        chats: [],
        currentChatId: null,
        isLoading: false,
        selectedAgent: null,
        availableAgents: [],

        createChat: async (title?: string) => {
            try {
                set((state) => ({ ...state, isLoading: true }));

                // 优化逻辑：如果当前对话列表中第一个对话为空，则跳转到第一个对话而不是创建新对话
                const currentChats = get().chats;
                if (currentChats.length > 0) {
                    const firstChat = currentChats[0];
                    if (isChatEmpty(firstChat)) {
                        // 第一个对话为空，跳转到第一个对话
                        get().setCurrentChat(firstChat.id);
                        return;
                    }
                }

                const selectedAgent = get().selectedAgent;

                if (title) {
                    // 如果提供了标题，从服务端创建会话
                    const response = await agentService.createThread(title, selectedAgent || undefined);
                    const newChat = createEmptyChat({
                        userId: 'guest',
                        threadId: response.threadId,
                        title: title
                    });
                    set((state) => ({
                        chats: [newChat, ...state.chats],
                        currentChatId: newChat.id
                    }));
                } else {
                    // 如果没有提供标题，创建一个默认标题的会话
                    const response = await agentService.createThread('新对话', selectedAgent || undefined);
                    const newChat = createEmptyChat({
                        userId: 'guest',
                        threadId: response.threadId,
                        title: '新对话'
                    });
                    set((state) => ({
                        chats: [newChat, ...state.chats],
                        currentChatId: newChat.id
                    }));
                }
            } catch (error) {
                console.error('创建会话失败:', error);
                // 失败时创建本地会话
                const newChat = createEmptyChat();
                set((state) => ({
                    chats: [newChat, ...state.chats],
                    currentChatId: newChat.id
                }));
            } finally {
                set((state) => ({ ...state, isLoading: false }));
            }
        },

        deleteChat: async (chatId) => {
            try {
                set((state) => ({ ...state, isLoading: true }));

                // 找到要删除的会话
                const chatToDelete = get().chats.find(chat => chat.id === chatId);
                if (chatToDelete?.threadId) {
                    // 如果有 threadId，调用后端删除接口
                    await agentService.deleteThread(chatToDelete.threadId);
                }

                // 后端删除成功后，更新前端状态
                set((state) => {
                    const newChats = state.chats.filter((chat) => chat.id !== chatId);
                    const newCurrentChatId = state.currentChatId === chatId
                        ? (newChats.length > 0 ? newChats[0].id : null)
                        : state.currentChatId;

                    return {
                        chats: newChats,
                        currentChatId: newCurrentChatId
                    };
                });
            } catch (error) {
                console.error('删除会话失败:', error);
                // 可以考虑显示错误提示给用户
            } finally {
                set((state) => ({ ...state, isLoading: false }));
            }
        },

        setCurrentChat: (chatId) => {
            set({ currentChatId: chatId });

            // 当切换到新的会话时，自动加载其内容
            const chat = get().chats.find(c => c.id === chatId);
            if (chat && !chat.isContentLoaded) {
                loadChatContent(chatId);
            }
        },

        getCurrentChat: () => {
            const { chats, currentChatId } = get();
            if (!currentChatId) return null;
            return chats.find(chat => chat.id === currentChatId) || null;
        },

        getCurrentVisibleMessages: () => {
            const chat = get().getCurrentChat();
            if (!chat) return [];
            const result: Message[] = [];
            let message = chat.rootMessage;
            while (message) {
                result.push(message);
                message = message.branches[message.currentBranch].nextMessage;
            }
            return result;
        },

        switchBranch: (chatId: string, messageId: string, branchId: number) => {
            set((state) => {
                const newChats = state.chats.map((chat) => {
                    if (chat.id !== chatId) return chat;

                    const message = findMessageById(chat, messageId);
                    if (!message) {
                        throw new Error(`[Bug] Message with id ${messageId} not found in chat ${chatId}`);
                    }

                    if (branchId < 0 || branchId >= message.branches.length) {
                        throw new Error(`[Bug] Branch with id ${branchId} not found in message ${messageId} of chat ${chatId}`);
                    }

                    if (message.currentBranch === branchId) {
                        return chat;
                    }

                    message.currentBranch = branchId;

                    const lastMessage = findLastMessage(message);

                    return { ...chat, lastMessage: lastMessage };
                });
                return { chats: newChats };
            });
        },

        invokeUserMessage: async (message: string) => {
            const currentChat = get().getCurrentChat();
            if (!currentChat) return;

            setModelGenerating(currentChat.id, true);

            // 使用 threadId，如果没有则创建新的会话
            const threadId = currentChat.threadId;
            if (!threadId) {
                console.error('当前会话没有 threadId，无法发送消息');
                return;
            }

            const checkpointId = currentChat.lastMessage ? getMessageId(currentChat.lastMessage) : undefined;

            // 检测是否是第一条用户消息
            const isFirstUserMessage = !currentChat.rootMessage;

            // 如果是第一条用户消息，更新Thread标题
            if (isFirstUserMessage) {
                try {
                    const maxTitleLength = 20;
                    // 使用消息的前30个字符作为标题，如果消息太短则使用完整消息
                    const title = message.trim().length > maxTitleLength
                        ? message.trim().substring(0, maxTitleLength) + '...'
                        : message.trim();

                    // 更新后端标题
                    await agentService.updateThreadTitle(threadId, title);

                    // 更新本地Chat标题
                    set((state) => {
                        const chat = state.chats.find(chat => chat.id === currentChat.id);
                        if (chat) {
                            chat.title = title;
                        }
                        return { chats: [...state.chats] };
                    });
                } catch (error) {
                    console.error('更新Thread标题失败:', error);
                    // 即使更新标题失败，也继续发送消息
                }
            }

            // 添加用户消息
            addMessage(currentChat.id, {
                id: "",
                parts: singleTextPart(message.trim()),
                role: 'user',
            });

            // 订阅事件流，使用 threadId
            const selectedAgent = get().selectedAgent;
            agentService.runAgentStream(threadId, message, checkpointId, selectedAgent || undefined).subscribe({
                next: (event) => handleAgentEvent(currentChat.id, event),
                error: (error) => {
                    console.error("Error:", error);
                    setModelGenerating(currentChat.id, false);
                },
                complete: () => {
                    console.log("Run complete");
                    setModelGenerating(currentChat.id, false);
                },
            });
        },

        replayAssistantMessage: async (messageId: string) => {
            const currentChat = get().getCurrentChat();
            if (!currentChat) return;

            setModelGenerating(currentChat.id, true);

            const threadId = currentChat.threadId;
            if (!threadId) {
                console.error('当前会话没有 threadId，无法重放消息');
                return;
            }

            const checkpointId = getReplayCheckpointId(messageId);
            if (!checkpointId) {
                throw new Error(`[Bug] Message with id ${messageId} does not have a checkpoint in chat ${currentChat.id}`);
            }

            addBranch(currentChat.id, messageId, {
                id: "",
                parts: [],
                role: 'assistant',
            });

            const selectedAgent = get().selectedAgent;
            agentService.runAgentStream(threadId, undefined, checkpointId, selectedAgent || undefined).subscribe({
                next: (event) => handleAgentEvent(currentChat.id, event),
                error: (error) => {
                    console.error("Error:", error);
                    setModelGenerating(currentChat.id, false);
                },
                complete: () => {
                    console.log("Replay complete");
                    setModelGenerating(currentChat.id, false);
                },
            });
        },

        editUserMessage: async (messageId: string, userMessage: string) => {
            const currentChat = get().getCurrentChat();
            if (!currentChat) return;

            setModelGenerating(currentChat.id, true);

            const threadId = currentChat.threadId;
            if (!threadId) {
                console.error('当前会话没有 threadId，无法编辑消息');
                return;
            }

            const checkpointId = getReplayCheckpointId(messageId);
            if (checkpointId === null || checkpointId === undefined) {
                throw new Error(`[Bug] Message with id ${messageId} does not have a checkpoint in chat ${currentChat.id}`);
            }

            addBranch(currentChat.id, messageId, {
                id: "",
                parts: singleTextPart(userMessage),
                role: 'user',
            });

            const selectedAgent = get().selectedAgent;
            agentService.runAgentStream(threadId, userMessage, checkpointId, selectedAgent || undefined).subscribe({
                next: (event) => handleAgentEvent(currentChat.id, event),
                error: (error) => {
                    console.error("Error:", error);
                    setModelGenerating(currentChat.id, false);
                },
                complete: () => {
                    console.log("Replay complete");
                    setModelGenerating(currentChat.id, false);
                },
            });
        },

        loadChatsFromServer: async () => {
            try {
                set(() => ({ isLoading: true }));

                const selectedAgent = get().selectedAgent;
                const threadList = await agentService.getThreadList(selectedAgent || undefined);

                if (threadList.length === 0) {
                    // 如果没有历史对话，先清空chat列表，然后创建一个新对话
                    set(() => ({
                        chats: [],
                        currentChatId: null
                    }));
                    await get().createChat();
                } else {
                    // 将服务端的 thread 转换为 Chat 对象
                    const chats = threadList.map(threadInfo => createEmptyChat(threadInfo));

                    set(() => ({
                        chats: chats,
                        currentChatId: chats[0].id, // 切换到最新的（第一个）对话
                    }));

                    // 加载最新对话的内容
                    await loadChatContent(chats[0].id);
                }
            } catch (error) {
                console.error('从服务端加载会话列表失败:', error);
                // 如果失败，创建一个本地会话
                const newChat = createEmptyChat();
                set(() => ({
                    chats: [newChat],
                    currentChatId: newChat.id
                }));
            } finally {
                set(() => ({ isLoading: false }));
            }
        },

        updateChatTitle: async (chatId: string, newTitle: string) => {
            try {
                set((state) => ({ ...state, isLoading: true }));

                const chat = get().chats.find(c => c.id === chatId);
                if (!chat) {
                    throw new Error(`[Bug] Chat with id ${chatId} not found`);
                }

                // 更新后端标题
                const threadId = chat.threadId;
                if (threadId) {
                    await agentService.updateThreadTitle(threadId, newTitle);
                }

                // 更新本地状态
                set((state) => ({
                    chats: state.chats.map(c =>
                        c.id === chatId ? { ...c, title: newTitle } : c
                    ),
                    isLoading: false
                }));
            } catch (error) {
                console.error('更新对话标题失败:', error);
                set((state) => ({ ...state, isLoading: false }));
            }
        },

        setSelectedAgent: async (agentName: string | null) => {
            const currentAgent = get().selectedAgent;

            // 如果agent没有变化，不需要重新加载
            if (currentAgent === agentName) {
                return;
            }

            set((state) => ({ ...state, selectedAgent: agentName }));

            // 切换agent后重新加载对话列表
            await get().loadChatsFromServer();
        },

        loadAvailableAgents: async () => {
            try {
                const agents = await agentService.getAgentList();
                set((state) => ({
                    ...state,
                    availableAgents: agents,
                    // 如果当前没有选择agent且有可用的agent，选择第一个（但不触发对话列表重新加载）
                    selectedAgent: state.selectedAgent || (agents.length > 0 ? agents[0] : null)
                }));
            } catch (error) {
                console.error('加载可用agent列表失败:', error);
            }
        }
    };
});