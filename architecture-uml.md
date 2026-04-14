# 核心架构 UML 图

## 1. 类图 (Class Diagram)

```mermaid
classDiagram
    class Launcher {
        +launch(StageConfig): void
    }

    class StageConfig {
        +prompt: String
        +enableTodoNag: boolean
        +enableCompression: boolean
        +enableBackground: boolean
        +enableInbox: boolean
        +subagentWritable: boolean
        +autonomousTeammates: boolean
        +tools: List~Map~String, Object~~
        +systemTemplate: String
        +systemPrompt(SkillLoader, Path): String
        +s01(): StageConfig
        +s02(): StageConfig
        +s03(): StageConfig
        +s04(): StageConfig
        +s05(): StageConfig
        +s06(): StageConfig
        +s07(): StageConfig
        +s08(): StageConfig
        +s09(): StageConfig
        +s10(): StageConfig
        +s11(): StageConfig
        +s12(): StageConfig
        +sFull(): StageConfig
    }

    class AppContext {
        -env: EnvConfig
        -paths: WorkspacePaths
        -client: AnthropicClient
        -commandTools: CommandTools
        -todoManager: TodoManager
        -skillLoader: SkillLoader
        -compressionService: CompressionService
        -taskManager: TaskManager
        -backgroundManager: BackgroundManager
        -messageBus: MessageBus
        -teammateManager: TeammateManager
        -worktreeManager: WorktreeManager
        -runtime: AgentRuntime
        +runtime(): AgentRuntime
    }

    class AgentRuntime {
        -client: AnthropicClient
        -paths: WorkspacePaths
        -commandTools: CommandTools
        -todoManager: TodoManager
        -compressionService: CompressionService
        -taskManager: TaskManager
        -backgroundManager: BackgroundManager
        -messageBus: MessageBus
        -teammateManager: TeammateManager
        -worktreeManager: WorktreeManager
        -skillLoader: SkillLoader
        +runRepl(StageConfig): void
        +agentLoop(List~ChatMessage~, StageConfig): void
        +runSubagent(String, StageConfig): void
    }

    class AnthropicClient {
        -env: EnvConfig
        +createMessage(String, List~ChatMessage~, List~Map~, int): AnthropicResponse
    }

    class CommandTools {
        -paths: WorkspacePaths
        +runBash(String): String
        +runRead(String, Integer): String
        +runWrite(String, String): String
        +runEdit(String, String, String): String
    }

    class TodoManager {
        +update(List~Map~String, Object~~): String
        +list(): String
    }

    class SkillLoader {
        -paths: WorkspacePaths
        +getDescriptions(): String
        +getContent(String): String
    }

    class CompressionService {
        -paths: WorkspacePaths
        -client: AnthropicClient
        +microCompact(List~ChatMessage~): void
        +autoCompact(List~ChatMessage~, StageConfig): void
        +needsAutoCompact(List~ChatMessage~): boolean
    }

    class TaskManager {
        -paths: WorkspacePaths
        +create(String, String): String
        +update(Map~String, Object~): String
        +listAll(): String
        +getTask(int): String
        +claim(int, String): String
        +scanUnclaimed(): List~Map~String, Object~~
    }

    class BackgroundManager {
        -paths: WorkspacePaths
        -commandTools: CommandTools
        -notifications: BlockingQueue~Map~String, Object~~
        +run(String, Integer): String
        +check(String): String
        +drain(): List~Map~String, Object~~
    }

    class MessageBus {
        -paths: WorkspacePaths
        +send(String, String, String, String): String
        +readInbox(String): String
    }

    class TeammateManager {
        -paths: WorkspacePaths
        -client: AnthropicClient
        -commandTools: CommandTools
        -messageBus: MessageBus
        -taskManager: TaskManager
        +spawn(String, String, String, boolean): String
        +listAll(): String
        +handleShutdownRequest(String): String
        +handlePlanReview(Map~String, Object~): String
    }

    class WorktreeManager {
        -paths: WorkspacePaths
        -taskManager: TaskManager
        +create(String, int): String
        +list(): String
        +remove(String, boolean): String
    }

    class EnvConfig {
        +getApiKey(): String
        +getModel(): String
        +getBaseUrl(): String
    }

    class WorkspacePaths {
        -workdir: Path
        +tasksDir(): Path
        +teamDir(): Path
        +worktreesDir(): Path
        +transcriptsDir(): Path
        +safeResolve(String): Path
    }

    Launcher --> AppContext: creates
    AppContext --> StageConfig: uses
    AppContext --> AgentRuntime: creates
    AgentRuntime --> AnthropicClient: calls
    AgentRuntime --> CommandTools: calls
    AgentRuntime --> TodoManager: calls
    AgentRuntime --> SkillLoader: calls
    AgentRuntime --> CompressionService: calls
    AgentRuntime --> TaskManager: calls
    AgentRuntime --> BackgroundManager: calls
    AgentRuntime --> MessageBus: calls
    AgentRuntime --> TeammateManager: calls
    AgentRuntime --> WorktreeManager: calls

    CommandTools --> WorkspacePaths: uses
    TaskManager --> WorkspacePaths: uses
    MessageBus --> WorkspacePaths: uses
    WorktreeManager --> WorkspacePaths: uses

    CompressionService --> WorkspacePaths: uses
    CompressionService --> AnthropicClient: uses

    BackgroundManager --> CommandTools: uses

    TeammateManager --> MessageBus: uses
    TeammateManager --> TaskManager: uses

    WorktreeManager --> TaskManager: uses
```
```

## 2. 核心流程时序图 (Sequence Diagram)

### 2.1 Agent 主循环

```mermaid
sequenceDiagram
    participant User
    participant REPL as AgentRuntime.runRepl()
    participant Loop as agentLoop()
    participant Client as AnthropicClient
    participant Tools as CommandTools
    participant Config as StageConfig

    User->>REPL: 用户输入 query
    REPL->>Config: 获取 systemPrompt, tools
    REPL->>Loop: agentLoop(messages, config)

    rect rgb(240, 248, 255)
        Note over Loop: 前置处理
        alt enableCompression
            Loop->>Loop: microCompact() / autoCompact()
        end
        alt enableBackground
            Loop->>Loop: backgroundManager.drain()
        end
        alt enableInbox
            Loop->>Loop: messageBus.readInbox("lead")
        end
    end

    Loop->>Client: createMessage(system, messages, tools)
    Client-->>Loop: AnthropicResponse

    alt stop_reason == "tool_use"
        loop 工具调用
            Loop->>Tools: execute tool
            Tools-->>Loop: tool_result
            Loop->>Loop: messages.add(tool_result)
            Loop->>Client: 继续下一轮
            Client-->>Loop: response
        end
    else stop_reason != "tool_use"
        Note over Loop: 返回文本结果
    end

    Loop-->>REPL: 循环结束
    REPL-->>User: 打印结果
