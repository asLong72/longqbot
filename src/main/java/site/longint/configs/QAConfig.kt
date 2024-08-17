package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import site.longint.configs.BasicConfig.provideDelegate
import java.util.LinkedHashMap
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

object QAConfig : AutoSavePluginConfig("QAConfig") {
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()
    @ValueDescription("tipsMap: 问答条目编号数据")
    var tipsMap: Map<Int, String> by value()
    @ValueDescription("qaMap: 问答问题&答案内容数据")
    var qaMap: Map<String, String> by value()
    @ValueDescription("tipsAllowinGroup: 群引用")
    var tipsAllowinGroup: Map<Long, Map<Int, String>> by value()
}