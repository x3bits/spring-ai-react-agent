import React from 'react';
import { useChatStore } from '../store/chatStore';

const AgentSelector: React.FC = () => {
    const {
        selectedAgent,
        availableAgents,
        setSelectedAgent
    } = useChatStore();

    const handleAgentChange = async (event: React.ChangeEvent<HTMLSelectElement>) => {
        const agentName = event.target.value || null;
        try {
            await setSelectedAgent(agentName);
        } catch (error) {
            console.error('切换agent失败:', error);
        }
    };

    if (availableAgents.length === 0) {
        return (
            <div className="flex items-center space-x-2">
                <span className="text-sm text-base-content/70">Agent:</span>
                <span className="text-sm text-base-content/50">加载中...</span>
            </div>
        );
    }

    return (
        <div className="flex items-center space-x-2">
            <span className="text-sm text-base-content/70">Agent:</span>
            <select
                value={selectedAgent || ''}
                onChange={handleAgentChange}
                className="select select-sm select-bordered bg-base-100 text-sm min-w-[120px]"
            >
                {availableAgents.map((agent) => (
                    <option key={agent} value={agent}>
                        {agent}
                    </option>
                ))}
            </select>
        </div>
    );
};

export default AgentSelector;
