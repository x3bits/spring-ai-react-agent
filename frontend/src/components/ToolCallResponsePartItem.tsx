import React from 'react';
import { ToolCallResponse } from '../store/chatStore';

interface ToolCallResponsePartItemProps {
    part: ToolCallResponse;
}

const ToolCallResponsePartItem: React.FC<ToolCallResponsePartItemProps> = ({ part }) => {
    return (
        <div className="p-2 my-2 border border-neutral rounded-md">
            <details className="mt-1" open>
                <summary className="text-sm font-medium text-base-content/80 cursor-pointer hover:text-accent-focus">工具调用结果</summary>
                <pre className="bg-base-300 p-2 mt-1 rounded-md text-xs overflow-x-auto">
                    {part.data}
                </pre>
            </details>
        </div>
    );
};

export default ToolCallResponsePartItem; 