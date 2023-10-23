package site.longint.utils;

import net.mamoe.mirai.contact.Member;
import site.longint.Longqbot;
import site.longint.configs.BasicConfig;

public class PermissionUtil {
    static public Boolean groupFuncAccess(Member user, Integer state){
        // 超管
        if(user.getId() == BasicConfig.INSTANCE.getSuperAdmin())
        {
            Longqbot.INSTANCE.getLogger().warning("超级管理");
            return Boolean.TRUE;
        }
        else
        {
            // 群主
            if(state == 2 && user.getPermission().getLevel()>=2)
            {
                Longqbot.INSTANCE.getLogger().warning("群主");
                return Boolean.TRUE;
            }
            // 管理
            else if(state == 3 && user.getPermission().getLevel()>=1)
            {
                Longqbot.INSTANCE.getLogger().warning("管理");
                return Boolean.TRUE;
            }
            // 任何群员
            else if(state == 4)
            {
                Longqbot.INSTANCE.getLogger().warning("任何群员");
                return Boolean.TRUE;
            }
            else
            {
                // 群白名单检测
                if(true)
                {
                    return Boolean.FALSE;
                }
                return Boolean.FALSE;
            }
        }
    }
}
