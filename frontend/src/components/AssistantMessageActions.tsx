import React from 'react';
import BranchSwitcher from './BranchSwitcher';

interface AssistantMessageActionsProps {
    messageContent: string;
    onRefresh: () => void;
    currentBranch: number;
    totalBranches: number;
    onSwitchBranch: (branchIndex: number) => void;
    disabled?: boolean;
}

const AssistantMessageActions: React.FC<AssistantMessageActionsProps> = ({
    messageContent,
    onRefresh,
    currentBranch,
    totalBranches,
    onSwitchBranch,
    disabled = false
}) => {
    const handleCopy = () => {
        if (disabled) return;
        navigator.clipboard.writeText(messageContent);
    };

    const handleRefresh = () => {
        if (disabled) return;
        onRefresh();
    };

    return (
        <div className="flex items-center justify-start gap-2 pt-1 pb-1 pl-2 bg-base-100">
            <BranchSwitcher
                currentBranch={currentBranch}
                totalBranches={totalBranches}
                onSwitchBranch={onSwitchBranch}
                disabled={disabled}
            />

            <button
                onClick={handleCopy}
                disabled={disabled}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="复制回复"
                aria-label="复制回复"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75" />
                </svg>
            </button>

            <button
                onClick={handleRefresh}
                disabled={disabled}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="重新生成回复"
                aria-label="重新生成回复"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
            </button>
        </div>
    );
};

export default AssistantMessageActions; 