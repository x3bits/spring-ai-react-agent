import React, { useState } from 'react';
import { useChatStore } from '../store/chatStore';

interface MessageInputProps {
    disabled?: boolean;
}

const MessageInput: React.FC<MessageInputProps> = ({ disabled = false }) => {
    const [message, setMessage] = useState('');
    const { invokeUserMessage: sendMessageToServer, getCurrentChat } = useChatStore();
    const currentChat = getCurrentChat();
    const isModelGenerating = currentChat?.isModelGenerating || false;

    const isSubmitDisabled = disabled || isModelGenerating;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        if (!message.trim() || isSubmitDisabled) return;

        // 调用 store 的 action 发送消息
        sendMessageToServer(message.trim());

        // 清空输入框
        setMessage('');
    };

    return (
        <form onSubmit={handleSubmit} className="p-4 bg-base-100">
            <div className="relative flex items-center">
                <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="输入消息..."
                    className="textarea w-full pr-12 resize-none overflow-y-auto max-h-60 min-h-[60px] focus:outline-none focus:ring-0 bg-base-100 shadow-md"
                    rows={1}
                    disabled={disabled}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault();
                            handleSubmit(e);
                        }
                    }}
                />
                <button
                    type="submit"
                    className="btn btn-sm btn-ghost btn-square absolute right-3 top-1/2 transform -translate-y-1/2 text-primary disabled:text-base-content/30"
                    disabled={!message.trim() || isSubmitDisabled}
                    aria-label="发送消息"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
                        <path d="M3.478 2.405a.75.75 0 00-.926.94l2.432 7.905H13.5a.75.75 0 010 1.5H4.984l-2.432 7.905a.75.75 0 00.926.94 60.519 60.519 0 0018.445-8.986.75.75 0 000-1.218A60.517 60.517 0 003.478 2.405z" />
                    </svg>
                </button>
            </div>
        </form>
    );
};

export default MessageInput; 