package site.longint.controller.impl;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;
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
import java.nio.file.Paths;
import java.util.*;

public class QuotationsController extends Controller {
    public static final QuotationsController INSTANCE = new QuotationsController();

    public QuotationsController() {
        super("经典", false);
    }

    protected void register() {
        subFuncs = new LinkedHashMap<>();

        try {
            subFuncs = new LinkedHashMap<>();

            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test", GroupMessageEvent.class, String[].class));
            subFuncs.put("添加", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "add", GroupMessageEvent.class, String[].class));
            subFuncs.put("删除", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "delete", GroupMessageEvent.class, String[].class));
            subFuncs.put("典", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "query", GroupMessageEvent.class, String[].class));
            subFuncs.put("引用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "reference", GroupMessageEvent.class, String[].class));
            subFuncs.put("禁用", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "ban", GroupMessageEvent.class, String[].class));
            subFuncs.put("列表", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "list", GroupMessageEvent.class, String[].class));

            for(String subFuncName: subFuncs.keySet()){
                if(QuotationConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                    QuotationConfig.INSTANCE.getFuncsDiscription().put(subFuncName, "暂无详细描述");
                }
            }
//                Longqbot.INSTANCE.getLogger().info(String.valueOf(subFuncs.size()));
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
    }

    @Override
    public void onCall(Event event, String[] args) {
        if (subFuncs == null) {
            Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "subFuncs is null");
            register();
        }

        if (args != null && args.length != 0) {
//            Longqbot.INSTANCE.getLogger().info(args[0]);

            Method subFunc = subFuncs.getOrDefault(args[0], null);
            if (subFunc != null)
            {
                if (usersSpecialState == null) {
                    Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "usersSpecialState is null");
                    usersSpecialState = new LinkedHashMap<>();
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
                query((GroupMessageEvent) event,args);
            }
        } else {
            info(event, args);
        }
    }

    @Override
    public void info(Event event, String[] args){
        //
        String help = QuotationsController.INSTANCE.getKeyword() + "功能介绍: ";
        for (String subFuncName : subFuncs.keySet()) {
            String discription;
            //
            if(QuotationConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null)==null){
                discription = "暂无详细描述";
                //
                QuotationConfig.INSTANCE.getFuncsDiscription().put(subFuncName, discription);
            }else{
                //
                discription = QuotationConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, "");
            }
            help += "\n" + subFuncName + ": " + discription;
        }
        ((GroupMessageEvent)event).getSubject().sendMessage(help); // 回复消息
    }

    void test(GroupMessageEvent event, String[] args) {
        Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "测试");

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

        if(args.length == 4) {
            String signs = ",/:\\*?\"<>|, .";
            if(args[1].matches("["+signs+"]")||args[2].matches("["+signs+"]")){
                event.getSubject().sendMessage("非法标签名: " + signs); // 回复消息
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
                        // data/xxx/img/quotation
                        String rPath = Longqbot.relativeDataFolderPath + "/img/quotation";
//                        event.getSubject().sendMessage(rPath); // 回复消息
                        File file = new File(rPath);
                        if (!file.exists()  && !file.isDirectory())
                        {
                            Longqbot.INSTANCE.getLogger().warning(String.format("img folder of %s not exist", keyword));
                            file.mkdir();
                        }
                        rPath += "/" + args[1];
                        file = new File(rPath);
                        if (!file.exists()  && !file.isDirectory())
                        {
                            Longqbot.INSTANCE.getLogger().warning(String.format("img folder of %s not exist", keyword + "/" + args[1]));
                            file.mkdir();
                        }

                        //
                        ImageIndicator ii = new ImageIndicator(img);
                        // 根据消息中图片类型添加文件后缀: gif/jpg/png
                        // linux distinguish low/high
                        rPath += "/" + args[2] + "." + (ii.getType().toLowerCase());
                        //
                        CrawlerUtil.INSTANCE.saveFile(Image.queryUrl(img), rPath);
                        ii.setNativeURI(rPath);
                        if(QuotationConfig.INSTANCE.getQuotationsMap().getOrDefault(args[1], null)==null){
                            Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "QuotationsMap is null");
                            QuotationConfig.INSTANCE.getQuotationsMap().put(args[1], new LinkedHashMap<>());
                        }
                        QuotationConfig.INSTANCE.getQuotationsMap().get(args[1]).put(args[2], ii);

                        if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                            Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "TipsAllowinGroup is null");
                            QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new ArrayList<String>());
                        }
                        if(!QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).contains(args[1])){
                            QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).add(args[1]);
                        }

                        event.getSubject().sendMessage("添加成功"); // 回复消息
                    }catch (IOException e){
                        Longqbot.INSTANCE.getLogger().error(e.toString());
                        event.getSubject().sendMessage("添加失败, 网络异常"); // 回复消息
                    }
                    break;
                }
            }
        }else{
            event.getSubject().sendMessage("指令格式: #经典 添加 分组名称 子分组名称 [图片]"); // 回复消息
        }
    }
    void delete(GroupMessageEvent event, String[] args) {
        event.getSubject().sendMessage("delete"); // 回复消息

    }

    void query(GroupMessageEvent event, String[] args){
//        event.getSubject().sendMessage("query"); // 回复消息
        // '经典 典 分组 关键词’
        Integer offset = 0;
        if (args[offset].equals("典")){
            offset+=1;
        }
        if(args.length==2+offset && QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(),null)!=null
                &&QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(),null).contains(args[offset])){
            ImageIndicator ii = QuotationConfig.INSTANCE.getQuotationsMap().get(args[offset]).getOrDefault(args[offset + 1],null);
            if(ii!=null){
                try{
                    event.getSubject().sendMessage(
                            Objects.requireNonNull(
                                    ImgUtils.getImagefromImageIndicator(args[offset],args[offset+1],ii, event.getSubject(), event.getBot()))); // 回复消息
                }catch (FileNotFoundException e){
                    Longqbot.INSTANCE.getLogger().warning(Paths.get("").toAbsolutePath().toString() + "/" + ii.getNativeURI());
                    event.getSubject().sendMessage("图片本地备份不存在"); // 回复消息
                }
            }else if(offset == 1){
                event.getSubject().sendMessage("关键词不存在"); // 回复消息
            }
        }else if(args.length!=3 && offset==1){
            event.getSubject().sendMessage("使用表情包的指令格式: '#经典 典 标签名 子标签名' 或 '(标签名 子标签名)'"); // 回复消息
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
        }else{
            // 经典 引用 串
            if(args.length==2){
                String[] tips = args[1].split(",");
                ArrayList<String> succ = new ArrayList<>();
                if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                    Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "TipsAllowinGroup is null");
                    QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new ArrayList<String>());
                }
