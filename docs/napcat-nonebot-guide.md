# NapCat + NoneBot2 入门教程

> 来源整理: [NapCat 官方文档](https://napneko.github.io/)、[NoneBot 官方文档](https://nonebot.dev/docs/)、[OneBot11 协议](https://11.onebot.dev)
> 适用: 从零搭建基于 **NapCat(协议端) + NoneBot2(机器人框架)** 的 QQ 机器人

---

## 1. 背景与架构

### 1.1 三层架构

```
┌─────────────────────────────────────────────┐
│  你的机器人逻辑 (NoneBot2, Python)           │   ← 业务层: 命令、问答、功能
├─────────────────────────────────────────────┤
│  协议端 (NapCat, 基于 NTQQ)                  │   ← 协议层: 收发 QQ 消息/事件
│    ↳ 通过 OneBot11 协议 (HTTP/WS) 对外服务   │
├─────────────────────────────────────────────┤
│  QQNT 客户端 (真实登录的 QQ)                 │   ← 账号层: 扫码登录
└─────────────────────────────────────────────┘
```

- **NapCat**: 基于 TypeScript 构建的 Bot 框架,通过启动器主动调用 QQNT 客户端提供的接口实现 Bot 功能;对外按 **OneBot11** 约定提供 HTTP / WebSocket 接口。
- **NoneBot2**: 现代、跨平台、可扩展的 **Python 异步机器人框架**(Python ≥ 3.9),通过「适配器」连接不同协议端,如 `nonebot-adapter-onebot`。
- 两者通过 **反向 WebSocket**(NapCat 作为 WS 客户端连接 NoneBot)或正向 WS / HTTP 通信。

### 1.2 为什么用这套组合(对比 mirai)

| 维度 | mirai-console (现 longqbot) | NapCat + NoneBot2 |
|------|------------------------------|-------------------|
| 语言 | Java / Kotlin | Python(业务)/ TS(协议端) |
| 登录 | 签名服务 + 设备信息,维护成本高 | 直接扫码登录 NTQQ,省心 |
| 协议 | mirai 私有协议 | 标准 OneBot11,生态通用 |
| 生态 | mirai 插件(Java/Kotlin) | NoneBot 插件(Python,量大) |
| 开发速度 | 反射回调手动路由 | 依赖注入 + 响应器,开箱即用 |

> 补充: 若只想换协议端、保留 mirai 插件,可用 [Overflow](https://github.com/MrXiaoM/Overflow) 让 NapCat 反向对接 mirai 插件,无需重写。

---

## 2. 部署 NapCat

### 2.1 安装
按官方指引下载对应平台安装包(Windows 有 Shell/Installer 等版本),启动后完成 **QQ 扫码登录**。

### 2.2 登录与 WebUI
1. 启动 NapCat,进入 WebUI(默认 `http://127.0.0.1:6099/webui`),首次登录用启动日志里的随机 token。
2. 进入 **QQ 登录 → QRCode**,用手机 QQ 扫码。
3. 登录成功后,**强制要求修改 WebUI 密码**,否则大部分功能被禁用。
4. 随后进入 **网络配置** 创建连接。

> 公网部署务必启用 token 鉴权。

### 2.3 四种网络连接(NapCat 视角)

| 类型 | 方向 | 说明 |
|------|------|------|
| HTTP 服务端 | NapCat 接收请求 | 单工: 外部调用 API,不主动推送事件 |
| HTTP 客户端 | NapCat 发起请求 | 单工: 把事件 POST 到指定地址 |
| WebSocket 服务端(正向 WS) | NapCat 监听 | 双工: 既能推送事件也能接收请求 |
| WebSocket 客户端(反向 WS) | NapCat 主动连接 | 双工: **对接 NoneBot 最常用** |

---

## 3. 部署 NoneBot2

### 3.1 安装脚手架
```bash
python -m pip install --user pipx
python -m pipx ensurepath    # 完成后重开终端
pipx install nb-cli
```

### 3.2 创建项目
```bash
nb create
```
交互选项建议:
- 模板选 **simple**(自行写插件)或 bootstrap(可装商店插件);
- 适配器选 **OneBot V11**;
- 驱动器选 **FastAPI**(默认自带反向 WS);
- 选择创建虚拟环境,立即安装依赖。

### 3.3 运行
```bash
nb run
```
NoneBot 启动后会输出监听端口(默认 `8080`),并在 `/onebot/v11/ws` 提供反向 WS 接入点。

---

## 4. NapCat ↔ NoneBot 对接(反向 WS)

### 4.1 关键参数
- NoneBot 反向 WS 地址: `ws://127.0.0.1:8080/onebot/v11/ws`
  - `8080` = NoneBot 输出端口;`/onebot/v11/ws` = onebot 适配器默认路径
- 鉴权: 若 NapCat 配置了 token,NoneBot 侧 `.env` 中必须设置 `ONEBOT_ACCESS_TOKEN=<同一token>`,否则会 403。

### 4.2 配置步骤
1. **NoneBot 侧**(若 403 或启用 token): 编辑 `.env`,加入
   ```env
   ONEBOT_ACCESS_TOKEN=你的token
   ```
   然后 `nb run` 启动,记录端口。
2. **NapCat 侧**: WebUI → 网络配置 → 新建 → **WebSocket 客户端**,URL 填
   ```
   ws://127.0.0.1:8080/onebot/v11/ws
   ```
   保存后即完成对接。

### 4.3 验证
在 NoneBot 项目中启用自带 `echo` 插件或写一个 ping-pong 响应器,群里发 `#ping` 应收到回复。

---

## 5. 第一个 NoneBot 插件

### 5.1 目录结构(NoneBot 插件)
```
awesome-bot/
├── .env                     # 全局配置(驱动、token 等)
├── pyproject.toml
└── src/plugins/
    └── my_plugin/           # 一个插件 = 一个包
        ├── __init__.py      # 插件入口,加载响应器
        └── config.py        # 插件配置(可选)
```

### 5.2 基础响应器示例
```python
# src/plugins/my_plugin/__init__.py
from nonebot import on_command, on_message
from nonebot.rule import to_me
from nonebot.adapters.onebot.v11 import Bot, GroupMessageEvent, MessageEvent

# 命令响应器: #ping
ping = on_command("ping", priority=10)

@ping.handle()
async def _():
    await ping.finish("pong!")

# 仅@机器人时响应的消息
catch = on_message(rule=to_me(), priority=100, block=False)

@catch.handle()
async def _(event: MessageEvent):
    await catch.send(f"你说: {event.get_plaintext()}")
```

### 5.3 常用 API 速写(对照 mirai)
```python
# 发送群消息
await bot.send_group_msg(group_id=123, message="hello")

# 发送私聊消息
await bot.send_private_msg(user_id=456, message="hi")

# 获取群列表
groups = await bot.get_group_list()

# 撤回消息
await bot.delete_msg(message_id=msg_id)

# 取登录信息
info = await bot.get_login_info()
```

### 5.4 常用事件类型
```python
from nonebot.adapters.onebot.v11 import (
    GroupMessageEvent,   # 群消息
    PrivateMessageEvent, # 私聊消息
    GroupMemberIncreaseEvent,  # 入群
    GroupMemberDecreaseEvent,  # 退群
    GroupBanEvent,             # 禁言
)
```

---

## 6. 进阶主题

### 6.1 NapCat 内置插件(TS)与 NoneBot 插件的选择
- **需要高性能 / 与 NapCat WebUI 联动 / 直接调 NapCat 底层**: 写 NapCat TS 插件(生命周期 `plugin_init` / `plugin_onmessage` / `plugin_onevent` / `plugin_cleanup`,通过 `ctx.actions.call(...)` 调 OneBot Action)。
- **快速迭代业务逻辑 / 使用 Python 生态**: 写 NoneBot 插件,经反向 WS 通信。
- 两者可共存: NapCat 负责协议与底层,NoneBot 负责业务。

### 6.2 定时任务
```python
from nonebot import require
scheduler = require("nonebot_plugin_apscheduler").scheduler

@scheduler.scheduled_job("cron", hour=8, minute=0)
async def daily():
    await bot.send_group_msg(group_id=..., message="早安")
```

### 6.3 数据持久化
- NoneBot: 用 `nonebot-plugin-localstore` 或直接 sqlite/json;插件配置可用 Pydantic 模型 + `get_plugin_config`。
- NapCat 插件: `ctx.dataPath` / `ctx.configPath` 提供持久化目录。

---

## 7. 参考链接
- NapCat 快速开始: https://napneko.github.io/guide/start-install
- NapCat 接入框架(NoneBot 等): https://napneko.github.io/use/integration
- NapCat 插件开发: https://napneko.github.io/develop/plugin/
- NoneBot 概览/快速上手: https://nonebot.dev/docs/ / https://nonebot.dev/docs/quick-start
- OneBot11 协议: https://11.onebot.dev
- Overflow(mirai 插件迁移到 NapCat): https://github.com/MrXiaoM/Overflow
