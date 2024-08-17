package site.longint.controller.impl;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.PlainText;
import site.longint.Longqbot;
import site.longint.configs.BasicConfig;
import site.longint.configs.WelcomeConfig;
import site.longint.controller.Controller;
import site.longint.utils.MethodPointerUtil;
import site.longint.utils.PermissionUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class WelcomeController extends Controller {
    public static final WelcomeController INSTANCE = new WelcomeController();

    public LinkedHashMap<Long, Integer> coolDownCount;

    public WelcomeController() {
        super("欢迎", false);
    }

    @Override
    protected void register() {
        subFuncs = new LinkedHashMap<>();
        try{
            subFuncs.put("功能介绍", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "info", Event.class, String[].class));
            subFuncs.put("修改", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "setState", GroupMessageEvent.class, String[].class));
            subFuncs.put("冷却", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "coolDown", Event.class, String[].class));
            subFuncs.put("状态", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "state", GroupMessageEvent.class, String[].class));
            subFuncs.put("欢迎", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "welcome", MemberJoinEvent.class, String[].class));

//            coolDownCount = new LinkedHashMap<>(coolDownTarget);
            // 重启插件时, 冷却cd重置
            coolDownCount = new LinkedHashMap<>();
            for (Long groupID : WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().keySet()) {
                Integer re_cd = WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().get(groupID)/10;
                coolDownCount.put(groupID, re_cd);
            }
        }catch (Exception e){
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
    }

    @Override
    public void onCall(Event event, String[] args) {
        if (subFuncs == null) {
            Longqbot.INSTANCE.getLogger().warning(keyword + ": subFuncs is null");
            register();
        }

        //
        Long groupID = 0L;
        if(event instanceof GroupMessageEvent){
            groupID = ((GroupMessageEvent)event).getSubject().getId();
        }else if(event instanceof MemberJoinEvent){
            groupID = ((MemberJoinEvent)event).getGroupId();
        }
        //
        if(WelcomeConfig.INSTANCE.getGroupWelcomeScripts().getOrDefault(groupID,null)==null){
            WelcomeConfig.INSTANCE.getGroupWelcomeScripts().put(groupID, "\n欢迎进群, 输入 #帮助 获得本QQbot在本群的功能启用情况与使用说明");
        }
        //
        if(WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().getOrDefault(groupID,null)==null){
            WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().put(groupID,0);
            coolDownCount.put(groupID, 0);
        }

        if (args != null && args.length != 0) {
//            Longqbot.INSTANCE.getLogger().info(args[0]);

            Method subFunc = subFuncs.getOrDefault(args[0], null);
            if (subFunc != null)
            {
                if (usersSpecialState == null) {
                    Longqbot.INSTANCE.getLogger().warning(keyword + ": usersSpecialState is null");
                    usersSpecialState = new HashMap<>();
                }
                try {
                    subFunc.invoke(INSTANCE, event, args);
                } catch (Exception e) {
                    Longqbot.INSTANCE.getLogger().error(e.toString());
                }
            }
        } else {
            info(event, args);
        }
    }

    @Override
    public void info(Event event, String[] args){
        //
        String help = WelcomeController.INSTANCE.getKeyword() + "功能介绍: ";
        for (String subFuncName : subFuncs.keySet()) {
            String discription;
            //
            if(WelcomeConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                discription = "暂无详细描述";
                //
                WelcomeConfig.INSTANCE.getFuncsDiscription().put(subFuncName, discription);
            }else{
                //
                discription = WelcomeConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, "");
            }
            help += "\n" + subFuncName + ": " + discription;
        }
        ((GroupMessageEvent)event).getSubject().sendMessage(help); // 回复消息
    }

    // 修改欢迎词或冷却
    void setState(GroupMessageEvent event, String[] args) {
        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }

        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if(args.length >= 2) {
//            Longqbot.INSTANCE.getLogger().warning(args[2]);
            if (args[1].equals("欢迎词")) {
                if(args.length >= 3){
                    args = Arrays.copyOfRange(args, 2, args.length);
                    // 空格在切割参数的时候被去掉了, 在拼接时需要补上
                    String anwser = String.join(" ",args);
                    WelcomeConfig.INSTANCE.getGroupWelcomeScripts().put(event.getSubject().getId(), anwser);
                    event.getSubject().sendMessage("修改成功"); // 回复消息
                }
                else
                {
                    event.getSubject().sendMessage("格式: #欢迎 修改 欢迎词 <新欢迎词>"); // 回复消息
                }
            } else if (args[1].equals("冷却")) {
                if (args.length == 3) {
                    if (WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().getOrDefault(event.getSubject().getId(), null) == null) {
                        WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().put(event.getSubject().getId(), 0);
                    }
                    Integer cool;
                    try {
                        cool = Integer.parseInt(args[2]);
                    } catch (Exception e) {
                        event.getSubject().sendMessage("参数异常"); // 回复消息
//                event.getSubject().sendMessage("\n\n\n"); // 回复消息
                        return;
                    }

                    Long groupID = event.getSubject().getId();
                    WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().put(groupID, cool);
                    // 不伴随本次修改重置冷却cd
//                    coolDownCount.put(groupID, cool);
                    if(coolDownCount.getOrDefault(groupID, -1)<0){
                        coolDownCount.put(groupID, cool);
                    }
                    event.getSubject().sendMessage(String.format("修改成功, 当前cd区间为: 0 ~ %d", cool)); // 回复消息
                } else {
                    event.getSubject().sendMessage("格式: #欢迎 修改 冷却 <冷却区间最大值>"); // 回复消息
                }
            }
        }else{
            event.getSubject().sendMessage("格式: \n#欢迎 修改 欢迎词 <新欢迎词>\n或\n#欢迎 修改 冷却 <冷却区间最大值>");
        }
    }

    // 欢迎词和冷却状态查看
    void state(GroupMessageEvent event, String[] args) {
        String msg = "";
        msg += "当前群聊新成员进群欢迎词: " + WelcomeConfig.INSTANCE.getGroupWelcomeScripts().getOrDefault(event.getSubject().getId(), "") + "\n\n";
        msg += "当前群聊发送欢迎词冷却上限(按群聊消息数计): " + WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().getOrDefault(event.getSubject().getId(), 0) + "\n\n";
        msg += "当前群聊发送欢迎词冷却倒数(按群聊消息数计): " + coolDownCount.getOrDefault(event.getSubject().getId(), 0);

        event.getSubject().sendMessage(new PlainText(msg));
    }

    //
    void coolDown(Event event, String[] args){
        Long groupID = 0L;
        if(event instanceof GroupMessageEvent){
            groupID = ((GroupMessageEvent)event).getSubject().getId();
        }else if(event instanceof MemberJoinEvent){
            groupID = ((MemberJoinEvent)event).getGroupId();
        }
        Integer count = coolDownCount.getOrDefault(groupID,0);
        if(count>0){
            coolDownCount.put(groupID, count-1);
        }
    }

    //
    void welcome(MemberJoinEvent event, String[] args){
        Long groupID = event.getGroupId();
        Integer count = coolDownCount.getOrDefault(groupID,0);
        if(count==0){
            event.getGroup().sendMessage(new At(event.getMember().getId()).plus(new PlainText(WelcomeConfig.INSTANCE.getGroupWelcomeScripts().get(event.getGroupId()))));
            coolDownCount.put(groupID,WelcomeConfig.INSTANCE.getGroupWelcomeCoolDownTarget().getOrDefault(groupID,0));
        } else {
            coolDown(event, args);
        }
    }
}
