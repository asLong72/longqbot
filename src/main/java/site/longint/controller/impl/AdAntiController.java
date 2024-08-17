package site.longint.controller.impl;

import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.PlainText;
import site.longint.Longqbot;
import site.longint.configs.AdAntiConfig;
import site.longint.configs.WelcomeConfig;
import site.longint.controller.Controller;
import site.longint.utils.MassageUtil;
import site.longint.utils.MethodPointerUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class AdAntiController extends Controller {
    public static final AdAntiController INSTANCE = new AdAntiController();

    public AdAntiController() {
        super("反广告", false);
    }

    @Override
    protected void register() {
        subFuncs = new LinkedHashMap<>();
        try{
            subFuncs.put("功能介绍", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "info", Event.class, String[].class));
            subFuncs.put("状态", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "state", GroupMessageEvent.class, String[].class));
            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test", GroupMessageEvent.class, String[].class));
            subFuncs.put("肃反", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "anti", GroupMessageEvent.class, String[].class));
            subFuncs.put("整理", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "sort", GroupMessageEvent.class, String[].class));
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
            else
            {
//                Longqbot.INSTANCE.getLogger().info(args[0]);
                anti((GroupMessageEvent)event, args);
            }
        } else {
            info(event, args);
        }
    }

    @Override
    public void info(Event event, String[] args) {
        //
        String help = AdAntiController.INSTANCE.getKeyword() + "功能介绍: ";
        for (String subFuncName : subFuncs.keySet()) {
            String discription = "暂无详细描述";
            //
            if(AdAntiConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                //
                AdAntiConfig.INSTANCE.getFuncsDiscription().put(subFuncName, discription);
            }else{
                //
                discription = AdAntiConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, "");
            }
            help += "\n" + subFuncName + ": " + discription;
        }
        ((GroupMessageEvent)event).getSubject().sendMessage(help); // 回复消息
    }

    public void state(GroupMessageEvent event, String[] args){

    }

    public void anti(GroupMessageEvent event, String[] args){
        String msg = String.join(" ", args);
        Longqbot.INSTANCE.getLogger().warning(msg);
        if(true){
            try {
                //
                MassageUtil.recall(event);
//                event.getSender().sendMessage(new PlainText("已撤回可疑发言"));
                event.getSubject().sendMessage(new PlainText("已撤回可疑发言"));
            }catch (PermissionDeniedException e){
                event.getSubject().sendMessage(new PlainText("bot账号在本群权限不足, 无法撤回可疑发言, 仅发消息进行警示"));
                Longqbot.INSTANCE.getLogger().warning("群聊权限不足");
            }catch (IllegalStateException e){
                Longqbot.INSTANCE.getLogger().warning("可疑发言已被他人撤回, bot无法处理");
            }
        }
    }

    public void sort(GroupMessageEvent event, String[] args){

    }

    public void test(GroupMessageEvent event, String[] args){
        event.getSender().sendMessage(new PlainText(String.valueOf(event.getPermission().getLevel())));
    }
}
