import React, { Fragment, useRef, useEffect, useState } from 'react';
import { Message, getMessageCurrentBranch, getMessageCurrentContent, useChatStore, getMessageId } from '../store/chatStore';
import MessageItem from './MessageItem';
import AssistantMessageActions from './AssistantMessageActions';
import UserMessageActions from './UserMessageActions';

interface MessageThreadProps {
    messages: Message[];
}

const MessageThread: React.FC<MessageThreadProps> = ({
    messages
}) => {
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
    const { switchBranch, getCurrentChat, replayAssistantMessage } = useChatStore();
    const currentChat = getCurrentChat();
    const isModelGenerating = currentChat?.isModelGenerating || false;

    // 自动滚动到底部
    useEffect(() => {
        // Scroll to bottom, but only if not currently editing a message
        // to prevent view jumping while typing in textarea.
        if (!editingMessageId) {
            messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages, editingMessageId]);

    const handleStartEdit = (messageId: string) => {
        setEditingMessageId(messageId);
    };

    const handleCancelEdit = () => {
        setEditingMessageId(null);
    };

    const handleSwitchBranch = (messageId: string, branchIndex: number) => {
        const currentChat = getCurrentChat();
        if (currentChat) {
            switchBranch(currentChat.id, messageId, branchIndex);
        }
    };

    return (
        <div className="flex flex-col h-full bg-base-100 relative">
            {/* 消息列表 */}
            <div className={`flex-1 overflow-y-auto px-4 pt-4 pb-4`}>
                {messages.length === 0 ? (
                    <div className="flex h-full items-center justify-center text-base-content/50">
                        开始新的对话
                    </div>
                ) : (
                    messages.map((message) => (
                        <Fragment key={getMessageId(message)}>
                            <MessageItem
                                message={message}
                                isEditing={editingMessageId === getMessageId(message)}
                                onStartEdit={() => handleStartEdit(getMessageId(message))}
                                onCancelEdit={handleCancelEdit}
                            />
                            {/* Action buttons are only shown when NOT editing that specific message */}
                            {editingMessageId !== getMessageId(message) && (
                                <>
                                    {getMessageCurrentBranch(message).role === 'assistant' && (
                                        <AssistantMessageActions
                                            messageContent={getMessageCurrentContent(message)}
                                            onRefresh={() => {
                                                replayAssistantMessage(getMessageId(message));
                                            }}
                                            currentBranch={message.currentBranch}
                                            totalBranches={message.branches.length}
                                            onSwitchBranch={(branchIndex) => handleSwitchBranch(getMessageId(message), branchIndex)}
                                            disabled={isModelGenerating}
                                        />
                                    )}
                                    {getMessageCurrentBranch(message).role === 'user' && (
                                        <UserMessageActions
                                            messageContent={getMessageCurrentContent(message)}
                                            onEdit={() => handleStartEdit(getMessageId(message))}
                                            currentBranch={message.currentBranch}
                                            totalBranches={message.branches.length}
                                            onSwitchBranch={(branchIndex) => handleSwitchBranch(getMessageId(message), branchIndex)}
                                            disabled={isModelGenerating}
                                        />
                                    )}
                                </>
                            )}
                        </Fragment>
                    ))
                )}
                <div ref={messagesEndRef} />
            </div>
        </div>
    );
};

export default MessageThread; 