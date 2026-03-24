package com.learnclaudecode.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务持久化模型，字段命名尽量对齐 Python 版 JSON。
 */
public class TaskRecord {
    public int id;
    public String subject;
    public String description = "";
    public String status = "pending";
    public String owner = "";
    public String worktree = "";
    public List<Integer> blockedBy = new ArrayList<>();
    public List<Integer> blocks = new ArrayList<>();
    public long created_at;
    public long updated_at;
}
