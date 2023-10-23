package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import site.longint.configs.QAConfig.provideDelegate
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

object LOLConfig : AutoSavePluginConfig("LOLConfig") { // 文件名为 BasicConfig, 会被保存为 BasicConfig.yml
    @ValueDescription("heroList: 英雄总数")
    var heroList: Map<Int,Map<String,String>> by value()
    @ValueDescription("groupSet: 群配置")
    var groupSet: Map<Long, Map<String,Boolean>> by value()
}