```

### 2.2 启动流程

```mermaid
sequenceDiagram
    participant Main as S01AgentLoop.main()
    participant Launcher as Launcher.launch()
    participant Context as AppContext
    participant Runtime as AgentRuntime

    Main->>Launcher: launch(StageConfig.s01())
    Launcher->>Context: new AppContext()
    rect rgb(255, 248, 240)
        Note over Context: 依赖注入
        Context->>Context: new EnvConfig()
        Context->>Context: new WorkspacePaths()
        Context->>Context: new AnthropicClient()
        Context->>Context: new CommandTools()
        Context->>Context: new TodoManager()
        Context->>Context: new SkillLoader()
        Context->>Context: new CompressionService()
        Context->>Context: new TaskManager()
        Context->>Context: new BackgroundManager()
        Context->>Context: new MessageBus()
        Context->>Context: new TeammateManager()
        Context->>Context: new WorktreeManager()
        Context->>Context: new AgentRuntime(...)
    end
    Context->>Runtime: runtime()
    Launcher->>Runtime: runRepl(config)
```

### 2.3 多 Agent 协作流程 (S09-S11)

```mermaid
sequenceDiagram
    participant Lead as Lead Agent
    participant TM as TeammateManager
    participant MB as MessageBus
    participant Tasks as TaskManager
    participant Teammate as Teammate Thread

    Lead->>TM: spawn_teammate("alice", "coder", prompt)
    TM->>Teammate: 启动守护线程
    TM-->>Lead: "Spawned 'alice'"

    Lead->>MB: send_message(to="alice", content="...")
    MB-->>Lead: "Message sent"

    Teammate->>MB: readInbox("alice")
    MB-->>Teammate: messages

    Teammate->>Teammate: model.decision()
    Teammate->>Teammate: execute tools
    Teammate->>MB: send_message(to="lead", result)

    alt autonomous mode
        Teammate->>Tasks: scanUnclaimed()
        Tasks-->>Teammate: unclaimed tasks
        Teammate->>Tasks: claim(task_id)
        Teammate->>Teammate: work on task
    end
