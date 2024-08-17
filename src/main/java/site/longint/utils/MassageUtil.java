package site.longint.utils;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.message.data.MessageSource;

import java.util.Timer;
import java.util.TimerTask;

public class MassageUtil {
    // 0.2 second
    static Long basicDelay = 200L;
    static void sendDelay(Event event, String msg){
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

    public static void recall(Event event){
        if(event instanceof GroupMessageEvent){
            MessageSource.recall( ((GroupMessageEvent) event).getMessage());
        }else if(event instanceof FriendMessageEvent){
            MessageSource.recall( ((FriendMessageEvent) event).getMessage());
        }
    }
    public static void recallWithDelay(Event event, Integer delay){
        if(event instanceof GroupMessageEvent){
            MessageSource.recallIn( ((GroupMessageEvent) event).getMessage(), delay);
        }else if(event instanceof FriendMessageEvent){
            MessageSource.recallIn( ((FriendMessageEvent) event).getMessage(), delay);
        }
    }
}
