package site.longint.controller.impl;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.PlainText;
import site.longint.Longqbot;
import site.longint.configs.BasicConfig;
import site.longint.configs.QAConfig;
import site.longint.controller.Controller;
import site.longint.utils.MethodPointerUtil;
import site.longint.utils.PermissionUtil;

import java.lang.reflect.Method;
import java.util.*;

public class QAController extends Controller {
    public static final QAController INSTANCE = new QAController();

    // <序号, 问题>
    HashMap<Integer, String> QAinEdit = new LinkedHashMap<>();

    QAController() {
        super("问答", false);
    }

    void register() {
        try {
            subFuncs = new LinkedHashMap<>();

//            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test", GroupMessageEvent.class, String[].class));
            subFuncs.put("功能介绍", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "info", GroupMessageEvent.class, String[].class));
            subFuncs.put("添加", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "add", GroupMessageEvent.class, String[].class));
            subFuncs.put("修改", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "edit", GroupMessageEvent.class, String[].class));
            subFuncs.put("删除", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "remove", GroupMessageEvent.class, String[].class));
            subFuncs.put("引用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "reference", GroupMessageEvent.class, String[].class));
            subFuncs.put("禁用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "ban", GroupMessageEvent.class, String[].class));
            subFuncs.put("列表", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "list", GroupMessageEvent.class, String[].class));
            subFuncs.put("条目", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "quest", GroupMessageEvent.class, String[].class));

            for(String subFuncName: subFuncs.keySet()){
                if(QAConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                    QAConfig.INSTANCE.getFuncsDiscription().put(subFuncName, "暂无详细描述");
                }
            }
//                Longqbot.INSTANCE.getLogger().info(String.valueOf(subFuncs.size()));
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e);
        }
    }

    @Override
    public void onCall(GroupMessageEvent event, String[] args) {
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
                    Longqbot.INSTANCE.getLogger().error(e.getMessage());
                }
            }
            else
            {
//                Longqbot.INSTANCE.getLogger().info(args[0]);
                quest(event, args);
            }
        } else {
            String help = QAController.INSTANCE.getKeyword() + "功能介绍: ";
            for (String funcName : subFuncs.keySet()) {
                String discription = QAConfig.INSTANCE.getFuncsDiscription().getOrDefault(funcName, "");
                help += "\n" + funcName + ": " + discription;
            }
            event.getSubject().sendMessage(help); // 回复消息
        }
    }

    @Override
    public void info(GroupMessageEvent event, String[] args){

    }

    public void test(GroupMessageEvent event, String[] args) {
        event.getSubject().sendMessage("测试"); // 回复消息
        event.getSubject().sendMessage("权限: " + event.getSender().getPermission().getLevel()); // 回复消息}
    }

    void add(GroupMessageEvent event, String[] args) {
//        event.getSubject().sendMessage("add"); // 回复消息
//        Longqbot.INSTANCE.getLogger().info(String.valueOf(event.getSender().getId()));
//        Longqbot.INSTANCE.getLogger().info(String.join(" ", args));
//        Longqbot.INSTANCE.getLogger().info(String.valueOf(args.length));

        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if (args.length >= 3) {
//            event.getSubject().sendMessage(String.join(" ", args)); // 回复消息
            // 浅复制对象, 引用对象
            if (QAConfig.INSTANCE.getTipsMap() == null) {
                Longqbot.INSTANCE.getLogger().warning("tipsMap is null");
                QAConfig.INSTANCE.setTipsMap(new LinkedHashMap<>());
            }

            if (QAConfig.INSTANCE.getQaMap() == null) {
                Longqbot.INSTANCE.getLogger().warning("QaMap is null");
                QAConfig.INSTANCE.setQaMap(new LinkedHashMap<>());
            }

            String anwser = String.join("", Arrays.copyOfRange(args, 2, args.length));
            Integer key = QAConfig.INSTANCE.getTipsMap().size();
            QAConfig.INSTANCE.getTipsMap().put(key, args[1]);
            QAConfig.INSTANCE.getQaMap().put(args[1], anwser);
            if (QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()) == null)
            {
                Longqbot.INSTANCE.getLogger().warning("tipsAllowinGroup is null");
                QAConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(),new LinkedHashMap<>());
            }
            QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).put(key, args[1]);

            event.getSubject().sendMessage(String.format("问题[%s]添加成功, 序号: %d", args[1], key));
        }
        else
        {
            event.getSubject().sendMessage("格式: #问答 添加 问题名称 答案"); // 回复消息
        }
//        Integer SpecialState = usersSpecialState.get(event.getSender().getId());
//        if (SpecialState == null){
//            Longqbot.INSTANCE.getLogger().info("SpecialState is null");
//        }
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

        Integer key = 0;
        String qustion;