```

## 3. 包结构图 (Package Diagram)

```mermaid
graph TD
    subgraph "com.learnclaudecode"
        A[agents] -->|依赖| B[common]
        A -->|依赖| C[tools]
        A -->|依赖| D[model]
        A -->|依赖| E[context]
        A -->|依赖| F[tasks]
        A -->|依赖| G[team]
        A -->|依赖| H[background]
        A -->|依赖| I[skills]
    end

    subgraph "agents 包"
        A1[Launcher]
        A2[AgentRuntime]
        A3[AppContext]
        A4[StageConfig]
        A5[S01-S12, SFull]
    end

    subgraph "common 包"
        B1[AnthropicClient]
        B2[EnvConfig]
        B3[WorkspacePaths]
        B4[JsonUtils]
    end

    subgraph "tools 包"
        C1[CommandTools]
        C2[TodoManager]
    end

    subgraph "其他包"
        D[model: 数据模型]
        E[CompressionService]
        F[TaskManager, WorktreeManager]
        G[MessageBus, TeammateManager]
        H[BackgroundManager]
        I[SkillLoader]
    end

    A --> A1
    A --> A2
    A --> A3
    A --> A4
    A --> A5
```

## 4. 阶段能力演进图

```mermaid
graph LR
    S01[🔵 S01: 最小闭环] --> S02[S02: 文件工具]
    S02 --> S03[S03: Todo]
    S03 --> S04[S04: 子代理]
    S04 --> S05[S05: 技能加载]
    S05 --> S06[S06: 上下文压缩]
    S06 --> S07[S07: 任务系统]
    S07 --> S08[S08: 后台任务]
    S08 --> S09[S09: 多Agent协作]
    S09 --> S10[S10: 团队协议]
    S10 --> S11[S11: 自治队友]
    S11 --> S12[S12: Worktree隔离]
    S12 --> SF[🟢 SFull: 完整版]

    style S01 fill:#e1f5fe
    style SF fill:#c8e6c9
```

## 5. 关键设计模式图

```mermaid
graph TD
    subgraph "依赖注入 (Dependency Injection)"
        DI1[AppContext 构造函数] --> DI2[创建所有服务]
        DI2 --> DI3[AgentRuntime 聚合]
    end

    subgraph "策略模式 (Strategy Pattern)"
        SP1[StageConfig] --> SP2[s01()-s12()]
        SP2 --> SP3[不同能力配置]
    end

    subgraph "责任链模式 (Chain of Responsibility)"
        CH1[agentLoop] --> CH2[compression]
        CH2 --> CH3[background drain]
        CH3 --> CH4[inbox read]
        CH4 --> CH5[model call]
    end

    subgraph "生产者-消费者 (Producer-Consumer)"
        PC1[BackgroundManager] --> PC2[execute thread]
        PC2 --> PC3[notifications.offer]
        PC3 --> PC4[drain()]
    end
```

## 6. 核心架构总结

| 设计模式 | 位置 | 说明 |
|---------|------|------|
| **依赖注入** | AppContext 构造函数 | 所有服务通过构造函数注入，AgentRuntime 聚合所有服务 |
| **策略模式** | StageConfig | s01-s12 不同阶段对应不同"策略"（工具集 + 能力开关） |
| **责任链** | AgentRuntime.agentLoop() | 每轮循环依次处理：压缩 → 后台结果 → inbox → 模型调用 |
| **生产者-消费者** | BackgroundManager | execute 线程生产通知，drain() 消费 |
| **单例运行时** | AppContext.runtime() | 全局共享一个 AgentRuntime 实例 |
| **文件型持久化** | TaskManager, MessageBus | JSON/JSONL 文件存储，无外部依赖 |