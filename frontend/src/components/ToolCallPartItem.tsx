import React from 'react';
import { ToolCall } from '../store/chatStore';

interface ToolCallPartItemProps {
    part: ToolCall;
}

const ToolCallPartItem: React.FC<ToolCallPartItemProps> = ({ part }) => {
    return (
        <div className="p-2 my-2 border border-neutral rounded-md">
            <details className="mt-1" open>
                <summary className="text-sm font-medium text-base-content/80 cursor-pointer hover:text-accent-focus">调用工具 {part.toolName}</summary>
                <pre className="bg-base-300 p-2 mt-1 rounded-md text-xs overflow-x-auto">
                    {JSON.stringify(part.parameters, null, 2)}
                </pre>
            </details>
        </div>
    );
};

export default ToolCallPartItem; 