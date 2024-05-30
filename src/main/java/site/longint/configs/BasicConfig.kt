package site.longint.configs

import net.mamoe.mirai.console.data.*
import site.longint.configs.QAConfig.provideDelegate

//object BasicConfig : AutoSavePluginConfig("BasicConfig"){
//    val value1 by value<String>() // 推断为 Int
//}

object BasicConfig : AutoSavePluginConfig("BasicConfig") { // 文件名为 BasicConfig, 会被保存为 BasicConfig.yml
    @ValueDescription("superAdmin: 超级管理账号")
    val superAdmin: Long by value()
//    @ValueDescription("databaseURL: 数据库配置")
//    val databaseURL: String by value("")
    @ValueDescription("functionLoad: 功能可选启用(全局生效)，true为开启，false为关闭")
    var functionLoad: Map<String,Boolean> by value()
    @ValueDescription("groupEnable: 群基础监听，true为开启，false为关闭")
    var groupEnable: Map<Long,Boolean> by value()
    @ValueDescription("enableScripts: 群启用台词")
    val groupEnableScripts: Map<Long,String> by value()
    @ValueDescription("disableScripts: 群禁用台词")
    val groupDisableScripts: Map<Long,String> by value()
    @ValueDescription("groupWhiteList: 群内启用功能权限细分，0为不启用, 1仅自己(memberpermission==0为普通群员)")
    var groupWhiteList: Map<Long,Map<String,Int>> by value()
    @ValueDescription("webdriverPath: 无头浏览器驱动文件地址")
    val webdriverPath: String by value("F:\\AboutPython\\Lib\\site-packages\\chromedriver\\chromedriver.exe")
}