import React, { useState, useEffect, useRef } from 'react';
import { getMessageCurrentBranch, getMessageCurrentContent, getMessageId, Message, useChatStore } from '../store/chatStore';
import TextPartItem from './TextPartItem';
import ToolCallPartItem from './ToolCallPartItem';
import ToolCallResponsePartItem from './ToolCallResponsePartItem';

interface MessageItemProps {
    message: Message;
    isEditing: boolean;
    onStartEdit: () => void;
    onCancelEdit: () => void;
}

const MessageItem: React.FC<MessageItemProps> = ({ message, isEditing, onCancelEdit }) => {
    const [editedContent, setEditedContent] = useState(getMessageCurrentContent(message));
    const { getCurrentChat, editUserMessage } = useChatStore();
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        if (isEditing) {
            setEditedContent(getMessageCurrentContent(message));
            textareaRef.current?.focus();
            textareaRef.current?.setSelectionRange(getMessageCurrentContent(message).length, getMessageCurrentContent(message).length);
        }
    }, [isEditing, message]);

    const handleSave = () => {
        const currentChat = getCurrentChat();
        if (currentChat && editedContent.trim() !== '') {
            editUserMessage(getMessageId(message), editedContent);
        }
        onCancelEdit();
    };

    const handleCancel = () => {
        setEditedContent(getMessageCurrentContent(message));
        onCancelEdit();
    };

    const isUser = getMessageCurrentBranch(message).role === 'user';

    if (isEditing && isUser) {
        return (
            <div className={`py-1 flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                <div className={`rounded-lg py-2 px-3.5 max-w-[80%] shadow-sm w-full break-words bg-base-100 border border-primary`}>
                    <textarea
                        ref={textareaRef}
                        value={editedContent}
                        onChange={(e) => setEditedContent(e.target.value)}
                        className="textarea w-full resize-none overflow-y-auto max-h-40 min-h-[48px] p-0 focus:outline-none focus:ring-0 border-none bg-transparent"
                        rows={3}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                handleSave();
                            }
                            if (e.key === 'Escape') {
                                handleCancel();
                            }
                        }}
                    />
                    <div className="flex justify-end gap-2 mt-2">
                        <button onClick={handleCancel} className="btn btn-sm btn-ghost">
                            取消
                        </button>
                        <button onClick={handleSave} className="btn btn-sm btn-primary">
                            保存
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className={`py-1 flex ${isUser ? 'justify-end' : 'justify-start'}`}>
            <div
                className={`rounded-lg py-2 px-3.5 max-w-[80%] shadow-sm break-words ${isUser
                    ? 'bg-primary text-primary-content'
                    : 'bg-base-200 text-base-content'
                    }`}
            >
                <div className="whitespace-pre-wrap">
                    {getMessageCurrentBranch(message).parts.map((part, index) => {
                        switch (part.type) {
                            case 'text':
                                return <TextPartItem key={index} part={part} />;
                            case 'toolCall':
                                return <ToolCallPartItem key={index} part={part} />;
                            case 'toolCallResponse':
                                return <ToolCallResponsePartItem key={index} part={part} />;
                            default:
                                return null;
                        }
                    })}
                </div>
            </div>
        </div>
    );
};

export default MessageItem; 