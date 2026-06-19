# Farmhand — 一键种植 / 连锁收获 / 自动锄地

[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)](https://fabricmc.net)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen)](https://minecraft.net)
[![Java](https://img.shields.io/badge/Java-25%2B-orange)](https://adoptium.net)

**Farmhand** 是一个 Minecraft Fabric 实用模组，为原版 Farming 体验提供**范围自动化**操作——自动锄地、连锁种植、连锁收获+自动补种，所有功能仅在**潜行时触发**，非潜行完全保留原版行为。

> 🌱 **种田加速器，不破坏原版平衡，不添加新方块/物品。**

---

## 功能一览

| 功能 | 触发方式 | 作用范围 | 说明 |
|---|---|---|---|
| 🧑‍🌾 **自动锄地** | 潜行 + 右键（手持锄头） | 9×9 | 将周围可耕方块统一变为耕地，消耗耐久 |
| 🌱 **连锁种植** | 潜行 + 右键（手持种子） | 17×17 | 在空耕地上批量种植，种子用完后自动从背包补充 |
| 🌾 **连锁收获+补种** | 潜行 + 右键（对准成熟作物） | 17×17 | 收获同种成熟作物并自动消耗种子补种 |

---

## 详细用法

### 自动锄地

1. 手持**任何锄头**
2. **潜行** + 右键点击地面
3. 以点击位置为中心的 9×9 范围内的草方块、泥土、粗泥、土径、缠根泥土会被翻为耕地
4. 耐久不足时自动停止，避免锄头损坏

### 连锁种植

1. 手持**种子/作物物品**（小麦种子、胡萝卜、马铃薯、甜菜种子、下界疣、可可豆）
2. **潜行** + 右键点击**空耕地**
3. 以点击位置为中心的 17×17 范围内所有空耕地会被种上对应作物
4. 当前手持种子用完后，自动从背包**工具栏 → 背包**依次搜索同种种子继续种植

### 连锁收获 + 自动补种

1. **潜行** + 右键点击**成熟作物**
2. 以点击位置为中心的 17×17 范围内所有**同种成熟作物**会被一并收获
3. 收获后自动从背包搜索种子进行补种（可可豆会在周围丛林原木上补种）
4. 支持的特殊作物：
   - **标准作物**：小麦、胡萝卜、马铃薯、甜菜根、下界疣 → 收获 + 补种
   - **可可豆** → 收获 + 在周围丛林原木自动补种
   - **甜浆果** → 收获浆果，植株保留为 age=1
   - **发光浆果** → 收获浆果，vines 保留
   - **西瓜 / 南瓜** → 破坏果实方块
   - **甘蔗** → 破坏第二格及以上，保留根部
   - **竹子** → 破坏第二格及以上，保留根部

---

## 支持作物列表

| 种子/物品 | 作物方块 | 种植 | 收获 | 补种 |
|---|---|---|---|---|
| 小麦种子 | 小麦 | ✅ | ✅ | ✅ |
| 胡萝卜 | 胡萝卜 | ✅ | ✅ | ✅ |
| 马铃薯 | 马铃薯 | ✅ | ✅ | ✅ |
| 甜菜种子 | 甜菜 | ✅ | ✅ | ✅ |
| 下界疣 | 下界疣 | ✅ | ✅ | ✅ |
| 可可豆 | 可可豆 | ✅ | ✅ | ✅（丛林原木） |
| — | 甜浆果 | ❌ | ✅ | — |
| — | 发光浆果 | ❌ | ✅ | — |
| — | 西瓜/南瓜 | ❌ | ✅ | — |
| — | 甘蔗 | ❌ | ✅（保留根部） | — |
| — | 竹子 | ❌ | ✅（保留根部） | — |

---

## 安装

### 环境要求

- **Minecraft**: 26.1.2（对应 1.21.5+）
- **Mod Loader**: Fabric Loader ≥0.19.3
- **Java**: ≥25
- **依赖**: Fabric API（任意兼容版本）

### 步骤

1. 安装 [Fabric Loader](https://fabricmc.net/use/)
2. 下载 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `.minecraft/mods/`
3. 下载 Farmhand 的 JAR 文件放入 `.minecraft/mods/`
4. 启动游戏

---

## 构建

```bash
# Windows
gradlew build

# macOS / Linux
./gradlew build
```

构建产物位于 `build/libs/` 目录。

---

## 模组信息

- **Mod ID**: `farmhand`
- **开发框架**: Fabric Loom
- **映射**: Mojang 官方映射（Official mappings）
- **许可证**: [CC0-1.0](LICENSE)

---

## 开发

本项目使用 Fabric Loom 构建，源码结构：

```
src/
├── main/
│   ├── java/com/farmhand/
│   │   ├── TemplateMod.java          # Mod 主入口
│   │   └── handler/FarmHandler.java  # 核心功能处理器
│   └── resources/
│       └── fabric.mod.json           # Mod 元数据
└── client/
    ├── java/com/farmhand/client/
    │   └── TemplateModClient.java    # 客户端入口
    └── resources/
```

核心逻辑全部集中在 `FarmHandler.java` 中，通过 Fabric API 的 `UseBlockCallback` 事件监听玩家交互。
