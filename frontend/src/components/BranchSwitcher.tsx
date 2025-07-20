import React from 'react';

interface BranchSwitcherProps {
    currentBranch: number;
    totalBranches: number;
    onSwitchBranch: (branchIndex: number) => void;
    disabled?: boolean;
}

const BranchSwitcher: React.FC<BranchSwitcherProps> = ({
    currentBranch,
    totalBranches,
    onSwitchBranch,
    disabled = false
}) => {
    const handlePrevBranch = () => {
        if (disabled) return;
        const prevBranch = (currentBranch - 1 + totalBranches) % totalBranches;
        onSwitchBranch(prevBranch);
    };

    const handleNextBranch = () => {
        if (disabled) return;
        const nextBranch = (currentBranch + 1) % totalBranches;
        onSwitchBranch(nextBranch);
    };

    return totalBranches <= 1 ? (
        <>
        </>
    ) : (
        <div className="flex items-center">
            <button
                onClick={handlePrevBranch}
                disabled={disabled || totalBranches <= 1}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="上一个分支"
                aria-label="上一个分支"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-3 h-3">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
                </svg>
            </button>
            <div className="text-xs text-base-content/60 px-1 min-w-[32px] text-center">
                {currentBranch + 1} / {totalBranches}
            </div>
            <button
                onClick={handleNextBranch}
                disabled={disabled || totalBranches <= 1}
                className="btn btn-xs btn-ghost btn-square text-base-content/60 hover:text-primary transition-colors"
                title="下一个分支"
                aria-label="下一个分支"
            >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-3 h-3">
                    <path strokeLinecap="round" strokeLinejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                </svg>
            </button>
        </div>
    );
};

export default BranchSwitcher; 