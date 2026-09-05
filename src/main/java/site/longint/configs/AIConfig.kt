package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * AI 对话功能配置(DeepSeek API)
 *
 * 字段说明见各 @ValueDescription。首次启动后保存为 AIConfig.yml,
 * 其中 apiKey 需在 yml 中手动填写。
 */
object AIConfig : AutoSavePluginConfig("AIConfig") {
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()

    @ValueDescription("apiKey: DeepSeek API 密钥(必填, 在yml中配置)")
    var apiKey: String by value()

    @ValueDescription("apiUrl: DeepSeek API 地址(OpenAI兼容)")
    var apiUrl: String by value("https://api.deepseek.com/chat/completions")

    @ValueDescription("model: DeepSeek 模型名")
    var model: String by value("deepseek-chat")

    @ValueDescription("coolDownMinSeconds: AI触发冷却下限(秒), 冷却=5*log10(bot上次回复该用户内容长度), 不低于此值(默认5)")
    var coolDownMinSeconds: Int by value(5)

    @ValueDescription("coolDownMaxSeconds: AI触发冷却上限(秒, 默认30)")
    var coolDownMaxSeconds: Int by value(30)

    @ValueDescription("forbiddenWords: 对话违禁词集合(子串命中即违规)")
    var forbiddenWords: MutableList<String> by value()

    @ValueDescription("forbiddenRegex: 对话违禁正则集合(正则命中即违规, 可识别变体)")
    var forbiddenRegex: MutableList<String> by value()

    @ValueDescription("adFeatures: 广告甄别特征(子串或正则, 命中则拦截不调用AI)")
    var adFeatures: MutableList<String> by value()

    @ValueDescription("bannedReply: 违禁提示语(用户内容或AI回复命中违禁时发送)")
    var bannedReply: String by value("该内容涉及违规, 已拒绝回复")

    @ValueDescription("adReply: 广告拦截提示语")
    var adReply: String by value("检测到疑似广告内容, 已忽略")

    @ValueDescription("aiName: bot在群内的称呼(用于system提示)")
    var aiName: String by value("小bot")

    @ValueDescription("systemPrompt: AI人设/系统提示(拼接在请求前)")
    var systemPrompt: String by value("你是本QQ群里的AI助手, 回答简洁、友好、口语化。")

    @ValueDescription("globalHistoryMax: 群内全局上下文队列最大条数(默认10)")
    var globalHistoryMax: Int by value(10)

    @ValueDescription("personalHistoryMax: 交互用户的个人上下文队列最大条数(默认10)")
    var personalHistoryMax: Int by value(10)

    @ValueDescription("interactWindowMinutes: 与机器人交互后, 该用户个人队列的活跃窗口(分钟, 默认10)")
    var interactWindowMinutes: Int by value(10)

    @ValueDescription("botSentMax: bot 最近真实发送到QQ的消息队列最大条数(默认10)")
    var botSentMax: Int by value(10)

    @ValueDescription("contextMinutes: 个人发言与bot输出参与最终上下文的最近时间窗口(分钟, 默认5)")
    var contextMinutes: Int by value(5)
}
