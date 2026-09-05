# longqbot × Overflow × NapCat 对接操作指南(当前执行方案)

> 生成日期: 2026-08-15
> 决策: **路线 B(Overflow 过渡)** 为当前执行方案 —— 让 longqbot(mirai 插件)通过 Overflow 桥接 NapCat 继续运行,**不改业务代码**。
> 未来完全迁移到 NoneBot 的规划见 `docs/migrate-mirai-to-nonebot.md` 与仓库记忆 TODO。

---

## 1. 方案原理

```
┌──────────────────────────────────────────────────────────────┐
│  longqbot (mirai-console 插件, Java/Kotlin, 无需修改)        │
│    ↳ 依赖 mirai-core-api 接口(事件/消息/权限)                 │
├──────────────────────────────────────────────────────────────┤
│  Overflow (mirai 的 OneBot11 转接器/适配器)                   │
│    ↳ 扮演 OneBot 客户端 + mirai-core-api 实现                 │
│    ↳ 把 OneBot 事件转成 mirai 事件, 把 mirai 调用转成 OneBot  │
├──────────────────────────────────────────────────────────────┤
│  NapCat (OneBot11 协议端, 基于 NTQQ)                          │
│    ↳ 负责真实 QQ 登录与收发(替代 mirai 签名服务)              │
└──────────────────────────────────────────────────────────────┘
```

