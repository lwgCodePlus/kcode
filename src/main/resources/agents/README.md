# KCode 多智能体架构

<p align="center">
  <strong>六大智能体协同工作，构建高效 AI 编程助手</strong>
</p>

---

## 🏛️ 架构概述

KCode 采用 **Hub-and-Spoke（枢纽-辐射）** 架构，由一个编排器（Orchestrator）作为核心枢纽，协调五个专业智能体（Spokes）各司其职。

```
                    ┌─────────────────┐
                    │   Orchestrator  │
                    │   (编排器)      │
                    └─────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌───────────┐      ┌───────────┐      ┌───────────┐
│  Explorer │      │  Librarian│      │   Oracle  │
│  (探索者) │      │ (知识检索)│      │  (顾问)   │
└───────────┘      └───────────┘      └───────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    ┌──────┴──────┐
                    │             │
                    ▼             ▼
             ┌───────────┐ ┌───────────┐
             │  Designer │ │   Fixer   │
             │  (设计师) │ │  (执行者) │
             └───────────┘ └───────────┘
```

---

## 📋 智能体列表

| 智能体 | 文件 | 角色 | 推荐模型 |
|--------|------|------|----------|
| **Orchestrator** | `kcodeAgent.md` | 编排协调，任务委托 | GPT-4 / Claude |
| **Explorer** | `explorerAgent.md` | 代码库搜索探索 | GPT-4-mini / 快速模型 |
| **Oracle** | `oracleAgent.md` | 架构决策、调试顾问 | GPT-4 / Claude |
| **Librarian** | `librarianAgent.md` | 文档检索、库研究 | GPT-4-mini |
| **Designer** | `designerAgent.md` | UI/UX 设计实现 | Claude / GPT-4 |
| **Fixer** | `fixerAgent.md` | 快速代码执行 | GPT-4-mini / 快速模型 |

---

## 🔍 智能体详解

### 01. Orchestrator（编排器）

**职责**：主入口，分析任务并委托给专业智能体。

**特点**：
- 理解请求意图（明确需求 + 隐含需求）
- 路径分析（质量、速度、成本、可靠性）
- 委托决策（何时委托、委托给谁）
- 并行协调（多个智能体同时工作）
- 结果整合与验证

**委托规则**：
| 专家 | 委托时机 | 不委托时机 |
|------|----------|------------|
| @explorer | 需发现未知内容 | 已知路径需读取内容 |
| @librarian | 复杂/演进 API | 基础稳定用法 |
| @oracle | 高风险决策 | 常规决策 |
| @designer | 用户界面体验 | 后端逻辑 |
| @fixer | 3+ 独立任务 | 1-2 简单任务 |

---

### 02. Explorer（探索者）

**职责**：代码库快速搜索和探索。

**工具使用**：
- `glob`：文件名模式匹配
- `grep`：内容正则搜索
- `astGrep`：AST 结构搜索
- `list_directory`：目录浏览
- `view_text_file`：文件内容查看

**输出格式**：
```xml
<results>
<files>
- /path/to/file.java:42 - 发现内容描述
</files>
<answer>问题的简洁回答</answer>
</results>
```

---

### 03. Oracle（顾问）

**职责**：战略技术顾问，架构决策和复杂调试。

**适用场景**：
- ✅ 重大架构决策
- ✅ 2+ 次修复后问题仍存在
- ✅ 高风险重构
- ✅ 安全/性能权衡分析
- ❌ 常规决策
- ❌ 第一次 bug 修复

**输出格式**：
```xml
<analysis>问题描述</analysis>
<options>多种选项分析</options>
<recommendation>推荐方案</recommendation>
<implementation_notes>实施注意事项</implementation_notes>
```

---

### 04. Librarian（知识检索）

**职责**：外部文档和库研究。

**工具使用**：
- `context7`：官方文档查找
- `grepApp`：GitHub 代码搜索
- `webSearch`：网页搜索
- `webFetch`：获取网页内容

**来源可靠性**：
| 来源 | 可靠性 |
|------|--------|
| 官方文档 | 高 |
| GitHub 高星仓库 | 高 |
| 活跃维护项目 | 中高 |
| 社区博客 | 中 |

---

### 05. Designer（设计师）

**职责**：UI/UX 设计和前端实现。

**设计原则**：
- **字体**：选择独特字体，避免通用默认
- **色彩**：主导色 + 锐利强调色
- **动效**：关注高影响时刻，一个时机正确的动画
- **空间**：打破惯例，大量负空间或控制密度
- **深度**：渐变网格、噪点纹理、分层透明

