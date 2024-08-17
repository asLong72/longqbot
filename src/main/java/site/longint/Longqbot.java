package site.longint;

import net.mamoe.mirai.console.command.Command;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.data.AutoSavePluginConfig;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.apache.commons.exec.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import site.longint.command.DailyNewsCommand;
import site.longint.command.QACommand;
import site.longint.configs.*;
import site.longint.controller.Controller;
import site.longint.controller.impl.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public final class Longqbot extends JavaPlugin {
    public static final Longqbot INSTANCE = new Longqbot();

    static ArrayList<Command> commands = new ArrayList<>();
    static LinkedHashMap<String, Controller> funcs = new LinkedHashMap<>();
    static LinkedHashMap<String, AutoSavePluginConfig> configs = new LinkedHashMap<>();
    static LinkedHashMap<Long,LinkedHashMap<Long, Integer>> groupMemberState = new LinkedHashMap<>();

    static public String relativeDataFolderPath = "";

    private Longqbot() {
        // @/src/main/resources
        // https://mirai.mamoe.net/topic/1669/%E6%89%BE%E4%B8%8D%E5%88%B0plugin-yml%E7%9A%84%E9%97%AE%E9%A2%98/20
//        super(JvmPluginDescription.loadFromResource());
        //
        super(new JvmPluginDescriptionBuilder("site.longint.longqbot", "1.1.17")
                .name("longqbot")
                .author("longint72")
                .build());
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {
        super.onLoad($this$onLoad);
    }

    @Override
    public void onEnable() {
        Longqbot.INSTANCE.getLogger().info("Longqbot plugin on loading");

        // basic path check
        // workdir: Disk:/a/b/c
        // DataFolderPath: Disk:/a/b/c/data/xxx
        // relative: data/xxx
        if(System.getProperty("os.name").toUpperCase().contains("WIN")){
            relativeDataFolderPath = Longqbot.INSTANCE.getDataFolderPath().toString().replace(Paths.get("").toAbsolutePath().toString()+"\\", "");
            System.setProperty("webdriver.chrome.driver", "F:/AboutPython/Lib/site-packages/chromedriver/chromedriver.exe");
        }else{
            relativeDataFolderPath = Longqbot.INSTANCE.getDataFolderPath().toString().replace(Paths.get("").toAbsolutePath().toString()+"/", "");
            System.setProperty("webdriver.chrome.driver", "F:/AboutPython/Lib/site-packages/chromedriver/chromedriver.exe");
        }
        //
        String path = "";
        path = Longqbot.INSTANCE.getDataFolderPath() + "/img";
        File file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            Longqbot.INSTANCE.getLogger().warning("img folder not exist");
            file.mkdir();
        }
        //
        path = Longqbot.INSTANCE.getDataFolderPath() + "/temp";
        file = new File(path);
        if (!file.exists() && !file.isDirectory()) {
            Longqbot.INSTANCE.getLogger().warning("temp folder not exist");
            file.mkdir();
        }

        // config load
        Longqbot.INSTANCE.getLogger().info("loading configFiles");
        //1.0
        configs.put("基础",BasicConfig.INSTANCE);
        configs.put("问答",QAConfig.INSTANCE);
        configs.put("lol",LOLConfig.INSTANCE);
        configs.put("经典", QuotationConfig.INSTANCE);
        //1.1
        configs.put("欢迎", WelcomeConfig.INSTANCE);
        //1.2
        configs.put("反广告", AdAntiConfig.INSTANCE);
        for (AutoSavePluginConfig cfg : configs.values()) {
            INSTANCE.reloadPluginConfig(cfg);
        }

        // command register
//        Longqbot.INSTANCE.getLogger().info("commands loading");
//        commands.add(QACommand.INSTANCE);
//        commands.add(DailyNewsCommand.INSTANCE);
//        for (Command cmd : commands) {
//            CommandManager.INSTANCE.registerCommand(cmd, false);
//        }

        // enable controller
        Longqbot.INSTANCE.getLogger().info("controllers loading");
        // funcs.put(Keyword, Controller INSTANCE);
        // 1.0
        funcs.put(QAController.INSTANCE.getKeyword(), QAController.INSTANCE);
        funcs.put(LOLController.INSTANCE.getKeyword(), LOLController.INSTANCE);
        funcs.put(QuotationsController.INSTANCE.getKeyword(), QuotationsController.INSTANCE);
        // 1.1
        funcs.put(WelcomeController.INSTANCE.getKeyword(), WelcomeController.INSTANCE);
        // 1.2
        funcs.put(AdAntiController.INSTANCE.getKeyword(), AdAntiController.INSTANCE);
        //
        LinkedHashMap<String, Boolean> funcsState = new LinkedHashMap<>(BasicConfig.INSTANCE.getFunctionLoad());
        for (String funcKeyword : funcs.keySet()) {
            funcs.get(funcKeyword).setEnable(funcsState.getOrDefault(funcKeyword, false));
        }

        // eventListener
        GlobalEventChannel.INSTANCE.parentScope(INSTANCE).subscribeAlways(GroupMessageEvent.class, this::GroupMSGListener);
        GlobalEventChannel.INSTANCE.parentScope(INSTANCE).subscribeAlways(MemberJoinEvent.class, this::GroupJoinListener);

        // finish!
        Longqbot.INSTANCE.getLogger().info("plugin loaded");
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
        Longqbot.INSTANCE.getLogger().info("plugin disabled with saving");
    }

    void GroupJoinListener(MemberJoinEvent event){
        if (BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getGroupId(), false)) {
            String args[] = {"欢迎", "欢迎"};
            callFunc(event, args);
//            MessageChain chain = event.getMessage(); // 可获取到消息内容等, 详细查阅 `GroupMessageEvent`
        }
    }

    void GroupMSGListener(GroupMessageEvent event) {
        MessageChain chain = event.getMessage(); // 可获取到消息内容等, 详细查阅 `GroupMessageEvent`
//         getLogger().info(chain.contentToString());
        // 待机状态
        String msg = chain.contentToString();
        if (BasicConfig.INSTANCE.getGroupEnable().getOrDefault(event.getSubject().getId(), false)) {
//            event.getSubject().sendMessage("Hello!"); // 回复消息
            String[] args = {"欢迎", "冷却"};
            callFunc(event,args);

            char first = msg.charAt(0);
//            char last = msg.charAt(msg.length()-1);
            if(first == '#'||first == '＃'){
                if(first == '#'){
                    msg = msg.replaceFirst("#", "");
                }else{
                    msg = msg.replaceFirst("＃", "");
                }
                args = msg.split(" ");
//                Longqbot.INSTANCE.getLogger().warning(args[0]);
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
                                if(state<1||state>4){
                                    throw new NumberFormatException();
                                }
                            }catch(NumberFormatException e){
                                event.getSubject().sendMessage(String.format("数值异常: %s", args[2]));
                                return;
                            }
                        }

                        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null){
                            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
                        }
                        BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).put(args[1],state);

                        String report = args[1] + "功能已启用";
                        if(state==1){
                            report += ", 部分功能权限模式为: 仅允许 bot超级管理 \\ 群白名单";
                        }else if(state==2){
                            report += ", 部分功能权限模式为: 仅允许 bot超级管理 \\ 群白名单 \\ 群主";
                        }else if(state==3){
                            report += ", 部分功能权限模式为: 仅允许 bot超级管理 \\ 群白名单 \\ 管理员及更高";
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
                //
                if(msg.equals("列表"))
                {
                    args = new String[]{"问答", msg};
                    callFunc(event,args);
                }
                else if(msg.equals("取消"))
                {
                    return;
                }
                else if(QAConfig.INSTANCE.getQaMap().getOrDefault(msg, null)!=null)
                {
                    args = new String[]{"问答", msg};
                    callFunc(event,args);
                }
                else if(first=='('||first=='（')
                {
                    char last = msg.charAt(msg.length()-1);
//                    Longqbot.INSTANCE.getLogger().warning(String.format("%c", last));
                    if(last==')'||last=='）'){
                        // (
//                        long count=0;
//                        count = msg.chars().filter(ch -> (ch=='('||ch=='（')).count();
                        // from a to behind b
                        msg = msg.substring(1, msg.length()-1);
//                        Longqbot.INSTANCE.getLogger().warning(msg);
                        if(!msg.matches("[(（）)]")){
                            // ?指令关键词冲突
                            msg = "经典 " + msg;
//                            Longqbot.INSTANCE.getLogger().warning(msg);
                            args = msg.split(" ");
                            List<String> tips = new ArrayList<>();
                            tips = QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(),null);
                            if(tips!=null){
                                if(args.length>1 && args[1].length()!=0 && tips.contains(args[1])){
                                    callFunc(event,args);
                                }
                            }
                        }
                    }
                }
            }
        }
        // 超级管理指令处理
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

    /* 在此callFunc函数内先剔除功能名参数, 再传入功能调用 */
    Boolean callFunc(Event event, String[] args){
//        Longqbot.INSTANCE.getLogger().warning(args[0]);

        Long subjectID = null;
        if(event instanceof GroupMessageEvent){
            subjectID = ((GroupMessageEvent)event).getSubject().getId();
        }else if(event instanceof MemberJoinEvent){
            subjectID = ((MemberJoinEvent)event).getGroupId();
        }

        if (event instanceof GroupMessageEvent && BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(((GroupMessageEvent)event).getSubject().getId(),null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(((GroupMessageEvent)event).getSubject().getId(), new LinkedHashMap<>());
        }

        Controller funcOnCall = funcs.getOrDefault(args[0], null);
//        Longqbot.INSTANCE.getLogger().warning("func name: " + args[0]);
        // && BasicConfig.INSTANCE.getFunctionLoad().getOrDefault(args[0],true)
        if(funcOnCall!=null && BasicConfig.INSTANCE.getGroupWhiteList().get(subjectID).getOrDefault(args[0], 0)>0){
            // !!!
            args = Arrays.copyOfRange(args, 1, args.length);
            funcOnCall.onCall(event, args);
            return Boolean.TRUE;
        }else {
//            Longqbot.INSTANCE.getLogger().warning("func is null, func name: " + args[0]);
            return Boolean.FALSE;
        }
    }
}