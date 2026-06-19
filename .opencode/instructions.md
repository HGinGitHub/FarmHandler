# Farmhand Mod — 项目规则

## 版本管理

- **需求变更** 或 **功能新增** 时，必须同步递增 `gradle.properties` 中的 `mod_version`
- 版本号格式：`主版本.次版本.修订号`
  - 主版本：破坏性变更或不兼容改动
  - 次版本：功能新增
  - 修订号：Bug 修复、小型优化

## 文档同步

- **任何代码改动**（包括但不限于新增功能、修改逻辑、修复 Bug、配置变更）都必须同步更新 `README.md`
- README 中需要更新的部分：
  - 功能列表（是否有新增/修改的功能）
  - 详细用法说明（操作方式是否有变化）
  - 支持作物列表（是否有新增作物类型）
  - 配置项说明（配置是否有变动）
  - 依赖项说明（依赖是否有增减）
  - 源码结构目录（是否有新文件/模块）

## 提交规范

- 所有改动完成后，必须提交 git
- 提交信息使用**中文**
- 每次提交应原子化，不同模块的改动分多次提交
- 提交信息格式：
  - `feat: xxx` — 新功能
  - `fix: xxx` — Bug 修复
  - `docs: xxx` — 文档更新
  - `refactor: xxx` — 重构
  - `build: xxx` — 构建/依赖变更

## 发布流程

- 完成功能开发并确认 `mod_version` 已递增后，打标签并推送以触发 GitHub Actions 自动发布：
  1. 确保 `gradle.properties` 中 `mod_version` 与标签版本一致
  2. 推送代码到远程：`git push`
  3. 打标签：`git tag v{mod_version}`（如 `git tag v1.1.0`）
  4. 推送标签：`git push origin v{mod_version}`
- Actions 会自动构建 JAR 并在 Releases 页面创建发行版
- **注意**：标签必须推送，仅推代码不会触发 Release

## 检查清单（每次改动后执行）

- [ ] `mod_version` 是否已递增？
- [ ] `README.md` 是否已同步更新？
- [ ] 工作区是否已 git 提交？
- [ ] 如需发布，是否已打标签并推送？
