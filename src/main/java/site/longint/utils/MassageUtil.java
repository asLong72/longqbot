package site.longint.utils;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;

import java.util.Timer;
import java.util.TimerTask;

public class MassageUtil {
    // 0.2 second
    static Long basicDelay = new Long(200);
    static void sendDelay(String msg, Event event){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(event instanceof GroupMessageEvent){
                    ((GroupMessageEvent) event).getSubject().sendMessage(msg);
                }else if(event instanceof MemberJoinEvent){
                    ((MemberJoinEvent) event).getGroup().sendMessage(msg);
                }else if(event instanceof FriendMessageEvent){
                    ((FriendMessageEvent) event).getSender().sendMessage(msg);
                }
                timer.cancel();
            }
        }, basicDelay);
    }

    public static Long getBasicDelay() {
        return basicDelay;
    }

    public static void setBasicDelay(Long basicDelay) {
        MassageUtil.basicDelay = basicDelay;
    }
}
