package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import site.longint.configs.BasicConfig.provideDelegate
import site.longint.configs.LOLConfig.provideDelegate

object AdAntiConfig : AutoSavePluginConfig("AdAntiConfig") { // 文件名为 BasicConfig, 会被保存为 BasicConfig.yml
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()
    @ValueDescription("creditRecord: 账号信用积分")
    var creditRecord: Map<Long, Int> by value()
    @ValueDescription("invitionship: 邀请入群账号联系")
    var invitionship: Map<Long, Long> by value()
}