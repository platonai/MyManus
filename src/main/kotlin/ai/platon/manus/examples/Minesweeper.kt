package ai.platon.manus.examples

import ai.platon.manus.agent.plan.PlanningFlow
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ai.platon.manus.api"])
class MinesweeperApplication(
    private val planningFlow: PlanningFlow
) {
    private val query = """
### 📌 产品需求文档：Minesweeper 游戏（扫雷）

**产品经理：** ivincent.zhang@gmail.com
**开发负责人：** MyManus
**版本：** v1.0
**发布日期：** TBD

---

### 一、背景与目标

我们希望实现一个经典的扫雷游戏（Minesweeper），用于学习与展示 Python 的基本图形界面开发能力。

---

### 二、核心需求

#### 1. 游戏玩法逻辑

- 游戏棋盘为二维网格，尺寸支持初级（9x9, 10雷）、中级（16x16, 40雷）、高级（30x16, 99雷）。
- 玩家点击格子：
  - 若该格子是地雷，游戏失败，显示所有雷。
  - 若不是雷，显示该格子周围雷的数量。
  - 若周围雷为0，则自动展开周围格子。
- 玩家可右键（或用键盘操作）标记“旗帜”，表示怀疑此处是地雷。
- 当玩家标出所有雷，或打开所有非雷格子时，游戏胜利。

#### 2. 基本功能

- 可视化界面（优先使用 tkinter，如有更优选择可提）。
- 显示游戏用时。
- 显示剩余未标记的雷数量。

---

### 三、技术要求

- 使用 Python 3.x 开发。
- UI 优先使用 `tkinter`，如需使用其他库请提前沟通。
- 项目结构清晰，代码模块化（例如分为逻辑层、UI 层）。
- 提供简单的 README 文档，说明如何运行程序。

---

    """

    @PostConstruct
    fun run() {
        planningFlow.newPlan("plan_" + System.currentTimeMillis())
        planningFlow.execute(query)
    }
}

fun main() {
    // val additionalProfiles = mutableListOf("private")
    System.setProperty("spring.profiles.include", "private")
    runApplication<MinesweeperApplication>()
}