//                Longqbot.INSTANCE.getLogger().warning("for");
                for(String tip:tips){
//                    Longqbot.INSTANCE.getLogger().warning(tip);
                    if (QuotationConfig.INSTANCE.getQuotationsMap().getOrDefault(tip, null)!=null){
//                        Longqbot.INSTANCE.getLogger().warning("contain");
                        if(!QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).contains(tip)){
                            QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).add(tip);
                            succ.add(tip);
                        }
                    }
                }
                String[] temp=new String[succ.size()];
                succ.toArray(temp);
                String msg = "成功将以下表情包分组添加到本群: \n" + String.join("\n", temp) + "\n使用指令 '#经典 列表 分组名' 获取相应分组的表情包的使用方法";
                event.getSubject().sendMessage(new PlainText(msg));
            }else{
                event.getSubject().sendMessage(new PlainText("指令格式 '#经典 引用 分组名(逗号分割)'"));
            }
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
        }else{
            // 经典 禁用 串
            if(args.length==2){
                String[] tips = args[1].split(",");
                ArrayList<String> succ = new ArrayList<>();
                if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                    Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "TipsAllowinGroup is null");
                    QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new ArrayList<String>());
                }
                for(String tip:tips){
                    if (QuotationConfig.INSTANCE.getQuotationsMap().getOrDefault(tip, null)!=null){
                        if(QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).contains(tip)){
                            QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).remove(tip);
                            succ.add(tip);
                        }
                    }
                }
                String[] temp=new String[succ.size()];
                succ.toArray(temp);
                String msg = "成功将以下表情包分组禁用: \n" + String.join("\n", temp);
                event.getSubject().sendMessage(new PlainText(msg));
            }else{
                event.getSubject().sendMessage(new PlainText("指令格式 '#经典 引用 分组名(逗号分割)'"));
            }
        }

    }

    // 列表
    void list(GroupMessageEvent event, String[] args) {
//        event.getSubject().sendMessage("list"); // 回复消息
//        QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null);
//        Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "list");

        // 指令格式: #经典 列表 标签名称 子标签名称
        if(args.length==1){
            if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "TipsAllowinGroup is null");
                QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new ArrayList<String>());
            }
            List<String> tips = new ArrayList<>();
            try {
                tips = QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId());
            }catch (Exception e){
                Longqbot.INSTANCE.getLogger().warning(e.toString());
            }
//            tips = QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId());
            String msg = "";
            for(String tip: tips){
                msg += tip + "\n";
            }
            if(tips.size()==0){
                msg="本群暂无可用表情包分组\n持有bot权限者发送 '#经典 引入 分组名(逗号分割)' 以添加可用表情包分组";
            }else {
                msg="本群有以下表情包分组: \n"+msg+"\n发送 '#经典 列表 分组名称' 查看分组详细信息";
            }

            event.getSubject().sendMessage(new PlainText(msg)); // 回复消息
        }else if(args.length==2){
            if(QuotationConfig.INSTANCE.getTipsAllowinGroup().getOrDefault(event.getSubject().getId(), null)==null){
                Longqbot.INSTANCE.getLogger().warning(keyword + ": " + "TipsAllowinGroup is null");
                QuotationConfig.INSTANCE.getTipsAllowinGroup().put(event.getSubject().getId(), new ArrayList<String>());
            }
            if(QuotationConfig.INSTANCE.getTipsAllowinGroup().get(event.getSubject().getId()).contains(args[1])){
                Map data = QuotationConfig.INSTANCE.getQuotationsMap().getOrDefault(args[1],null);
                if(data!=null){
                    String msg = "分组[" + args[1] + "]中的以下关键词有对应表情包: \n";
                    Object[] tips = data.keySet().toArray();
                    for(Object tip: tips){
                        msg += ((String) tip) + "\n";
                    }
                    msg += "\n发送 '#经典 典 分组名称 关键词' 或 '(分组 关键词)' 以让bot发送对应表情包图片";
                    event.getSubject().sendMessage(new PlainText(msg)); // 回复消息
                }
            }
        }else{
            event.getSubject().sendMessage("指令格式(方框内为选填参数): #经典 列表[ 分组名称]"); // 回复消息
        }
    }
}
