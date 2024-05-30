package site.longint.controller.impl;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;
import site.longint.Longqbot;
import site.longint.configs.LOLConfig;
import site.longint.controller.Controller;
import site.longint.utils.CrawlerUtil;
import site.longint.utils.ImgUtils;
import site.longint.utils.MethodPointerUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class LOLController extends Controller {
    public static final LOLController INSTANCE = new LOLController();

    static LinkedHashMap<Long,Boolean> matchState = new LinkedHashMap<>();
    static LinkedHashMap<Long,LinkedHashMap<Integer,ArrayList<Integer>>> matchHeroList = new LinkedHashMap<>();
    static LinkedHashMap<Long,LinkedHashMap<Integer,ArrayList<Long>>> matchPlayerList = new LinkedHashMap<>();
    static LinkedHashMap<Long,LinkedHashMap<Long,Integer>> memberState = new LinkedHashMap<>();
    Random random;
    int heroCount;

    public LOLController() {
        super("lol", false);
    }

    void register(){
        try {
            subFuncs = new LinkedHashMap<>();
            subFuncs.put("自定义", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "start",GroupMessageEvent.class,String[].class));
            subFuncs.put("重随", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "reroll",GroupMessageEvent.class,String[].class));
            subFuncs.put("结束", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "complete",GroupMessageEvent.class,String[].class));
            subFuncs.put("更新英雄", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "updateHeroList",GroupMessageEvent.class,String[].class));
            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test",GroupMessageEvent.class,String[].class));
