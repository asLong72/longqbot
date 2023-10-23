package site.longint.command;

import net.mamoe.mirai.console.command.CommandOwner;
import net.mamoe.mirai.console.command.java.JCompositeCommand;
import org.jetbrains.annotations.NotNull;
import site.longint.Longqbot;

public class QACommand extends JCompositeCommand {
    public static final QACommand INSTANCE = new QACommand();

    private QACommand() {
        super(Longqbot.INSTANCE, "questandanwser"); // 使用插件主类对象作为指令拥有者；设置主指令名为 "test"
        // 可选设置如下属性
//        setUsage("/dailynews"); // 设置用法，这将会在 /help 中展示
        setDescription("问答功能"); // 设置描述，也会在 /help 中展示
        setPrefixOptional(true); // 设置指令前缀是可选的，即使用 `test` 也能执行指令而不需要 `/test`
    }
}
