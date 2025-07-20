import { Observable, Observer } from 'rxjs';
import { fetchEventSource, EventSourceMessage } from '@microsoft/fetch-event-source';

// Agent API 的基础 URL，从环境变量获取
const AGENT_API_URL = process.env.NEXT_PUBLIC_AGENT_API_URL || "http://localhost:8000/springAiReactAgent/api";

// 新增：线程相关的类型定义
export interface ThreadInfo {
    userId: string;
    threadId: string;
    title: string;
}

export interface CreateThreadRequest {
    title: string;
}

export interface CreateThreadResponse {
    threadId: string;
}

export interface ThreadItem {
    threadId: string;
    checkpointId: string;
    previousCheckpointId: string;
    type: 'user' | 'assistant';
    content: ThreadItemContent[];
}

export interface ThreadItemContent {
    id?: string;
    type: 'userEvent' | 'assistantContent' | 'toolResult';
    content?: string;
    data?: {
        type: 'toolCall' | 'text';
        id?: string;
        name?: string;
        args?: Record<string, unknown>;
        content?: string;
    };
    callId?: string;
}

// 新的事件类型定义，根据后端返回格式更新
export interface BaseAgentEvent {
    id?: string;
    type: string;
}

export interface UserEventIdEvent extends BaseAgentEvent {
    type: 'userEventId';
    id: string;
}

export interface AssistantStartEvent extends BaseAgentEvent {
    type: 'assistantStart';
    id: string;
}

export interface AssistantContentEvent extends BaseAgentEvent {
    type: 'assistantContent';
    data: {
        type: 'toolCall' | 'text';
        id?: string;
        name?: string;
        args?: Record<string, unknown>;
        content?: string;
    };
}

export interface ToolResultEvent extends BaseAgentEvent {
    type: 'toolResult';
    id: string;
    callId: string;
    content: string;
}

export interface AssistantPartialTextEvent extends BaseAgentEvent {
    type: 'assistantPartialText';
    text: string;
}

export interface IdBeforeInvokeEvent extends BaseAgentEvent {
    type: 'idBeforeInvoke';
    id: string;
}

export type AgentEvent = UserEventIdEvent | AssistantStartEvent | AssistantContentEvent | ToolResultEvent | AssistantPartialTextEvent | IdBeforeInvokeEvent;

export interface ChatStreamRequest {
    threadId: string;
    userMessage: string | undefined;
    checkpointId: string | undefined;
    agentBeanName?: string;
}

export interface ChatReplayRequest {
    threadId: string;
    checkpointId: string;
    agentBeanName?: string;
}

export interface AgentListResponse {
    agents: string[];
}

export interface UpdateThreadTitleRequest {
    threadId: string;
    title: string;
}

// 自定义错误类型
class RetriableError extends Error {
    constructor(message?: string) {
        super(message);
        this.name = 'RetriableError';
    }
}

class FatalError extends Error {
    constructor(message?: string) {
        super(message);
        this.name = 'FatalError';
    }
}

export class AgentService {
    private static instance: AgentService;

    private constructor() { }

    public static getInstance(): AgentService {
        if (!AgentService.instance) {
            AgentService.instance = new AgentService();
        }
        return AgentService.instance;
    }

    /**
     * 构建API URL的辅助方法，支持相对路径和绝对路径
     * @param endpoint API端点
     * @param params 查询参数
     * @returns 完整的URL字符串
     */
    private buildApiUrl(endpoint: string, params?: Record<string, string>): string {
        let url: string;

        // 如果AGENT_API_URL是相对路径，直接拼接
        if (AGENT_API_URL.startsWith('/')) {
            url = `${AGENT_API_URL}${endpoint}`;
        } else {
            // 如果是绝对路径，确保正确拼接路径
            // 移除 AGENT_API_URL 末尾的斜杠（如果有的话）
            const baseUrl = AGENT_API_URL.endsWith('/') ? AGENT_API_URL.slice(0, -1) : AGENT_API_URL;
            // 确保 endpoint 以斜杠开头
            const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
            url = `${baseUrl}${normalizedEndpoint}`;
        }

        // 添加查询参数
        if (params && Object.keys(params).length > 0) {
            const searchParams = new URLSearchParams(params);
            url += `?${searchParams.toString()}`;
        }

        return url;
    }

