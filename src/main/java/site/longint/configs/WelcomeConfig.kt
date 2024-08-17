package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object WelcomeConfig: AutoSavePluginConfig("WelcomeConfig") {
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()
    @ValueDescription("disableScripts: 群迎新台词")
    val groupWelcomeScripts: Map<Long,String> by value()
    @ValueDescription("disableScripts: 群迎新cd")
    val groupWelcomeCoolDownTarget: Map<Long,Int> by value()
}