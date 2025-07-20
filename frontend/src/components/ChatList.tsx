import React, { useState } from 'react';
import { useChatStore } from '../store/chatStore';

const ChatList: React.FC = () => {
    const { chats, currentChatId, createChat, deleteChat, updateChatTitle, setCurrentChat } = useChatStore();
    const [editingChatId, setEditingChatId] = useState<string | null>(null);
    const [editingTitle, setEditingTitle] = useState<string>('');

    const handleDeleteChat = async (chatId: string, e: React.MouseEvent) => {
        e.stopPropagation();

        try {
            await deleteChat(chatId);
        } catch (error) {
            console.error('删除会话失败:', error);
            // 这里可以添加用户提示，比如 toast 通知
        }
    };

    const handleCreateChat = async () => {
        try {
            await createChat();
        } catch (error) {
            console.error('创建会话失败:', error);
            // 这里可以添加用户提示，比如 toast 通知
        }
    };

    const handleEditTitle = (chatId: string, currentTitle: string, e: React.MouseEvent) => {
        e.stopPropagation();
        setEditingChatId(chatId);
        setEditingTitle(currentTitle);
        const modal = document.getElementById('edit_title_modal') as HTMLDialogElement;
        modal?.showModal();
    };

    const handleSaveTitle = async () => {
        if (!editingChatId || !editingTitle.trim()) return;

        try {
            await updateChatTitle(editingChatId, editingTitle.trim());
            const modal = document.getElementById('edit_title_modal') as HTMLDialogElement;
            modal?.close();
            setEditingChatId(null);
            setEditingTitle('');
        } catch (error) {
            console.error('更新标题失败:', error);
        }
    };

    const handleCancelEdit = () => {
        const modal = document.getElementById('edit_title_modal') as HTMLDialogElement;
        modal?.close();
        setEditingChatId(null);
        setEditingTitle('');
    };

    return (
        <div className="flex flex-col h-full bg-base-100">
            <div className="p-4 h-[60px] flex items-center">
                <button
                    onClick={handleCreateChat}
                    className="flex items-center gap-2 text-base-content hover:text-primary transition-colors"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                    </svg>
                    新建对话
                </button>
            </div>

            <div className="flex-1 overflow-y-auto">
                {chats.map(chat => (
                    <div
                        key={chat.id}
                        className={`flex items-center justify-between px-4 py-3 cursor-pointer transition-colors group ${chat.id === currentChatId ? 'bg-base-200' : 'hover:bg-base-200'
                            }`}
                        onClick={() => setCurrentChat(chat.id)}
                    >
                        <div className="flex-1 truncate text-sm">{chat.title}</div>
                        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            <button
                                onClick={(e) => handleEditTitle(chat.id, chat.title, e)}
                                className="text-base-content/40 hover:text-info transition-colors p-1 rounded-full hover:bg-base-300"
                                aria-label="编辑标题"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L10.582 16.07a4.5 4.5 0 0 1-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 0 1 1.13-1.897l8.932-8.931Zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0 1 15.75 21H5.25A2.25 2.25 0 0 1 3 18.75V8.25A2.25 2.25 0 0 1 5.25 6H10" />
                                </svg>
                            </button>
                            <button
                                onClick={(e) => handleDeleteChat(chat.id, e)}
                                className="text-base-content/40 hover:text-error transition-colors p-1 rounded-full hover:bg-base-300"
                                aria-label="删除聊天"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                                </svg>
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* 编辑标题模态框 */}
            <dialog id="edit_title_modal" className="modal">
                <div className="modal-box">
                    <h3 className="font-bold text-lg">编辑对话标题</h3>
                    <div className="py-4">
                        <input
                            type="text"
                            className="input input-bordered w-full"
                            placeholder="请输入对话标题"
                            value={editingTitle}
                            onChange={(e) => setEditingTitle(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    handleSaveTitle();
                                } else if (e.key === 'Escape') {
                                    handleCancelEdit();
                                }
                            }}
                        />
                    </div>
                    <div className="modal-action">
                        <button
                            className="btn btn-ghost"
                            onClick={handleCancelEdit}
                        >
                            取消
                        </button>
                        <button
                            className="btn btn-primary"
                            onClick={handleSaveTitle}
                            disabled={!editingTitle.trim()}
                        >
                            保存
                        </button>
                    </div>
                </div>
                <form method="dialog" className="modal-backdrop">
                    <button onClick={handleCancelEdit}>关闭</button>
                </form>
            </dialog>
        </div>
    );
};

export default ChatList; 