package site.longint;

import net.mamoe.mirai.console.command.Command;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.data.AutoSavePluginConfig;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import site.longint.command.DailyNewsCommand;
import site.longint.command.QACommand;
import site.longint.configs.BasicConfig;
import site.longint.configs.LOLConfig;
import site.longint.configs.QAConfig;
import site.longint.configs.QuotationConfig;
import site.longint.controller.Controller;
import site.longint.controller.impl.LOLController;
import site.longint.controller.impl.QAController;
import site.longint.controller.impl.QuotationsController;

import java.io.File;
import java.util.*;

public final class Longqbot extends JavaPlugin {
    public static final Longqbot INSTANCE = new Longqbot();

    static ArrayList<Command> commands = new ArrayList<>();
    static LinkedHashMap<String, Controller> funcs = new LinkedHashMap<>();
    static LinkedHashMap<String, AutoSavePluginConfig> configs = new LinkedHashMap<>();
    static LinkedHashMap<Long,LinkedHashMap<Long, Integer>> groupMemberState = new LinkedHashMap<>();

    private Longqbot() {
//        super(JvmPluginDescription.loadFromResource());
        super(new JvmPluginDescriptionBuilder("site.longint.longqbot", "1.0-SNAPSHOT")
                .name("longqbot")
                .author("longint72")
                .build());
    }

