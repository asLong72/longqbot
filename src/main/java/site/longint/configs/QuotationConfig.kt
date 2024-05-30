package site.longint.configs

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import site.longint.DAO.ImageIndicator
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

object QuotationConfig : AutoSavePluginConfig("QuotationConfig")  {
    //    @ValueDescription("test: 自定义java数据类")
//    var test: ImageIndicatorDO by value()
    // 自定义java数据类为什么会报错
    @ValueDescription("funcsDiscription: 子功能描述")
    var funcsDiscription: Map<String, String> by value()
    @ValueDescription("QuotationsMap: 索引表")
    var QuotationsMap: Map<String, Map<String, ImageIndicator>> by value()
    @ValueDescription("tipsAllowinGroup: 群引用")
    var tipsAllowinGroup: Map<Long, Map<String, Boolean>> by value()

}