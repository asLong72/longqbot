package site.longint.controller;

import net.mamoe.mirai.console.plugin.Plugin;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Controller {
    protected Plugin plugin;
    protected String keyword;
    protected boolean enable;
    protected LinkedHashMap<String, Method> subFuncs = null;

    // <群, <成员，状态>>
    protected static Map<Long, Map<Long,Integer>> usersSpecialState = null;

    public Controller(String k,boolean b){
        keyword =k;
        enable =b;
    }
    public Controller(String k){
        keyword = k;
        enable = false;
    }

    public abstract void onCall(GroupMessageEvent event, String[] arg);
    public abstract void info(GroupMessageEvent event, String[] args);

    public Plugin getPlugin(){
        return plugin;
    }
    public void setPlugin(Plugin p){
        plugin = p;
    }
    public String getKeyword(){
        return keyword;
    }
    public void setKeyword(String k){
        keyword = k;
    }
    public boolean getEnable(){
        return enable;
    }
    public void setEnable(boolean b){
        enable = b;
    }
}
