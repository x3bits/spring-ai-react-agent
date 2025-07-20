package com.x3bits.springaireactagent.web.controller;

import com.x3bits.springaireactagent.web.entity.Thread;
import com.x3bits.springaireactagent.web.service.ThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread控制器 - 与Python API完全兼容
 */
@RestController
@RequestMapping("/springAiReactAgent/api")
public class ThreadController {

    @Autowired
    private ThreadService threadService;

    private static final String DEFAULT_USER_ID = "guest";

    /**
     * 创建新线程
     * POST /thread/create
     */
    @PostMapping("/thread/create")
    public ResponseEntity<CreateThreadResponse> createThread(@RequestBody CreateThreadRequest request) {
        if (request.getAgentBeanName() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            // 使用请求中的agentBeanName，如果没有指定则使用默认值
            String agent =  request.getAgentBeanName();
            Thread thread = threadService.createThread(DEFAULT_USER_ID, request.getTitle(), agent);
            CreateThreadResponse response = new CreateThreadResponse(thread.getThreadId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取线程列表
     * GET /thread/list?agentBeanName=xxx
     */
    @GetMapping("/thread/list")
    public ResponseEntity<List<Thread>> listThreads(@RequestParam(value = "agentBeanName") String agentBeanName) {
        try {
            List<Thread> threads = threadService.getThreadsByUserIdAndAgent(DEFAULT_USER_ID, agentBeanName);
            return ResponseEntity.ok(threads);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 更新线程标题
     * POST /thread/update_title
     */
    @PostMapping("/thread/updateTitle")
    public ResponseEntity<Map<String, Object>> updateThreadTitle(@RequestBody UpdateThreadTitleRequest request) {
        try {
            boolean success = threadService.updateThreadTitle(request.getThreadId(), request.getTitle());
            Map<String, Object> response = new HashMap<>();

            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除线程
     * DELETE /thread/{threadId}
     */
    @DeleteMapping("/thread/{threadId}")
    public ResponseEntity<Map<String, Object>> deleteThread(@PathVariable("threadId") String threadId) {
        try {
            boolean success = threadService.deleteThread(threadId);
            Map<String, Object> response = new HashMap<>();

            if (success) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== DTO Classes ==========

    /**
     * 创建线程请求体
     */
    public static class CreateThreadRequest {
        private String title;
        private String agentBeanName;

        public CreateThreadRequest() {
        }

        public CreateThreadRequest(String title, String agentBeanName) {
            this.title = title;
            this.agentBeanName = agentBeanName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAgentBeanName() {
            return agentBeanName;
        }

        public void setAgentBeanName(String agentBeanName) {
            this.agentBeanName = agentBeanName;
        }
    }

    /**
     * 创建线程响应体
     */
    public static class CreateThreadResponse {
        private String threadId;

        public CreateThreadResponse() {
        }

        public CreateThreadResponse(String threadId) {
            this.threadId = threadId;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
    }

    /**
     * 更新线程标题请求体
     */
    public static class UpdateThreadTitleRequest {
        private String threadId;
        private String title;

        public UpdateThreadTitleRequest() {
        }

        public UpdateThreadTitleRequest(String threadId, String title) {
            this.threadId = threadId;
            this.title = title;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}