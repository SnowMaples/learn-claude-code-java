package com.learnclaudecode.background;

import com.learnclaudecode.common.WorkspacePaths;
import com.learnclaudecode.tools.CommandTools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 后台任务管理器，对齐 s08 的异步执行与通知机制。
 */
public class BackgroundManager {
    private final WorkspacePaths paths;
    private final CommandTools commandTools;
    private final ConcurrentHashMap<String, Map<String, Object>> tasks = new ConcurrentHashMap<>();
    private final BlockingQueue<Map<String, Object>> notifications = new LinkedBlockingQueue<>();

    /**
     * 初始化后台任务管理器。
     *
     * @param paths 工作区路径工具
     */
    public BackgroundManager(WorkspacePaths paths) {
        this.paths = paths;
        this.commandTools = new CommandTools(paths);
    }

    /**
     * 以异步方式启动后台命令。
     *
     * @param command 命令文本
     * @param timeoutSeconds 超时时间
     * @return 后台任务启动结果
     */
    public String run(String command, int timeoutSeconds) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        tasks.put(taskId, new ConcurrentHashMap<>(Map.of(
                "status", "running",
                "command", command,
                "result", ""
        )));
        // 每个后台命令放到独立线程执行，主对话循环只拿 taskId，不阻塞当前交互。
        Executors.newSingleThreadExecutor().submit(() -> execute(taskId, command, timeoutSeconds));
        return "Background task " + taskId + " started: " + command.substring(0, Math.min(80, command.length()));
    }

    /**
     * 在后台线程中真正执行命令并记录状态。
     *
     * @param taskId 任务 ID
     * @param command 命令文本
     * @param timeoutSeconds 超时时间
     */
    private void execute(String taskId, String command, int timeoutSeconds) {
        String status;
        String result;
        try {
            ProcessBuilder builder = new ProcessBuilder(commandTools.shellCommand(command));
            builder.directory(paths.workdir().toFile());
            Process process = builder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                // 超时后强制终止，避免失控命令长期占用资源。
                process.destroyForcibly();
                status = "timeout";
                result = "Error: Timeout (" + timeoutSeconds + "s)";
            } else {
                String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                result = (stdout + stderr).trim();
                if (result.isBlank()) {
                    result = "(no output)";
                }
                status = "completed";
            }
        } catch (Exception e) {
            status = "error";
            result = "Error: " + e.getMessage();
        }
        // 执行结束后同时更新任务表和通知队列，供主循环在后续轮次注入结果。
        tasks.put(taskId, new ConcurrentHashMap<>(Map.of(
                "status", status,
                "command", command,
                "result", result.substring(0, Math.min(50000, result.length()))
        )));
        notifications.offer(Map.of(
                "task_id", taskId,
                "status", status,
                "result", result.substring(0, Math.min(500, result.length()))
        ));
    }

    /**
     * 查询后台任务状态。
     *
     * @param taskId 任务 ID，为空时返回全部任务
     * @return 任务状态文本
     */
    public String check(String taskId) {
        if (taskId != null && !taskId.isBlank()) {
            Map<String, Object> task = tasks.get(taskId);
            if (task == null) {
                return "Error: Unknown task " + taskId;
            }
            return "[" + task.get("status") + "] " + task.get("command") + "\n" + task.get("result");
        }
        if (tasks.isEmpty()) {
            return "No background tasks.";
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : tasks.entrySet()) {
            lines.add(entry.getKey() + ": [" + entry.getValue().get("status") + "] " + entry.getValue().get("command"));
        }
        return String.join("\n", lines);
    }

    /**
     * 取出当前累计的后台结果通知。
     *
     * @return 通知列表
     */
    public List<Map<String, Object>> drain() {
        List<Map<String, Object>> result = new ArrayList<>();
        notifications.drainTo(result);
        return result;
    }
}
