package site.longint.controller.impl;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.ImageType;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import site.longint.DAO.ImageIndicator;
import site.longint.Longqbot;
import site.longint.configs.BasicConfig;
import site.longint.configs.QuotationConfig;
import site.longint.controller.Controller;
import site.longint.utils.CrawlerUtil;
import site.longint.utils.ImgUtils;
import site.longint.utils.MethodPointerUtil;
import site.longint.utils.PermissionUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class QuotationsController extends Controller {
    public static final QuotationsController INSTANCE = new QuotationsController();

    public QuotationsController() {
        super("经典", false);
    }

    void register() {
        subFuncs = new LinkedHashMap<>();

        try {
            subFuncs = new LinkedHashMap<>();

            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test", GroupMessageEvent.class, String[].class));
            subFuncs.put("添加", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "add", GroupMessageEvent.class, String[].class));
            subFuncs.put("删除", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "delete", GroupMessageEvent.class, String[].class));
            subFuncs.put("典", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "query", GroupMessageEvent.class, String[].class));
            subFuncs.put("引用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "reference", GroupMessageEvent.class, String[].class));
            subFuncs.put("禁用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "ban", GroupMessageEvent.class, String[].class));

            for(String subFuncName: subFuncs.keySet()){
                if(QuotationConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                    QuotationConfig.INSTANCE.getFuncsDiscription().put(subFuncName, "暂无详细描述");
                }
            }
//                Longqbot.INSTANCE.getLogger().info(String.valueOf(subFuncs.size()));
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e);
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
                    usersSpecialState = new LinkedHashMap<>();
                }
                try {
                    subFunc.invoke(INSTANCE, event, args);
                } catch (Exception e) {
                    Longqbot.INSTANCE.getLogger().error(e.getMessage());
                }
            }
            else
            {
                Longqbot.INSTANCE.getLogger().info(args[0]);
                query(((GroupMessageEvent)event), args);
            }
        } else {
            String help = QuotationsController.INSTANCE.getKeyword() + "功能介绍: ";
            for (String funcName : subFuncs.keySet()) {
                String discription = QuotationConfig.INSTANCE.getFuncsDiscription().getOrDefault(funcName, "");
                help += "\n" + funcName + ": " + discription;
            }
            ((GroupMessageEvent)event).getSubject().sendMessage(help); // 回复消息
        }

    }

    @Override
    public void info(Event event, String[] args){

    }

    void test(GroupMessageEvent event, String[] args) {
        Longqbot.INSTANCE.getLogger().warning(keyword + ": 测试");

        SingleMessage repeat = new PlainText("无数据");
        for(SingleMessage msg :event.getMessage()){
            if(msg instanceof Image){
                event.getSubject().sendMessage(msg.plus(" \n" +Image.queryUrl((Image)msg))); // 回复消息
            }
        }

    }

    void add(GroupMessageEvent event, String[] args) {
//        event.getSubject().sendMessage("add"); // 回复消息
//        event.getSubject().sendMessage("" + args.length); // 回复消息

        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

        if(args.length == 3){
            event.getSubject().sendMessage("指令格式: #经典 添加 标签 子标签 [图片]"); // 回复消息
        }else if(args.length == 4) {
            if(args[1].matches("[/\\:*?\"<>|]")||args[2].matches("[/\\:*?\"<>|]")){
                event.getSubject().sendMessage("非法标签名: /\\:*?\"<>|"); // 回复消息
                return;
            }

            for (int i = 0;i<event.getMessage().size();i++){
//                event.getSubject().sendMessage(new PlainText(i + ": ").plus(event.getMessage().get(i))); // 回复消息
                if(event.getMessage().get(i) instanceof Image){
                    Image img =  (Image)event.getMessage().get(i);
//                    event.getSubject().sendMessage(img.getImageId());
//                    ImageIndicatorDO ii = new ImageIndicatorDO(img); // 回复消息
//                    img = Image.fromId(ii.getImgid());
//                    event.getSubject().sendMessage(img);
                    try{
                        String path = Longqbot.INSTANCE.getDataFolderPath() + "/img/quotation";
                        File file = new File(path);
                        if (!file.exists()  && !file.isDirectory())
                        {
                            Longqbot.INSTANCE.getLogger().warning(String.format("img folder of %s not exist", keyword));
                            file.mkdir();
                        }
                        path += "/" + args[1];
                        file = new File(path);
                        if (!file.exists()  && !file.isDirectory())
                        {
                            Longqbot.INSTANCE.getLogger().warning(String.format("img folder of %s not exist", keyword + "/" + args[1]));
                            file.mkdir();
                        }
                        path += "/" + args[2] + "." + ImageType.JPG.getFormatName();
//                        event.getSubject().sendMessage(ImageType.JPG.getFormatName()); // 回复消息

                        CrawlerUtil.INSTANCE.saveFile(Image.queryUrl(img),path);
                        ImageIndicator ii = new ImageIndicator(img);
                        ii.setNativeURI(path);
                        if(QuotationConfig.INSTANCE.getQuotationsMap().getOrDefault(args[1], null)==null){
                            Longqbot.INSTANCE.getLogger().warning(keyword + ": QuotationsMap is null");
                            QuotationConfig.INSTANCE.getQuotationsMap().put(args[1], new LinkedHashMap<>());
                        }
                        QuotationConfig.INSTANCE.getQuotationsMap().get(args[1]).put(args[2], ii);
                        if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                            Longqbot.INSTANCE.getLogger().warning(keyword + ": TipsAllowinGroup is null");
                            QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new LinkedHashMap<>());
                        }
                        QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).put(args[1], true);
                        event.getSubject().sendMessage("添加成功"); // 回复消息
                    }catch (IOException e){
                        Longqbot.INSTANCE.getLogger().error(e);
                        event.getSubject().sendMessage("添加失败, 网络异常"); // 回复消息
                    }
                    break;
                }
            }
        }else{
            event.getSubject().sendMessage("指令格式: #经典 添加 标签名称 子标签名称 [图片]"); // 回复消息
        }
    }
    void delete(GroupMessageEvent event, String[] args) {
        event.getSubject().sendMessage("delete"); // 回复消息

    }

    void query(GroupMessageEvent event, String[] args){
//        event.getSubject().sendMessage("query"); // 回复消息
        if(args.length==3){
            if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(),null)!=null&&QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(),null).getOrDefault(args[1],null)){
                ImageIndicator ii = QuotationConfig.INSTANCE.getQuotationsMap().get(args[1]).getOrDefault(args[2],null);
                if(ii!=null){
                    try{
                        event.getSubject().sendMessage( ImgUtils.getImagefromImageIndicator(ii, event.getSubject(), event.getBot())); // 回复消息
                    }catch (FileNotFoundException e){
                        event.getSubject().sendMessage("图片本地备份不存在"); // 回复消息
                    }
                }
            }
        }else{
            event.getSubject().sendMessage("指令格式: #经典 引用 标签名称 子标签名称"); // 回复消息
        }
    }

    void reference(GroupMessageEvent event, String[] args) {
//        event.getSubject().sendMessage("reference"); // 回复消息

        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }


    }
    void ban(GroupMessageEvent event, String[] args) {
//        event.getSubject().sendMessage("ban"); // 回复消息

        if(BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(event.getSubject().getId(), null) == null)
        {
            BasicConfig.INSTANCE.getGroupWhiteList().put(event.getSubject().getId(), new LinkedHashMap<>());
        }
        if (!PermissionUtil.groupFuncAccess(event.getSender(), BasicConfig.INSTANCE.getGroupWhiteList().get(event.getSubject().getId()).getOrDefault(keyword, 0))) {
            event.getSubject().sendMessage("权限不足"); // 回复消息
            return;
        }

    }

    void list(GroupMessageEvent event, String[] args) {
        event.getSubject().sendMessage("list"); // 回复消息

//        if(){
//
//        }else if(){
//
//        }else{
//
//        }
    }
}