**支持技术栈**：
- Vue 3 + Tailwind CSS
- React + Tailwind CSS
- HTML/CSS/SASS

---

### 06. Fixer（执行者）

**职责**：快速代码执行。

**约束**：
- ✅ 只读文件 → 执行更改 → 验证完成
- ❌ 禁止外部研究（webSearch、context7）
- ❌ 禁止委托（background_task）
- ❌ 禁止多步骤研究/规划

**输出格式**：
```xml
<summary>实现摘要</summary>
<changes>
- file1.java: 更改描述
</changes>
<verification>
- 测试通过: yes/no/skip
- LSP 诊断: clean/errors
</verification>
```

---

## 🔄 工作流程

### 标准流程

1. **理解**：Orchestrator 解析请求
2. **分析**：评估方法路径
3. **委托**：决策委托给谁
4. **并行**：多智能体同时工作
5. **整合**：收集结果
6. **验证**：确认满足需求

### 并行策略

```typescript
// 多个 Explorer 搜索不同领域
task(explorer, "搜索 Java 服务类")
task(explorer, "搜索配置文件")

// Explorer + Librarian 并行研究
task(explorer, "查找现有实现")
task(librarian, "查找官方文档")

// 多个 Fixer 并行执行
task(fixer, "修改文件 A")
task(fixer, "修改文件 B")
task(fixer, "修改文件 C")
```

---

## 📁 文件结构

```
src/main/resources/agents/
├── README.md           # 多智能体架构说明
├── kcodeAgent.md       # Orchestrator（编排器）
├── explorerAgent.md    # Explorer（探索者）
├── oracleAgent.md      # Oracle（顾问）
├── librarianAgent.md   # Librarian（知识检索）
├── designerAgent.md    # Designer（设计师）
└── fixerAgent.md       # Fixer（执行者）
```

---

## 🎯 使用示例

### 示例 1：添加新功能

```
用户：添加用户认证功能

Orchestrator 流程：
1. 委托 @explorer 查找现有认证实现
2. 委托 @librarian 查找 Spring Security 文档
3. 委托 @oracle 设计认证架构
4. 委托 @fixer 实现代码
5. 委托 @designer 实现登录界面
```

### 示例 2：调试问题

```
用户：为什么启动报 NullPointerException？

Orchestrator 流程：
1. 委托 @explorer 查找错误位置
2. 初次修复尝试
3. 如果失败 → 委托 @oracle 深度分析
4. 委托 @fixer 执行修复
```

### 示例 3：重构代码

```
用户：重构 Service 层

Orchestrator 流程：
1. 委托 @oracle 分析重构方案
2. 委托 @explorer 查找所有 Service 类
3. 委托多个 @fixer 并行修改
```

---

## 📊 模型推荐配置

| 智能体 | 推荐模型 | temperature | 说明 |
|--------|----------|-------------|------|
| Orchestrator | GPT-4 / Claude | 0.1 | 需要高质量推理 |
| Explorer | GPT-4-mini | 0.1 | 快速搜索，低成本 |
| Oracle | GPT-4 / Claude | 0.1 | 深度分析需要高质量 |
| Librarian | GPT-4-mini | 0.1 | 文档检索，成本效率 |
| Designer | Claude / GPT-4 | 0.7 | 创造性设计需要灵活性 |
| Fixer | GPT-4-mini | 0.2 | 执行任务需要一致性 |

---

## 🔗 参考来源

本多智能体架构参考了 [oh-my-opencode-slim](https://github.com/alvinunreal/oh-my-opencode-slim) 项目的设计理念，并根据 KCode 项目特点进行了适配。

---

## 📝 后续开发

### 待实现功能

1. **智能体注册机制**：Spring Bean 管理智能体
2. **智能体通信协议**：统一的请求/响应格式
3. **任务调度系统**：并行任务管理
4. **结果验证机制**：自动质量检查
5. **上下文传递**：智能体间上下文共享

### 技术实现路径

1. 定义 `Agent` 接口和基类
2. 实现各智能体的 Spring Bean
3. 开发 `AgentOrchestrator` 调度器
4. 集成 agentscope-java 框架
5. 添加智能体配置管理

---

## 📜 许可证

本多智能体架构文档属于 KCode 项目的一部分，遵循 MIT 许可证。