//                Longqbot.INSTANCE.getLogger().info(String.valueOf(subFuncs.size()));
        }catch (Exception e){
            Longqbot.INSTANCE.getLogger().error(e);
        }
        heroCount = LOLConfig.INSTANCE.getHeroList().size();
    }

    @Override
    public void onCall(Event event, String[] args) {
        if(LOLConfig.INSTANCE.getGroupSet().getOrDefault(((GroupMessageEvent)event).getSubject().getId(), null) == null){
            LOLConfig.INSTANCE.getGroupSet().put(((GroupMessageEvent)event).getSubject().getId(), new LinkedHashMap<String,Boolean>());
        }

        if(subFuncs == null) {
            Longqbot.INSTANCE.getLogger().warning(keyword + ": subFuncs is null");
            register();
        }

        if (args != null&&args.length!=0){
//            Longqbot.INSTANCE.getLogger().info(args[0]);

            Method subfunc = subFuncs.get(args[0]);
            if(subfunc != null){
                if(usersSpecialState == null){
                    Longqbot.INSTANCE.getLogger().warning(keyword + ": usersSpecialState is null");
                    usersSpecialState = new LinkedHashMap<>();
                }
                try {
                    subfunc.invoke(INSTANCE, event, args);
                }catch (Exception e){
                    Longqbot.INSTANCE.getLogger().error(e.getMessage());
                }
            }else{

            }
        } else {

        }
    }

    @Override
    public void info(Event event, String[] args) {

    }

    void test(GroupMessageEvent event, String[] args){
        event.getSubject().sendMessage(new PlainText("测试："));
    }

    void start(GroupMessageEvent event, String[] args){
//        Longqbot.INSTANCE.getLogger().info(String.valueOf(event.getSender().getId()));
//        Longqbot.INSTANCE.getLogger().info(String.join(" ", args));
        if(matchState.getOrDefault(event.getSubject().getId(), false)){
            event.getSubject().sendMessage("上一场对局未结束!发送 “#lol 结束” 以结束上一局"); // 回复消息
            return;
        }else{
            matchState.put(event.getSubject().getId(), true);
            matchHeroList.put(event.getSubject().getId(), new LinkedHashMap<Integer,ArrayList<Integer>>());
        }

        LinkedHashMap<Long, Integer> newRaceList = new LinkedHashMap<>();
        if(args.length == 1){
            if (usersSpecialState.get(event.getSubject().getId()) == null){
                Longqbot.INSTANCE.getLogger().info("group's SpecialState is null");
                usersSpecialState.put(event.getSubject().getId(), null);
                event.getSubject().sendMessage("无过往对局记录， 请在下一次消息气泡内艾特本局全部玩家以登记对局玩家");
                return;
            }else{
                Map<Long, Integer> users = usersSpecialState.get(event.getSubject().getId());
                for (Long user: users.keySet()){
                    int state = users.get(user);
                    if(state>=0){
                        users.replace(user, state+1);
                        newRaceList.put(user, 4);
                    }
                }
                if(newRaceList.size()<2){
                    event.getSubject().sendMessage("人数不足两人无法新建"); // 回复消息
                    return;
                }
                event.getSubject().sendMessage("未提供玩家更新清单， 将沿用上一局的玩家清单");
            }
        }else{
            if (usersSpecialState.get(event.getSubject().getId()) == null){
                usersSpecialState.put(event.getSubject().getId(), new LinkedHashMap<Long, Integer>());
            }else{
                Map<Long, Integer> users = usersSpecialState.get(event.getSubject().getId());
                for (Long user: users.keySet()){
                    users.replace(user, -1);
                }
            }
            MessageChain mc = event.getMessage();
            for (Message m : mc){
                if(m instanceof At){
                    usersSpecialState.get(event.getSubject().getId()).put(((At) m).getTarget(), 4);
                    newRaceList.put(((At) m).getTarget(), 4);
                }
            }
            int playerCount = newRaceList.size();
            if(playerCount<2){
                event.getSubject().sendMessage("人数不足两人无法新建"); // 回复消息

                matchState.put(event.getSubject().getId(), false);
                return;
            }else{
                event.getSubject().sendMessage(playerCount/2 + "v"+ (playerCount-playerCount/2)); // 回复消息
            }
        }

        random = new Random(System.currentTimeMillis());
        ArrayList<Long> players = new ArrayList<>(Arrays.asList(newRaceList.keySet().toArray(new Long[newRaceList.size()])));
        ArrayList<Long> team1_Player = new ArrayList<>();
        ArrayList<Long> team2_Player = new ArrayList<>();
        if(matchPlayerList.get(event.getSubject().getId())==null){
            matchPlayerList.put(event.getSubject().getId(), new LinkedHashMap<Integer,ArrayList<Long>>());
        }
//        String team1_PlayerStr = "";
//        String team2_PlayerStr = "";
        for (int i = 0;i<players.size()/2;i++){
            Long a = players.get(random.nextInt(players.size()));
            if (!team1_Player.contains(a)) {
//                team1_randStr += random.nextInt(LOLConfig.INSTANCE.getHeroCount());
                team1_Player.add(a);
//                ExternalResource.create(HttpClient.newHttpClient().(String.format("http://q2.qlogo.cn/headimg_dl?dst_uin={}&spec=100)", ));
//                team1_PlayerStr += team1_Player.get(i) + " ";
            }else {
                i--;
            }
        }
        matchPlayerList.get(event.getSubject().getId()).put(1, team1_Player);
        // any better way？
        for (Long player : players) {
            if (!team1_Player.contains(player)) {
                team2_Player.add(player);
//                team2_PlayerStr += team2_Player.get(team2_Player.size() - 1) + " ";
            }
        }
        matchPlayerList.get(event.getSubject().getId()).put(2, team2_Player);

        Image image;
        String[] tempStr;
        // team1
        tempStr = new String[team1_Player.size()];
        for (int i = 0;i<team1_Player.size();i++){
            tempStr[i] = String.format("http://q2.qlogo.cn/headimg_dl?dst_uin=%d&spec=100", team1_Player.get(i));
        }
        image = ImgUtils.uploadNativeImg(event.getSubject(), ImgUtils.joinWebImageListHorizontal(tempStr,"RGB"));
        event.getSubject().sendMessage(new PlainText("队伍1玩家： ").plus(image)); // 回复消息
        ArrayList<Integer> team1_rand = new ArrayList<>();
//        String team1_randStr = "";
        tempStr = new String[team1_Player.size()];
        for (int i = 0;i<team1_Player.size();i++){
            int hero = random.nextInt(heroCount);
            if(!team1_rand.contains(hero)){
                team1_rand.add(hero);
//            team2_randStr += LOLConfig.INSTANCE.getHeroList().get(team1_rand.get(i)).keySet().iterator().next() + " ";
                tempStr[i] = LOLConfig.INSTANCE.getHeroList().get(team1_rand.get(i)).values().iterator().next();
            }else {
                i--;
            }
        }
        matchHeroList.get(event.getSubject().getId()).put(1, team1_rand);
        image = ImgUtils.uploadNativeImg(event.getSubject(), ImgUtils.joinNativeImageListHorizontal(tempStr,"RGB"));
        event.getSubject().sendMessage(new PlainText("队伍1英雄池： ").plus(image));

        // team2
        tempStr = new String[team2_Player.size()];
        for (int i = 0;i<team2_Player.size();i++){
            tempStr[i] = String.format("http://q2.qlogo.cn/headimg_dl?dst_uin=%d&spec=100", team2_Player.get(i));
        }
        image = ImgUtils.uploadNativeImg(event.getSubject(), ImgUtils.joinWebImageListHorizontal(tempStr,"RGB"));
        event.getSubject().sendMessage(new PlainText("队伍2玩家： ").plus(image)); // 回复消息
        ArrayList<Integer> team2_rand = new ArrayList<>();
//        String team2_randStr = "";
        tempStr = new String[team2_Player.size()];
        for (int i = 0;i<team2_Player.size();i++){
            int hero = random.nextInt(heroCount);
            if(!team2_rand.contains(hero)){
                team2_rand.add(hero);
//              team2_randStr += LOLConfig.INSTANCE.getHeroList().get(team2_rand.get(i)).keySet().iterator().next() + " ";
                tempStr[i] = LOLConfig.INSTANCE.getHeroList().get(team2_rand.get(i)).values().iterator().next();
            }else {
                i--;
            }
        }
        matchHeroList.get(event.getSubject().getId()).put(2, team2_rand);
        image = ImgUtils.uploadNativeImg(event.getSubject(), ImgUtils.joinNativeImageListHorizontal(tempStr,"RGB"));
//        event.getSubject().sendMessage("队伍2英雄池： " + team2_randStr); // 回复消息
        event.getSubject().sendMessage(new PlainText("队伍2英雄池： ").plus(image));
    }

    void reroll(GroupMessageEvent event, String[] args){
        if(matchState.getOrDefault(event.getSubject().getId(), false)){
            ArrayList<Integer> team_rand;
            int team = 1;
            if(matchPlayerList.get(event.getSubject().getId()).get(team).contains(event.getSender().getId())){
                team_rand = matchHeroList.get(event.getSubject().getId()).get(team);
            }else if(matchPlayerList.get(event.getSubject().getId()).get(++team).contains(event.getSender().getId())){
                team_rand = matchHeroList.get(event.getSubject().getId()).get(team);
            }else{
                event.getSubject().sendMessage("你是?"); // 回复消息
                event.getSubject().sendMessage("非对局玩家无法参与重随"); // 回复消息
                return;
            }
            int hero;
            do {
                hero = random.nextInt(heroCount);
            } while (team_rand.contains(hero));
            team_rand.add(hero);
            String[] tempStr = new String[team_rand.size()];
            for (int i = 0;i<team_rand.size();i++){
                tempStr[i] = LOLConfig.INSTANCE.getHeroList().get(team_rand.get(i)).values().iterator().next();
            }
            matchHeroList.get(event.getSubject().getId()).put(team, team_rand);
            Image image = ImgUtils.uploadNativeImg(event.getSubject(), ImgUtils.joinNativeImageListHorizontal(tempStr,"RGB"));
//            event.getSubject().sendMessage("队伍2英雄池： " + team2_randStr); // 回复消息
            event.getSubject().sendMessage(new PlainText(String.format("重随后的队伍%d英雄池： ", team)).plus(image));
        }else{
            event.getSubject().sendMessage("当前无对局！发送 “#lol 自定义 @参与的群友” 开启新对局"); // 回复消息
        }
    }

    void complete(GroupMessageEvent event, String[] args){
        if(matchState.getOrDefault(event.getSubject().getId(), false)){
            matchState.put(event.getSubject().getId(), false);
            event.getSubject().sendMessage("当前对局已结束！发送 “#lol 自定义 @参与的群友” 开启新对局"); // 回复消息
        }else{
            event.getSubject().sendMessage("当前无对局！发送 “#lol 自定义 @参与的群友” 开启新对局"); // 回复消息
        }
    }

    void updateHeroList(GroupMessageEvent event, String[] args){
        if(matchState.getOrDefault(event.getSubject().getId(), false)){
            event.getSubject().sendMessage("上一场对局未结束!发送 “#lol 结束” 以结束上一局"); // 回复消息
        }else{
            event.getSubject().sendMessage("网络数据连接中, 请稍后"); // 回复消息
            try{
                String path = Longqbot.INSTANCE.getDataFolderPath() + "/img/lol";
                File file =new File(path);
                if (!file.exists()  && !file.isDirectory())
                {
                    Longqbot.INSTANCE.getLogger().warning("img folder of lol not exist");
                    file.mkdir();
                }
                LinkedHashMap<Integer, Map<String,String>> heroList = CrawlerUtil.INSTANCE.lolLengendsInfo(path);
//                Longqbot.INSTANCE.getLogger().info("setHeroList");
                LOLConfig.INSTANCE.setHeroList(heroList);
                heroCount = LOLConfig.INSTANCE.getHeroList().size();
                event.getSubject().sendMessage("英雄列表更新完成, 现有英雄数量: " + LOLConfig.INSTANCE.getHeroList().size()); // 回复消息
            }catch (Exception e){
                Longqbot.INSTANCE.getLogger().error(e);
                event.getSubject().sendMessage("英雄列表更新失败, 网络异常"); // 回复消息
            }
        }
    }
}