    /**
     * 创建事件流的通用方法
     * @param endpoint API端点
     * @param requestBody 请求体
     * @returns Observable<AgentEvent>
     */
    private createEventStream(endpoint: string, requestBody: ChatStreamRequest | ChatReplayRequest): Observable<AgentEvent> {
        return new Observable<AgentEvent>((observer: Observer<AgentEvent>) => {
            // 创建 AbortController 用于取消请求
            const abortController = new AbortController();

            // 使用 fetchEventSource 处理 SSE 流
            fetchEventSource(`${AGENT_API_URL}${endpoint}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                },
                body: JSON.stringify(requestBody),
                signal: abortController.signal,

                // 连接打开时的回调
                async onopen(response) {
                    if (response.ok && response.headers.get('content-type')?.includes('text/event-stream')) {
                        return; // 一切正常
                    } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
                        // 4xx 错误通常不可重试
                        throw new FatalError(`HTTP ${response.status}: ${response.statusText}`);
                    } else {
                        // 其他错误可以重试
                        throw new RetriableError(`HTTP ${response.status}: ${response.statusText}`);
                    }
                },

                // 接收到消息时的回调
                onmessage(event: EventSourceMessage) {
                    try {
                        // 解析事件数据
                        const agentEvent: AgentEvent = JSON.parse(event.data);
                        observer.next(agentEvent);

                        // 检查是否是错误事件
                        if (event.event === 'error') {
                            throw new FatalError(event.data);
                        }
                    } catch (error) {
                        if (error instanceof FatalError) {
                            throw error;
                        }
                        console.error('Failed to parse SSE message:', error);
                        // 解析错误不中断流，继续处理下一个消息
                    }
                },

                // 连接意外关闭时的回调
                onclose() {
                    observer.complete();
                },

                // 错误处理回调
                onerror(error) {
                    console.error('SSE error:', error);
                    observer.error(error);
                    throw error; // 重新抛出以停止重试
                },

                // 重试配置
                openWhenHidden: false, // 页面隐藏时不保持连接
            }).catch(error => {
                // 处理最终的错误
                console.error('Final SSE error:', error);
                observer.error(error);
            });

            // 返回清理函数
            return () => {
                abortController.abort();
            };
        });
    }

    /**
     * 运行 Agent 并返回事件流
     * @param threadId 线程ID
     * @param message 用户输入的消息
     * @param checkpointId 检查点ID
     * @param agentBeanName Agent Bean名称（可选）
     * @returns Observable<AgentEvent>
     */
    public runAgentStream(threadId: string, message: string | undefined = undefined, checkpointId: string | undefined = undefined, agentBeanName?: string): Observable<AgentEvent> {
        const requestBody: ChatStreamRequest = {
            threadId: threadId,
            userMessage: message,
            checkpointId: checkpointId,
            agentBeanName: agentBeanName
        };

        return this.createEventStream('/chat/stream', requestBody);
    }

    /**
     * 从检查点重放对话并返回事件流
     * @param threadId 线程ID
     * @param checkpointId 检查点ID
     * @param agentBeanName Agent Bean名称（可选）
     * @returns Observable<AgentEvent>
     */
    public replayFromCheckpoint(threadId: string, checkpointId: string, agentBeanName?: string): Observable<AgentEvent> {
        const requestBody: ChatReplayRequest = {
            threadId: threadId,
            checkpointId: checkpointId,
            agentBeanName: agentBeanName
        };

        return this.createEventStream('/chat/replay', requestBody);
    }

    /**
     * 获取会话列表
     * @param agentBeanName Agent Bean名称（可选）
     * @returns Promise<ThreadInfo[]>
     */
    public async getThreadList(agentBeanName?: string): Promise<ThreadInfo[]> {
        const params = agentBeanName ? { agentBeanName } : undefined;
        const url = this.buildApiUrl('/thread/list', params);

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`获取会话列表失败: ${response.status} ${response.statusText}`);
        }

        return response.json();
    }

    /**
     * 创建新会话
     * @param title 会话标题
     * @param agentBeanName Agent Bean名称（可选）
     * @returns Promise<CreateThreadResponse>
     */
    public async createThread(title: string, agentBeanName?: string): Promise<CreateThreadResponse> {
        const response = await fetch(`${AGENT_API_URL}/thread/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                title,
                agentBeanName
            }),
        });

        if (!response.ok) {
            throw new Error(`创建会话失败: ${response.status} ${response.statusText}`);
        }

        return response.json();
    }

    /**
     * 获取会话的消息列表
     * @param threadId 会话ID
     * @param agentBeanName Agent Bean名称（可选）
     * @returns Promise<ThreadItem[]>
     */
    public async getThreadItems(threadId: string, agentBeanName?: string): Promise<ThreadItem[]> {
        const params = agentBeanName ? { agentBeanName } : undefined;
        const url = this.buildApiUrl(`/thread/items/${threadId}`, params);

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`获取会话内容失败: ${response.status} ${response.statusText}`);
        }

        return response.json();
    }

    /**
     * 删除会话
     * @param threadId 会话ID
     * @returns Promise<void>
     */
    public async deleteThread(threadId: string): Promise<void> {
        const response = await fetch(`${AGENT_API_URL}/thread/${threadId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`删除会话失败: ${response.status} ${response.statusText}`);
        }
    }

    /**
     * 更新会话标题
     * @param threadId 会话ID
     * @param title 新标题
     * @returns Promise<void>
     */
    public async updateThreadTitle(threadId: string, title: string): Promise<void> {
        const response = await fetch(`${AGENT_API_URL}/thread/updateTitle`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                threadId: threadId,
                title: title
            }),
        });

        if (!response.ok) {
            throw new Error(`更新会话标题失败: ${response.status} ${response.statusText}`);
        }
    }

    /**
     * 获取所有可用的agent列表
     * @returns Promise<string[]>
     */
    public async getAgentList(): Promise<string[]> {
        const response = await fetch(`${AGENT_API_URL}/agents/list`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`获取agent列表失败: ${response.status} ${response.statusText}`);
        }

        const data: AgentListResponse = await response.json();
        return data.agents;
    }
}

// 导出单例实例
export const agentService = AgentService.getInstance(); 