import React from 'react';
import BranchSwitcher from './BranchSwitcher';

interface UserMessageActionsProps {
    messageContent: string;
    onEdit: () => void;
    currentBranch: number;
    totalBranches: number;
    onSwitchBranch: (branchIndex: number) => void;
    disabled?: boolean;
}

const UserMessageActions: React.FC<UserMessageActionsProps> = ({
    messageContent,
    onEdit,
    currentBranch,
    totalBranches,
    onSwitchBranch,
    disabled = false
}) => {
    const handleCopy = () => {
        if (disabled) return;
        navigator.clipboard.writeText(messageContent);
    };

    const handleEdit = () => {
        if (disabled) return;
        onEdit();
    };

    return (
        <div className="flex items-center justify-end gap-1 pt-1 pb-1 pr-0 bg-base-100">

            <button
                onClick={handleCopy}
                disabled={disabled}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="复制消息"
                aria-label="复制消息"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75" />
                </svg>
            </button>
            <button
                onClick={handleEdit}
                disabled={disabled}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="编辑消息"
                aria-label="编辑消息"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                </svg>
            </button>
            <BranchSwitcher
                currentBranch={currentBranch}
                totalBranches={totalBranches}
                onSwitchBranch={onSwitchBranch}
                disabled={disabled}
            />
        </div>
    );
};

export default UserMessageActions; 