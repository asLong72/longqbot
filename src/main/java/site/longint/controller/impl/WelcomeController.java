package site.longint.controller.impl;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import site.longint.Longqbot;
import site.longint.configs.BasicConfig;
import site.longint.configs.QAConfig;
import site.longint.controller.Controller;
import site.longint.utils.PermissionUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class WelcomeController extends Controller {
    public static final WelcomeController INSTANCE = new WelcomeController();

    public WelcomeController() {
        super("欢迎", false);
    }

    void register() {
        subFuncs = new LinkedHashMap<>();

    }

    @Override
    public void onCall(GroupMessageEvent event, String[] args) {
        if (subFuncs == null) {
            Longqbot.INSTANCE.getLogger().warning(keyword + ": subFuncs is null");
            register();
        }


    }

    @Override
    public void info(GroupMessageEvent event, String[] args){

    }

    void edit(GroupMessageEvent event, String[] args) {
        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if(args.length >= 2){
            args = Arrays.copyOfRange(args, 1, args.length);
            String anwser = String.join("",args);

//            QAConfig.INSTANCE.getQaMap().put(qustion, anwser);
//            event.getSubject().sendMessage("修改成功"); // 回复消息s
        }
        else
        {
            event.getSubject().sendMessage("格式: #欢迎 修改 新欢迎词"); // 回复消息
        }
    }
}
