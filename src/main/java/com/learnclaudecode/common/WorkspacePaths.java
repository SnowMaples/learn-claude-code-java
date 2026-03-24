package com.learnclaudecode.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 工作区路径工具，负责安全路径校验和常用目录访问。
 */
public final class WorkspacePaths {
    private final Path workdir;

    /**
     * 使用指定工作目录初始化路径工具。
     *
     * @param workdir 工作目录
     */
    public WorkspacePaths(Path workdir) {
        this.workdir = workdir.toAbsolutePath().normalize();
    }

    /**
     * 返回规范化后的工作目录。
     *
     * @return 工作目录路径
     */
    public Path workdir() {
        return workdir;
    }

    /**
     * 在工作区内安全解析相对路径。
     *
     * @param relativePath 相对路径
     * @return 解析后的绝对路径
     */
    public Path safeResolve(String relativePath) {
        // 所有文件工具都必须走这里，防止模型通过 ../ 逃逸出当前工作区。
        Path resolved = workdir.resolve(relativePath).normalize();
        if (!resolved.startsWith(workdir)) {
            throw new IllegalArgumentException("Path escapes workspace: " + relativePath);
        }
        return resolved;
    }

    /**
     * 读取工作区内指定文件的文本内容。
     *
     * @param relativePath 相对路径
     * @return 文件文本
     * @throws IOException 读取失败时抛出
     */
    public String readText(String relativePath) throws IOException {
        return Files.readString(safeResolve(relativePath), StandardCharsets.UTF_8);
    }

    /**
     * 向工作区内指定文件写入文本内容。
     *
     * @param relativePath 相对路径
     * @param content 写入内容
     * @throws IOException 写入失败时抛出
     */
    public void writeText(String relativePath, String content) throws IOException {
        Path path = safeResolve(relativePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * 返回任务目录路径。
     *
     * @return .tasks 目录路径
     */
    public Path tasksDir() {
        return workdir.resolve(".tasks");
    }

    /**
     * 返回团队协作目录路径。
     *
     * @return .team 目录路径
     */
    public Path teamDir() {
        return workdir.resolve(".team");
    }

    /**
     * 返回队友 inbox 目录路径。
     *
     * @return inbox 目录路径
     */
    public Path inboxDir() {
        return teamDir().resolve("inbox");
    }

    /**
     * 返回技能目录路径。
     *
     * @return skills 目录路径
     */
    public Path skillsDir() {
        return workdir.resolve("skills");
    }

    /**
     * 返回会话转录目录路径。
     *
     * @return .transcripts 目录路径
     */
    public Path transcriptDir() {
        return workdir.resolve(".transcripts");
    }

    /**
     * 返回 worktree 隔离目录路径。
     *
     * @return .worktrees 目录路径
     */
    public Path worktreesDir() {
        return workdir.resolve(".worktrees");
    }
}
