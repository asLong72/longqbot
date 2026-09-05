# longqbot: mirai → NoneBot 迁移规划(TODO)

> 生成日期: 2026-08-15
> 目标: 把现有基于 **mirai-console 2.15.0** 的 longqbot(Java/Kotlin, v1.1.17)迁移到 **NoneBot2 + NapCat(OneBot11)**。

---

## 0. 迁移决策(先读)

**两条路线(可组合):**

| 路线 | 说明 | 适用 | 状态 |
|------|------|------|------|
| **A. 渐进重写** | 新写 NoneBot 项目,按功能逐个迁移;期间旧 mirai 仍可运行 | 想换技术栈、长期维护 | ⏳ **未来**(见第 3 节 TODO) |
| **B. Overflow 过渡** | 用 [Overflow](https://github.com/MrXiaoM/Overflow) 让 NapCat 对接 mirai 插件,原插件不改代码继续用 | 只想换协议端、暂不重写 | ✅ **当前执行**(2026-08-15 已选定) |

> **当前(路线 B)**: 操作步骤详见 `docs/overflow-napcat-migration.md`。
> **未来(路线 A)**: 路线 B 稳定运行后,按本文档第 3 节逐个迁移到 NoneBot,迁移完成即停用 Overflow。

**本机环境前提(已确认):**
- Python 需 ≥ 3.9(建议 3.11+)用于 NoneBot
- Node 环境用于 NapCat(或使用官方启动器,按平台安装)

---

## 1. 现状功能清单(迁移源)

| # | 功能 | 入口/指令 | 涉及模块(现 mirai) |
|---|------|-----------|---------------------|
| F1 | 群开关 | 超管 `!原神!` / `!黑暗!`(含开/关脚本) | `Longqbot.GroupMSGListener`, `BasicConfig` |
| F2 | 帮助 | `#帮助`(列已启用功能) | `Longqbot`, `funcs` |
| F3 | 功能启用/禁用 | `#启用 功能 [1-4]`(仅超管) / `#禁用 功能` | `Longqbot`, `BasicConfig.groupWhiteList` |
| F4 | 群白名单权限(1仅超管/2+群主/3+管理/4全员) | 各功能内 `PermissionUtil` | `PermissionUtil`, `BasicConfig` |
| F5 | 问答 | `#问答 添加/修改/删除/引用/禁用/列表/条目`;`#数字`;直接命中问题 | `QAController`, `QAConfig`(tipsMap/qaMap/tipsAllowinGroup) |
| F6 | 经典表情包 | `#经典 添加/引用/禁用/列表`;`#经典 典 分组 关键词`;`(分组 关键词)` | `QuotationsController`, `QuotationConfig`(QuotationsMap/tipsAllowinGroup), `ImgUtils`, `CrawlerUtil` |
| F7 | 进群欢迎 | 自动 @新成员 + 欢迎词;`#欢迎 修改 欢迎词/冷却`;`#欢迎 状态`;按消息数冷却 | `WelcomeController`, `WelcomeConfig` |
| F8 | lol 自定义征召 | `#lol 自定义 [@玩家]` 随机分队/英雄池;`重随`/`结束`/`更新英雄`(爬 LOL 官网) | `LOLController`, `LOLConfig`, `CrawlerUtil`(Selenium+jsoup), `ImgUtils`(头像/英雄拼图) |
| F9 | 反广告 | 撤回可疑发言 + 警示 | `AdAntiController`, `AdAntiConfig`(creditRecord/invitionship) |
| F10 | 合成表情包 | 未开发(空实现) | `ComposeMemeController` |
| F11 | 每日新闻 | 未实现(仅空壳命令) | `DailyNewsCommand` |

---

## 2. 迁移目标架构

```
NoneBot2 (Python 3.11+, src/plugins/)
├── plugins/
│   ├── core/            # F1-F4: 群开关/帮助/启用禁用/权限(依赖注入)
│   ├── qa/              # F5: 问答
│   ├── quotation/       # F6: 经典表情包
│   ├── welcome/         # F7: 欢迎(APScheduler 或状态机做冷却)
│   ├── lol/             # F8: 自定义征召(头像/英雄拼图)
│   ├── anti_ad/         # F9: 反广告
│   └── ...              # F10/F11 可选
├── .env                 # ONEBOT_ACCESS_TOKEN / 驱动配置
└── data/                # 本地持久化(JSON/SQLite)
        ↕ 反向 WS (ws://127.0.0.1:8080/onebot/v11/ws)
NapCat (协议端, NTQQ 登录)
```

---

## 3. 迁移 TODO 清单(按阶段)

### 当前执行: 路线 B — Overflow 过渡(2026-08-15 已选定)
- [x] 选定方案: Overflow + NapCat 对接(操作指南: `docs/overflow-napcat-migration.md`)
- [ ] 部署 NapCat(扫码登录 + 启用正向/反向 WS)
- [ ] 下载 Overflow 整合包,首次启动生成 overflow.json
- [ ] 配置 overflow.json 连接 NapCat(推荐正向 WS)
- [ ] 迁移 longqbot jar + data + config 到 Overflow 目录(登录相关文件不迁移)
- [ ] 启动验证(#帮助/问答/经典图/欢迎/lol/开关群)
- [ ] 视情况安装 LocalFileService(图片以本地路径传输)
- [ ] 稳定运行观察期;体验问题登记到仓库记忆

### 阶段 1(路线 A,未来): 环境与骨架(约 1-2 天)
- [ ] 部署 NapCat: 安装 → 扫码登录 → WebUI 改密 → 网络配置(参考 `docs/napcat-nonebot-guide.md`)
- [ ] 安装 Python 3.11+ 与 `nb-cli`,创建 NoneBot 项目(`nb create`,模板 simple + OneBot V11 + FastAPI 驱动)
- [ ] 对接: NapCat 新建 WS 客户端 → `ws://127.0.0.1:8080/onebot/v11/ws`;`.env` 配 `ONEBOT_ACCESS_TOKEN`
- [ ] 验证链路: 内置 `echo` 插件 + `#ping` 响应器跑通
- [ ] 建立项目目录结构(core/qa/quotation/welcome/lol/anti_ad)与 `data/` 持久化约定(JSON)

### 阶段 2(路线 A,未来): 核心框架迁移(F1-F4,约 1-2 天)
- [ ] F1 群开关: `on_command("原神")`/`on_command("黑暗")`,仅超管(依赖注入校验 `event.user_id == SUPER_ADMIN`);启用/禁用时发送脚本文案
- [ ] F3 功能启用/禁用: `#启用 功能 [1-4]` / `#禁用 功能`,权限=超管;存储群级功能状态(JSON)
- [ ] F4 权限系统: 实现 `group_permission(user_id, role, state)` 依赖函数(1=仅超管, 2=群主, 3=管理+, 4=全员),对照现 `PermissionUtil`
- [ ] F2 帮助: `#帮助` 列出本群已启用功能(读群状态 + 各插件元数据)
- [ ] 配置: 把 `BasicConfig`(superAdmin/groupEnable/groupWhiteList/scripts)迁到 `.env` + 本地 JSON
- [ ] ✅ 里程碑: 群开关 + 帮助 + 启用/禁用 可在新 bot 完整使用

### 阶段 3(路线 A,未来): 功能迁移(F5-F9,约 4-6 天)
- [ ] F5 问答: `on_command("问答")` 子命令(add/edit/reference/ban/list);数据 `qaMap`/`tipsMap`/`tipsAllowinGroup` 迁到 JSON/SQLite;保留 `#数字` 快捷查询与"直接命中问题"逻辑
- [ ] F6 经典表情包: `on_command("经典")` 子命令;图片落盘 `data/quotation/<分组>/<子分组>.<ext>`;实现 `(分组 关键词)` 快捷触发(用 `startswith` 规则或 Matcher `Rule`);图片上传用 OneBot `image` 消息段(文件路径/base64);`CrawlerUtil.saveFile` 改用 `httpx`/`aiohttp`
- [ ] F7 欢迎: 监听 `GroupMemberIncreaseEvent`;冷却改为"按群消息数递减"状态机(存内存+JSON),或改用 APScheduler 时间冷却(简化);`#欢迎 修改 欢迎词/冷却`、`#欢迎 状态`
- [ ] F8 lol: `#lol 自定义/重随/结束/更新英雄`;随机分队逻辑照搬;头像/英雄拼图用 `Pillow` 实现(替代 `ImgUtils.joinXxxListHorizontal`);`更新英雄` 爬 LOL 官网改用 `httpx` + 静态页解析(去掉 Selenium 依赖更佳)
- [ ] F9 反广告: `on_message` 规则匹配可疑发言 → `delete_msg` 撤回 + 群内警示;实现 `creditRecord`/`invitionship` 积分逻辑;**修复 mirai 版恒真 `if(true)` 隐患**(新实现不要犯)
- [ ] 数据迁移脚本: 把 mirai `data/xxx/*.yml` 的 QA/Quotation/LOL/Welcome/AdAnti 配置转成新 JSON 格式

### 阶段 4(路线 A,未来): 收尾与上线(约 1-2 天)
- [ ] F10/F11 决定: 合成表情包(若做,用 Pillow 拼接)与每日新闻(定时任务)作为后续迭代,不阻塞迁移
- [ ] 权限复核: 所有"仅超管/群主/管理"入口用统一依赖注入校验;确认无越权
- [ ] 日志与错误处理: 各 handler 包 try/except,防止单异常拖垮 bot
- [ ] 灰度: 先在一个测试群与新 bot 跑通全功能,对照旧 bot 输出
- [ ] 文档: 更新 README;迁移说明归档
- [ ] 下线: 确认稳定后停用 mirai 侧,保留代码与数据备份

---

## 4. 迁移风险与对策

| 风险 | 对策 |
|------|------|
| 旧数据格式(yml/ImageIndicator)兼容 | 写一次性迁移脚本;图片 `nativeURI` 复制到新 data 目录 |
| 权限模型(1-4)行为差异 | 先写 `group_permission` 依赖 + 单元测试对照旧 `PermissionUtil` |
| Selenium 爬 LOL 官网不稳定 | 改为 `httpx` 直接解析或固定本地英雄数据(heroList 存 JSON) |
| 消息段差异(image/at/reply) | 以 `docs/onebot11-api-spec.md` 第 5 节对照表为准逐条转换 |
| 冷却/定时逻辑 | 统一用 APScheduler;欢迎冷却语义按"消息数"或"时间"二选一,提前定好 |
| NapCat token/403 | `.env` 的 `ONEBOT_ACCESS_TOKEN` 与 NapCat 侧 token 保持一致 |

---

## 5. 参考
- **Overflow 过渡操作指南(当前执行)**: `docs/overflow-napcat-migration.md`
- 入门与对接(NoneBot 路线): `docs/napcat-nonebot-guide.md`
- 接口规范与对照: `docs/onebot11-api-spec.md`
- 原项目档案/待办: `/memories/repo/longqbot-project.md`、`/memories/repo/longqbot-todo.md`