- **Overflow** 是 [mirai-core-api](https://github.com/mamoe/mirai/tree/dev/mirai-core-api) 的实现: 桥接 mirai 接口与 OneBot11 实现,**mirai 插件无需修改业务代码**。
- 连接方式二选一(只能选一种):
  - **正向 WebSocket(推荐)**: NapCat 做服务端,Overflow 做客户端主动连过去。
  - **反向 WebSocket**: Overflow 做服务端,NapCat 做客户端连过来。
- 前提: Java 8+;已部署一个 OneBot11 实现(NapCatQQ)。

---

## 2. 部署 NapCat(协议端)

1. 按 [NapCat 官方指引](https://napneko.github.io/guide/start-install) 下载安装(Windows 可用 Shell 版),启动并 **QQ 扫码登录**。
2. 打开 WebUI(默认 `http://127.0.0.1:6099/webui`),用启动日志随机 token 登录,登录后**强制修改密码**。
3. **网络配置 → 新建 → WebSocket 服务端(正向 WS)**:

   - 端口: 如 `3001`
   - 消息上报格式: 建议 `array`(与 mirai 消息段兼容更稳)
   - token: 可留空或自定义(若留空,Overflow 侧 token 也留空)
   - 记录该端口,作为 Overflow 的连接目标。

   > 若选反向 WS: 在 NapCat 新建 **WebSocket 客户端**,URL 填 Overflow 反向端口地址(见第 4 节)。
   >

---

## 3. 安装 Overflow(协议桥)

安装方式任选其一(推荐前两种):


| 方式                              | 说明                                                                                        |
| ----------------------------------- | --------------------------------------------------------------------------------------------- |
| **① 官网一键打包整合包**(最简单) | 访问 https://mirai.mrxiaom.top/#get-started 下载 "Overflow + mirai-console 整合包",解压即用 |
| ② MCL 脚本安装                   | https://mirai.mrxiaom.top/docs/install/MCLScript.html                                       |
| ③ 编辑 MCL config.json           | https://mirai.mrxiaom.top/docs/install/MCL.html                                             |
| ④ 替换 mirai-core 类库           | https://mirai.mrxiaom.top/docs/install/Raw.html                                             |
| ⑤ 已修改的 MCL                   | https://mirai.mrxiaom.top/docs/install/MCLOverflow.html                                     |
| ⑥ Docker(第三方)                 | https://hub.docker.com/r/sdjnmxd/overflow                                                   |

> 需要自动更新功能可改用 [MCL-patch1-with-overflow](https://github.com/MrXiaoM/mirai-console-loader/releases/download/v2.1.2-patch1/with-overflow.zip)。

**首次启动**: 解压后在目录运行启动脚本(MCL)至少一次,生成配置文件 `overflow.json` 与 `plugins/`、`data/`、`config/` 目录结构。

---

## 4. 配置 overflow.json

编辑 `overflow.json`,配置 `connections`(**只启用一种**)。

### 4.1 正向 WebSocket(NapCat 做服务端,推荐)

```json5
{
  "connections": [
    {
      "enable": true,
      "type": "websocket",          // 正向: Overflow 作为客户端
      "host": "ws://127.0.0.1:3001", // NapCat 正向 WS 地址(第 2 节端口)
      "token": ""                    // 与 NapCat 侧 token 一致, 留空则都不设
    }
  ]
}
```

### 4.2 反向 WebSocket(NapCat 做客户端)

```json5
{
  "connections": [
    {
      "enable": true,
      "type": "websocket-reverse", // 反向: Overflow 作为服务端
      "port": 3002,                 // Overflow 监听端口
      "token": ""
    }
  ]
}
```

然后到 NapCat WebUI **网络配置 → 新建 → WebSocket 客户端**,URL 填 `ws://127.0.0.1:3002`。

> 详细配置字段见 [Overflow 配置文件说明](https://mirai.mrxiaom.top/docs/configuration.html)。

---

## 5. 迁移 longqbot 插件与数据

把 longqbot 从原 mirai 环境搬到 Overflow 整合包:


| 原 mirai 目录           | 目标(Overflow 整合包目录) | 说明                                            |
| ------------------------- | --------------------------- | ------------------------------------------------- |
| `plugins/longqbot*.jar` | `plugins/`                | 编译产物 jar(当前 1.1.17)                       |
| `data/<插件数据目录>`   | `data/`                   | longqbot 的问答/经典/欢迎/lol/反广告 数据(yaml) |
| `config/<longqbot相关>` | `config/`                 | longqbot 的 AutoSavePluginConfig 配置           |
| `logs/`                 | 可留原处备份              | 历史日志                                        |

**注意(重要)**:

- **登录相关文件无需移动**: `AutoLogin.yml`、`mirai-device-generator.jar`、`fix-protocol-version.jar` 等,因为登录改由 NapCat 负责。
- 图片数据: longqbot 的 `QuotationsController` 把表情包图片存到 `data/xxx/img/quotation/...`,`ImgUtils.getImagefromImageIndicator` 依赖 `nativeURI`(相对工作目录)。迁移时 **保持工作目录相对关系一致**(Overflow 进程工作目录 = 整合包根目录),确保图片路径仍能解析。
- 若图片经 base64 传输异常,可安装附属插件 [LocalFileService](https://github.com/MrXiaoM/LocalFileService)(以本地文件路径传输图片/语音/视频),更贴合 longqbot 现有图片逻辑。

---

## 6. 启动顺序与验证

### 6.1 启动顺序

1. 先启动 **NapCat**,确认已扫码登录、正向/反向 WS 已启用。
2. 再启动 **Overflow**(MCL 启动脚本),观察日志:
   - 出现连接成功日志(正向 WS 握手 / 反向 WS 监听)。
   - longqbot 插件 `onEnable` 日志(`plugin loaded`)。

### 6.2 验证清单

- [ ]  群里发 `#帮助`,应列出已启用功能
- [ ]  `#问答 列表` 正常(数据已迁移)
- [ ]  `(经典 分组 关键词)` 能发图(图片路径解析正常)
- [ ]  新人进群触发欢迎
- [ ]  `#lol 自定义 @玩家` 分队/英雄池正常
- [ ]  超管 `!原神!` / `!黑暗!` 开关群正常

### 6.3 排错

- **连不上**: 检查 `overflow.json` 的 host/port、token 是否与 NapCat 一致;NapCat 是否把服务端/客户端启用。
- **消息类型不识别**: 日志 `logs/unknown_messages.log` 会记录不支持的消息类型,可据此反馈 Overflow 或换 `messagePostFormat`。
- **进程秒退**: 启动加 JVM 参数 `-Doverflow.not-exit=true`(连不上 OneBot 时不退出)便于看日志。
- **超时**: `-Doverflow.timeout=10000`(action 请求超时)、`-Doverflow.timeout-process=200000`(事件转换超时)。

### 6.4 常用 JVM 参数


| 参数                              | 说明                        |
| ----------------------------------- | ----------------------------- |
| `-Doverflow.config=路径`          | 指定 overflow.json 路径     |
| `-Doverflow.not-exit=true`        | 连不上 OneBot 时不结束进程  |
| `-Doverflow.timeout=毫秒`         | action 请求超时, 默认 10000 |
| `-Doverflow.timeout-process=毫秒` | 事件转换超时, 默认 200000   |

> ⚠️ 不要使用 `-Doverflow.skip-token-security-check`(除非完全清楚安全风险)。

---

## 7. 已知限制(评估)

- Overflow 部分 mirai 接口未完整实现(基础收发消息已稳定)。
- longqbot 用到的接口: `GroupMessageEvent`/`MemberJoinEvent`/`MessageSource.recall`/`Image.queryUrl`/图片上传等,均在常用范围内,但需实测。
- 反广告撤回(`MessageSource.recall`)依赖 OneBot `delete_msg`,NapCat 支持。
- 多 Bot 反向 WS 支持有限(正向 WS 无此问题)。
- Overflow 更新节奏慢、维护者个人维护;这正是"未来完全迁移 NoneBot"的理由之一。

---

## 8. 与"未来完全迁移"的关系

- **当前(路线 B)**: Overflow 过渡,longqbot 代码不改,快速恢复运行。
- **未来(路线 A)**: 渐进重写 NoneBot2 插件(见 `docs/migrate-mirai-to-nonebot.md`),届时按功能逐个迁移后,再停用 Overflow。
- 迁移期间 **原 mirai 代码与数据保留备份**,双轨运行可对比验证。

---

## 参考链接

- Overflow 官网/下载: https://mirai.mrxiaom.top/
- Overflow 用户手册: https://mirai.mrxiaom.top/docs/UserManual
- Overflow 配置文件: https://mirai.mrxiaom.top/docs/configuration.html
- NapCat 快速开始: https://napneko.github.io/guide/start-install
- NapCat 对接框架(Mirai): https://napneko.github.io/use/integration
- LocalFileService: https://github.com/MrXiaoM/LocalFileService
