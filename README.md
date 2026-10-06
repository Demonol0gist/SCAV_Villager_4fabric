# SCAV_Villager_4fabric


#注意，这里是模组的移植版本，移植版本不代表本产品的最终品质，感谢您的理解与支持，祝你好运！

Scav Villager的 **Fabric 客户端**移植 —— 由 Arkit 的 Forge 模组改写而来。

- **纯客户端**：服务器（含原版服务器）和其他玩家都不需要安装
- 支持 **1.20.1 / 全部 12 个 1.21.x 稳定版 / 26.2**

## 构建

两套工具链（因为 1.21.6 起 Minecraft 不再混淆，26.2 要用去混淆版 Loom）：

| 目标 | 需要 | 命令 |
| --- | --- | --- |
| 1.20.1 – 1.21.11 | JDK 21、Gradle 8.7、Loom 1.6.12 | `cd ScavVillager-Fabric && gradle build` |
| 26.2 | **JDK 25**、Gradle 9.8、Loom 1.18.2 | `cd ScavVillager-Fabric-26 && gradle build` |

26.2 的源码由 `构建脚本/make_26_sources.py` 从 `ScavVillager-Fabric/common` 自动改写而来
（类改名 / 换包：`ResourceLocation`→`Identifier`、实体类换子包、`GuiGraphics`→`GuiGraphicsExtractor` 等），
生成结果在 `src26/`（已随仓库提供，改了共享代码后重新跑一遍脚本即可）。

详细说明见两边的 `说明.md`（版本对照、已修问题、可调配置、验证程度）。

## 版权

素材与整体设计来自 **Arkit** 的《Scav_villager》Forge 模组。
Fabric 客户端移植：**Demonologist**。
