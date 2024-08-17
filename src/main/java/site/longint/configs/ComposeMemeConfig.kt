package site.longint.configs;

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import site.longint.configs.LOLConfig.provideDelegate

object ComposeMemeConfig : AutoSavePluginConfig("ComposeMemeConfig") {
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()

}