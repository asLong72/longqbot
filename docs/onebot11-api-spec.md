# OneBot11 接口规范(含 mirai 对照)

> 来源整理: [OneBot11 协议标准](https://11.onebot.dev)、[NapCat API 文档](https://napneko.github.io/onebot/api)、[mirai 文档](https://mirai.mamoe.net/)
> 用途: longqbot(mirai)迁移到 NoneBot(NapCat 作协议端)时的接口对照参考。

---

## 1. 总体模型

OneBot11 定义了统一的「**连接规范 + API + 事件 + 消息段**」,所有平台实现(如 NapCat)对外行为一致:

- **API(Action)**: 主动调用,如发消息、撤回、查群。
- **事件(Event)**: 被动上报,如收到消息、有人入群。
- **消息段(CQ 码/数组)**: 消息的组成单元(文本/图片/@/表情等)。

### 1.1 请求 / 响应格式
```jsonc
// 请求
{ "action": "send_group_msg", "params": { "group_id": 123, "message": "hi" }, "echo": "id-1" }

// 响应
{ "status": "ok", "retcode": 0, "data": { "message_id": 5678 }, "echo": "id-1" }
```
- `echo` 用于请求-响应配对(异步连接时必备)。
- `retcode` `0` = 成功;`1` = 失败;`100` = 未实现。

### 1.2 事件通用字段
```jsonc
{
  "post_type": "message",   // message / notice / request / meta_event
  "self_id": 10001,          // 机器人自身 QQ
  "time": 1700000000,
  // ...按事件类型附加字段
}
```

---

## 2. 网络连接方式

| 方式 | 描述 | 对接 NoneBot | 对接 mirai |
|------|------|-------------|-----------|
| HTTP 服务端 | 外部轮询/调用,单工 | 不常用 | 无 |
| HTTP 客户端 | 事件 POST 到指定 URL | 不常用 | 无 |
| 正向 WebSocket | 框架连 NapCat,双工 | 支持 | Overflow 推荐用正向 WS |
| 反向 WebSocket | NapCat 连框架,双工 | **推荐(默认)** | 支持 |

对接 NoneBot 默认反向 WS 地址: `ws://127.0.0.1:8080/onebot/v11/ws`。

---

## 3. 常用 API(Action)清单

### 3.1 消息类
| API | 参数 | 说明 | mirai 对照 |
|-----|------|------|-----------|
| `send_msg` | `message_type`(group/private), `user_id`/`group_id`, `message` | 通用发消息 | `Contact.sendMessage` |
| `send_group_msg` | `group_id`, `message` | 发群消息 | `Group.sendMessage` |
| `send_private_msg` | `user_id`, `message` | 发私聊 | `Friend.sendMessage` |
| `delete_msg` | `message_id` | 撤回消息 | `MessageSource.recall` |
| `get_msg` | `message_id` | 取消息详情 | 消息引用/查询 |
| `get_forward_msg` | `message_id` | 取合并转发 | — |
| `send_group_forward_msg` | `group_id`, `messages` | 发合并转发 | `ForwardMessage` |
| `get_image` / `get_record` | `file` | 取图片/语音资源 | `Image.queryUrl` |
| `send_poke` | 目标 | 戳一戳 | `Member.nudge` |

### 3.2 群管理类
| API | 参数 | 说明 | mirai 对照 |
|-----|------|------|-----------|
| `set_group_kick` | `group_id`, `user_id` | 踢人 | `Member.kick` |
| `set_group_ban` | `group_id`, `user_id`, `duration` | 禁言(秒) | `Member.mute` |
| `set_group_whole_ban` | `group_id`, `enable` | 全体禁言 | `Group.muteAll` |
| `set_group_admin` | `group_id`, `user_id`, `enable` | 设置/取消管理 | `Member.modifyAdmin` |
| `set_group_card` | `group_id`, `user_id`, `card` | 群名片 | `Member.nameCard` |
| `set_group_name` | `group_id`, `group_name` | 群名 | `Group.settings` |
| `get_group_info` / `get_group_list` | `group_id` | 群信息/群列表 | `Bot.getGroupList` |
| `get_group_member_info` / `get_group_member_list` | `group_id`, `user_id` | 群成员 | `Group.members` |

### 3.3 好友 / 账号
| API | 参数 | 说明 | mirai 对照 |
|-----|------|------|-----------|
| `get_friend_list` | — | 好友列表 | `Bot.getFriendList` |
| `get_login_info` | — | 登录账号信息 | `Bot.id`/`nick` |
| `get_stranger_info` | `user_id` | 陌生人信息 | `Bot.getStranger` |

### 3.4 文件 / 其它
| API | 说明 | mirai 对照 |
|-----|------|-----------|
| `upload_group_file` / `upload_private_file` | 上传文件 | `File` 相关 |
| `get_file` | 取文件信息 | — |
| `ocr_image` | 图片 OCR | — |
| `can_send_image` / `can_send_record` | 能力检查 | — |

### 3.5 NapCat 扩展 API(非标准 OneBot)
> 参考: https://napneko.github.io/develop/api

| API | 说明 |
|-----|------|
| `set_group_sign` | 群签到 |
| `get_recent_contact` | 最近联系人 |
| `send_poke` | 戳一戳 |
| `nc_get_packet_status` | PacketServer 状态 |
| `get_group_shut_list` | 群禁言用户列表 |
| `get_mini_app_ark` | 小程序卡片 |
| `get_ai_record` / `get_ai_characters` | AI 语音 |
| `ArkSharePeer` / `ArkShareGroup` | 分享联系人/群 |

---

## 4. 事件(Event)类型

### 4.1 消息事件 `post_type=message`
| 事件 | 关键字段 | mirai 对照 |
|------|---------|-----------|
| 群消息 | `message_type=group`, `group_id`, `user_id`, `message`, `raw_message`, `sender` | `GroupMessageEvent` |
| 私聊消息 | `message_type=private`, `user_id`, `message` | `FriendMessageEvent` |
| 群临时会话 | `message_type=group` + `sub_type=anonymous` 等 | — |

### 4.2 通知事件 `post_type=notice`
| 事件 `notice_type` | 说明 | mirai 对照 |
|------|------|-----------|
| `group_increase` | 有人入群 | `MemberJoinEvent` |
| `group_decrease` | 有人退群 | `MemberLeaveEvent` |
| `group_ban` | 群禁言(含成员被禁/解禁) | `MemberMuteEvent` / `MemberUnmuteEvent` |
| `group_admin` | 群管理变动 | `MemberPermissionChangeEvent` |
| `group_recall` | 群消息撤回 | `MessageRecallEvent` |
| `friend_recall` | 好友消息撤回 | — |
| `group_card` | 群名片变更 | `MemberCardChangeEvent` |
| `friend_add` | 好友添加 | `FriendAddEvent` |

### 4.3 请求事件 `post_type=request`
| 事件 `request_type` | 说明 | mirai 对照 |
|------|------|-----------|
| `friend` | 好友请求 | `NewFriendRequestEvent` |
| `group` | 加群请求/邀请 | `MemberJoinRequestEvent` |

> NoneBot 中用 `nonebot.adapters.onebot.v11` 的 `GroupRequestEvent` 等类型接收,自动处理 `approve`/`reject`。

### 4.4 元事件 `post_type=meta_event`
| 事件 `meta_event_type` | 说明 |
|------|------|
| `lifecycle` | 生命周期(enable/disable/connect) |
| `heartbeat` | 心跳 |

---

## 5. 消息段(CQ 消息段)

消息既可用字符串(CQ 码)也可用数组表示;NapCat 默认 `messagePostFormat` 可配 `string`/`array`。NoneBot 适配器建议用 **数组** 形式。

### 5.1 常用消息段(数组形式)
```jsonc
[
  { "type": "text",    "data": { "text": "你好" } },
  { "type": "at",      "data": { "qq": "123456" } },        // @某人 (all=全体)
  { "type": "face",    "data": { "id": "178" } },            // QQ 表情
  { "type": "image",   "data": { "file": "https://.../a.png" } }, // 图片(网络/本地/文件路径)
  { "type": "record",  "data": { "file": "..." } },          // 语音
  { "type": "video",   "data": { "file": "..." } },          // 视频
  { "type": "reply",   "data": { "id": "12345" } },          // 引用回复
  { "type": "json",    "data": { "data": "{...}" } },        // JSON 卡片
  { "type": "forward", "data": { "id": "..." } }             // 合并转发
]
```

### 5.2 与 mirai 消息元素对照
| OneBot 消息段 | mirai 消息元素 |
|--------------|----------------|
| `text` | `PlainText` |
| `at` | `At` |
| `face` | `Face` |
| `image` | `Image` / `Image.fromId` |
| `reply` | `QuoteReply` |
| `record` | `Voice` |
| `json` | `ServiceMessage` / 富文本卡片 |

### 5.3 image 的 file 字段取值
- 本地绝对路径 / 相对路径 / `file://`
- 网络 URL
- base64(`base64://...`)
- NapCat 还可传图片缓存文件名(`Image.queryUrl` 取原图 URL)

---

## 6. 事件→API 典型闭环示例(NoneBot)

```python
from nonebot import on_message
from nonebot.adapters.onebot.v11 import Bot, GroupMessageEvent

anti = on_message(priority=5)

@anti.handle()
async def _(bot: Bot, event: GroupMessageEvent):
    # 撤回可疑消息
    await bot.delete_msg(message_id=event.message_id)
    # 发送警告
    await bot.send_group_msg(group_id=event.group_id, message="已撤回可疑发言")
```

---

## 7. 迁移速查: mirai 概念 → OneBot11

| mirai | OneBot11 |
|-------|----------|
| `GroupMessageEvent` | `post_type=message` + `message_type=group` |
| `MemberJoinEvent` | `post_type=notice` + `notice_type=group_increase` |
| `event.getSubject().sendMessage(msg)` | `send_group_msg(group_id, message)` |
| `MessageSource.recall` | `delete_msg(message_id)` |
| `At(member).plus(PlainText)` | `[at, text]` 消息段数组 |
| `Contact.sendMessage(Image)` | `image` 消息段 |
| `PermissionUtil`(超管/群主/管理) | `event.sender.role` + 自定义依赖注入 |
| `AutoSavePluginConfig`(yml) | Pydantic 配置 + `nonebot-plugin-localstore` |
| `GlobalEventChannel.subscribe` | `on_message` / `on_notice` / `on_request` 装饰器 |
| 群白名单 `groupWhiteList` | `nonebot_plugin_localstore` 或数据库存 JSON |

---

## 8. 参考链接
- OneBot11 标准: https://11.onebot.dev
- NapCat API 文档: https://napneko.github.io/onebot/api
- NapCat 请求接口兼容情况: https://napneko.github.io/develop/api
- NoneBot OneBot V11 适配器 API: https://nonebot.dev/docs/advanced/adapter