//        Iterator<Integer> iterator = QAConfig.INSTANCE.getTipsMap().keySet().iterator();
//        while (iterator.hasNext()){
//            key = iterator.next();
//            qustion = QAConfig.INSTANCE.getTipsMap().getOrDefault(key,null);
//            if(qustion!=null){
//                break;
//            }
//        }

        qustion = QAConfig.INSTANCE.getTipsMap().getOrDefault(key,null);
        if(args.length >= 3){
            try{
                key = Integer.valueOf(args[1]);
                qustion = QAConfig.INSTANCE.getTipsMap().getOrDefault(key, null);
                if(key<0||qustion==null)
                {
                    event.getSubject().sendMessage(String.format("参数异常, %d条目在本群不存在", key)); // 回复消息
                    return;
                }
            }catch (NumberFormatException e){
                qustion = args[1];
            }
            args = Arrays.copyOfRange(args, 2, args.length);
            String anwser = String.join("",args);
            QAConfig.INSTANCE.getQaMap().put(qustion, anwser);
            event.getSubject().sendMessage("修改成功"); // 回复消息s
        }
        else
        {
//            QAinEdit.put(key, "");
            event.getSubject().sendMessage("格式: #问答 修改 数字/问题名称 新答案"); // 回复消息
        }
    }

    void remove(GroupMessageEvent event, String[] args) {
        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        event.getSubject().sendMessage("文件层面记录删除暂未实现"); // 回复消息

    }

    void list(GroupMessageEvent event, String[] args) {
        String temp;
        if (QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null || QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null).size()==0) {
            temp = "本群问答引用条目数为0";
        } else {
            temp = "本群问答引用条目数为" + QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null).size();
            for (Integer keyID : QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null).keySet()) {
                temp += "\n#" + keyID + ": " + QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null).getOrDefault(keyID, "");
            }
            temp += "\n\n输入\n '对应问题描述语' \n或\n '#' + '对应阿拉伯数字' \n以获取对应序号条目的回答";
        }
        event.getSubject().sendMessage(new PlainText(temp));
    }

    public void quest(GroupMessageEvent event, String[] args) {
        try{
            Integer keyID = Integer.valueOf(args[args.length-1]);
            if(QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                QAConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new LinkedHashMap<>());
            }
            String question = QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).getOrDefault(keyID, null);
            String anwser = QAConfig.INSTANCE.getQaMap().get(question);
            if(anwser != null){
                event.getSubject().sendMessage(new PlainText(anwser)); // 回复消息
            }
        }catch (NumberFormatException e){
            if(args[0].charAt(0)=='#'||args[0].charAt(0)=='＃'){
                event.getSubject().sendMessage("格式有误, 相近的指令（获取问答答案）正确格式为: #数字"); // 回复消息
            }else{
                for(Integer key: QAConfig.INSTANCE.getTipsMap().keySet()){
                    String question = QAConfig.INSTANCE.getTipsMap().getOrDefault(key,null);
                    if(question != null&& question.equals(args[0])){
                        if(QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null) == null){
                            QAConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new LinkedHashMap<>());
                        }
                        if(QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).getOrDefault(key, null)!=null){
                            event.getSubject().sendMessage(QAConfig.INSTANCE.getQaMap().get(question)); // 回复消息
                            break;
                        }
                    }
                }
            }
        }
    }

    public void reference(GroupMessageEvent event, String[] args) {
        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if (args.length == 2) {
            String[] keys = args[1].split(",");
            ArrayList<Integer> keysSucc = new ArrayList<>();
            if (QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null) == null) {
                QAConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new LinkedHashMap<>());
            }
            for (String key : keys) {
                try {
                    Integer keyID = Integer.valueOf(key);
                    String quest = QAConfig.INSTANCE.getTipsMap().getOrDefault(keyID, null);
                    if (quest != null) {
                        QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).put(keyID, quest);
                        keysSucc.add(keyID);
                    }
                } catch (Exception e) {
                    //
                }
            }
            if (keysSucc.size() != 0) {
                String msg = "成功引用以下问答条目";
                for (Integer key : keysSucc) {
                    msg += "\n#" + key + ": " + QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).get(key);
                }
                event.getSubject().sendMessage(new PlainText(msg)); // 回复消息
            }else{
                event.getSubject().sendMessage(new PlainText("未识别到目前不被本群引用的新条目")); // 回复消息
            }
        }else {
            event.getSubject().sendMessage("正确格式: #问答 引用 一组用英文逗号分割的数字,如 0,1,2"); // 回复消息
        }
    }

    void ban(GroupMessageEvent event, String[] args) {
        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if (args.length == 2)
        {
            String[] keys = args[1].split(",");
            ArrayList<Integer> keysSucc = new ArrayList<>();
            if(QAConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                QAConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new LinkedHashMap<>());
            }
            for (String key: keys){
                try{
                    Integer keyID = Integer.valueOf(key);
                    String quest = QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).getOrDefault(keyID, null);
                    if(quest != null){
                        QAConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).remove(keyID);
                        keysSucc.add(keyID);
                    }
                }
                catch (Exception e)
                {
                    //
                }
            }
            if (keysSucc.size()!=0){
                String msg = "成功移除以下问答条目在本群的引用";
                for(Integer key: keysSucc){
                    if(QAConfig.INSTANCE.getTipsMap().getOrDefault(key, null)!=null){
                        msg += "\n#" + key + ": " + QAConfig.INSTANCE.getTipsMap().get(key);
                    }
                }
                event.getSubject().sendMessage(new PlainText(msg)); // 回复消息
            }
        }
        else
        {
            event.getSubject().sendMessage("正确格式: #问答 禁用 一组用英文逗号分割的数字,如 0,1,2"); // 回复消息
        }
    }
}