    @Override
    public void onEnable() {
        getLogger().info("plugin on loading");

        // basic folder check
        String path = Longqbot.INSTANCE.getDataFolderPath() + "/img";
        File file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            Longqbot.INSTANCE.getLogger().warning("img folder not exist");
            file.mkdir();
        }
        path = Longqbot.INSTANCE.getDataFolderPath() + "/temp";
        file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            Longqbot.INSTANCE.getLogger().warning("temp folder not exist");
            file.mkdir();
        }

        // config load
        getLogger().info("loading configFiles");
        configs.put("基础",BasicConfig.INSTANCE);
        configs.put("问答",QAConfig.INSTANCE);
        configs.put("lol",LOLConfig.INSTANCE);
        configs.put("经典", QuotationConfig.INSTANCE);
        for (AutoSavePluginConfig cfg : configs.values()) {
            INSTANCE.reloadPluginConfig(cfg);
        }

        // command register
        getLogger().info("commands loading");
        commands.add(QACommand.INSTANCE);
        commands.add(DailyNewsCommand.INSTANCE);
        for (Command cmd : commands) {
            CommandManager.INSTANCE.registerCommand(cmd, false);
        }

        // enable controller
        getLogger().info("controllers loading");
        funcs.put(QAController.INSTANCE.getKeyword(), QAController.INSTANCE);
        funcs.put(LOLController.INSTANCE.getKeyword(), LOLController.INSTANCE);
        funcs.put(QuotationsController.INSTANCE.getKeyword(), QuotationsController.INSTANCE);
        LinkedHashMap<String, Boolean> funcsState = new LinkedHashMap<>(BasicConfig.INSTANCE.getFunctionLoad());
        for (String funcKeyword : funcs.keySet()) {
            funcs.get(funcKeyword).setEnable(funcsState.getOrDefault(funcKeyword, false));
        }

        // eventListener
        GlobalEventChannel.INSTANCE.parentScope(INSTANCE).subscribeAlways(GroupMessageEvent.class, this::GroupMSGListener);
        GlobalEventChannel.INSTANCE.parentScope(INSTANCE).subscribeAlways(MemberJoinEvent.class, this::GroupJoinListener);

        // finish!
        getLogger().info("plugin loaded");
    }

    @Override
    public void onDisable() {
        LinkedHashMap<String, Boolean> funcsState = new LinkedHashMap<>();
        for (String funcKeyword : funcs.keySet()) {
            funcsState.put(funcKeyword, funcs.get(funcKeyword).getEnable());
        }
        BasicConfig.INSTANCE.setFunctionLoad(funcsState);
        for (AutoSavePluginConfig cfg : configs.values()) {
            INSTANCE.savePluginConfig(cfg);
        }
        getLogger().info("plugin disabled with saving");
    }

    void GroupJoinListener(MemberJoinEvent event){
        if (BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getGroupId(), false)) {
//            MessageChain chain = event.getMessage(); // 可获取到消息内容等, 详细查阅 `GroupMessageEvent`
            if(BasicConfig.INSTANCE.getGroupWelcomeScripts().getOrDefault(event.getGroupId(),null)==null){
                BasicConfig.INSTANCE.getGroupWelcomeScripts().put(event.getGroupId(), "\n欢迎进群, 输入 #帮助 获得本QQbot在本群的功能启用情况与使用说明");
            }
            event.getGroup().sendMessage(new At(event.getMember().getId()).plus(new PlainText(BasicConfig.INSTANCE.getGroupWelcomeScripts().get(event.getGroupId()))));
        }
    }

    void GroupMSGListener(GroupMessageEvent event) {
        MessageChain chain = event.getMessage(); // 可获取到消息内容等, 详细查阅 `GroupMessageEvent`
//         getLogger().info(chain.contentToString());
        // 待机状态
        String msg = chain.contentToString();
        if (BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getSubject().getId(), false)) {
//            event.getSubject().sendMessage("Hello!"); // 回复消息
            char first = msg.charAt(0);
            if(first == '#'||first == '＃'){
                if(first == '#'){
                    msg = msg.replaceFirst("#", "");
                }else{
                    msg = msg.replaceFirst("＃", "");
                }
                String[] args = msg.split(" ");
                if(callFunc(event,args)==Boolean.FALSE && args.length == 1)
                {
                    if(msg.equals("帮助"))
                    {
                        String report = "本群以下功能已启用: ";
                        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null){
                            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
                        }
                        for(String funcName: funcs.keySet())
                        {
                            if(BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(funcName, 0) > 0)
                            {
                                report += "\n" + funcName;
                            }
                        }
                        report += "\n\n发送 #功能名 获取对应功能的详细介绍";
                        event.getSubject().sendMessage(report);
                    }
                    else
                    {
                        // 快捷问答列表
                        try{
                            Integer id = Integer.valueOf(args[0]);
                            if(id<0)
                            {
                                throw new NumberFormatException();
                            }
                            args = new String[]{"问答", msg};
                            callFunc(event,args);
                        }catch (java.lang.NumberFormatException e){
                            Longqbot.INSTANCE.getLogger().warning(String.format("数值异常: %s", args[0]));
                        }
                    }
                }
                else
                {
                    // 群内功能启用
                    if((args.length == 2||args.length == 3)&&args[0].equals("启用")&&event.getSender().getId()==BasicConfig.INSTANCE.getSuperAdmin())
                    {
                        if(funcs.getOrDefault(args[1], null) == null){
                            event.getSubject().sendMessage(String.format("%s功能不存在", args[1]));
                            return;
                        }

                        Integer state = 1;
                        if(args.length == 3){
                            try{
                                state = Integer.valueOf(args[2]);
                                if(state<1||state>3){
                                    throw new NumberFormatException();
                                }
                            }catch(NumberFormatException e){
                                event.getSubject().sendMessage(String.format("数值异常: %s", args[2]));
                                return;
                            }
                        } else {
                            state = 1;
                        }
                        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null){
                            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
                        }
                        BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).put(args[1],state);

                        String report = args[1] + "功能已启用";
                        if(state==1){
                            report += ", 部分功能权限模式为: 仅允许 bot超级管理 \\ 群白名单";
                        }else if(state==2){
                            report += ", 部分功能权限模式为: 仅允许 群主 \\ bot超级管理 \\ 群白名单";
                        }else if(state==3){
                            report += ", 部分功能权限模式为: 仅允许 管理员及更高 \\ bot超级管理 \\ 群白名单";
                        }else if(state==4){
                            report += ", 部分功能权限模式为: 全体成员";
                        }
                        event.getSubject().sendMessage(report);
                    }
                    else if(args.length == 2 && args[0].equals("禁用"))
                    {
                        //
                        if(funcs.getOrDefault(args[1], null) == null){
                            event.getSubject().sendMessage(String.format("%s功能不存在", args[1]));
                        }else{
                            if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null){
                                BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
                            }
                            BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).put(args[1],0);

                            event.getSubject().sendMessage(String.format("%s功能已禁用", args[1]));
                        }
                    }
                }

                return;
            }
            else
            {
                if(msg.equals("列表"))
                {
                    String[] args = {"问答", msg};
                    callFunc(event,args);
                }
                else if(msg.equals("取消"))
                {
                    return;
                }
                else if(QAConfig.INSTANCE.getQaMap().getOrDefault(msg, null)!=null)
                {
                    String[] args = {"问答", msg};
                    callFunc(event,args);
                }
            }
        }
        //
        if (event.getSender().getId() == BasicConfig.INSTANCE.getSuperAdmin()) {
            if (msg.equals("!原神!") && !BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getSubject().getId(), false)) {
                BasicConfig.INSTANCE.getGroupEnable().put(event.getSubject().getId(), true);
                event.getSubject().sendMessage("启动!");

                String enableScript = BasicConfig.INSTANCE.getGroupEnableScripts().getOrDefault(event.getSender().getId(),null);
                if(enableScript!=null){
                    event.getSubject().sendMessage(enableScript);
                }
            } else if (msg.equals("!黑暗!") && BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getSubject().getId(), false)) {
                BasicConfig.INSTANCE.getGroupEnable().replace(event.getSubject().getId(), false);
                event.getSubject().sendMessage("降临!");

                String disableScript = BasicConfig.INSTANCE.getGroupDisableScripts().getOrDefault(event.getSender().getId(),null);
                if(disableScript!=null){
                    event.getSubject().sendMessage(disableScript);
                }
            }
        }
    }

    Boolean callFunc(GroupMessageEvent event, String[] args){
//        Longqbot.INSTANCE.getLogger().warning(args[0]);

        if (BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(),null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }

        Controller funcOnCall = funcs.getOrDefault(args[0], null);
        // && BasicConfig.INSTANCE.getFunctionLoad().getOrDefault(args[0],true)
        if(funcOnCall!=null && BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(args[0], 0)>0){
            args = Arrays.copyOfRange(args, 1, args.length);
            funcOnCall.onCall(event, args);
            return Boolean.TRUE;
        }else {
            return Boolean.FALSE;
        }
    }
}