import React, { useEffect } from 'react';
import ChatList from './ChatList';
import MessageThread from './MessageThread';
import MessageInput from './MessageInput';
import AgentSelector from './AgentSelector';
import { useChatStore } from '../store/chatStore';

const ChatLayout: React.FC = () => {
    const { chats, loadChatsFromServer, getCurrentChat, getCurrentVisibleMessages, isLoading, loadAvailableAgents, selectedAgent } = useChatStore();

    const currentChat = getCurrentChat();

    // 页面加载时先加载agent列表，然后加载会话列表
    useEffect(() => {
        const initializeApp = async () => {
            if (chats.length === 0 && !isLoading && selectedAgent === null) {
                // 先加载agent列表
                await loadAvailableAgents();
                // 然后加载会话列表（此时selectedAgent已经被设置）
                await loadChatsFromServer();
            }
        };

        initializeApp();
    }, [chats.length, loadChatsFromServer, loadAvailableAgents, isLoading, selectedAgent]);

    // 如果正在加载，显示加载状态
    if (isLoading) {
        return (
            <div className="h-screen flex items-center justify-center bg-base-100">
                <div className="flex flex-col items-center gap-4">
                    <div className="loading loading-spinner loading-lg"></div>
                    <div className="text-base-content/70">正在加载会话...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="h-screen flex flex-col md:flex-row bg-base-100">
            {/* 左侧聊天列表 - 仅在md及以上屏幕显示 */}
            <div className="w-full md:w-[280px] h-auto md:h-full">
                <ChatList />
            </div>

            {/* 右侧聊天内容 */}
            <div className="flex-1 flex flex-col h-full overflow-hidden">
                {/* 顶部工具栏 */}
                <div className="flex justify-end items-center p-4 border-b border-base-300">
                    <AgentSelector />
                </div>

                {currentChat ? (
                    <>
                        <div className="flex-1 overflow-hidden">
                            <MessageThread
                                messages={getCurrentVisibleMessages()}
                            />
                        </div>
                        <MessageInput />
                    </>
                ) : (
                    <div className="flex-1 flex items-center justify-center text-base-content/50">
                        选择或创建新的对话
                    </div>
                )}
            </div>
        </div>
    );
};

export default ChatLayout; 