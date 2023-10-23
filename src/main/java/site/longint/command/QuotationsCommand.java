package site.longint.command;

import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.java.JRawCommand;
import net.mamoe.mirai.message.data.MessageChain;
import org.jetbrains.annotations.NotNull;
import site.longint.Longqbot;

public class QuotationsCommand extends JRawCommand {


    private QuotationsCommand() {
        super(Longqbot.INSTANCE, "Quotations"); // 使用插件主类对象作为指令拥有者；设置主指令名为 "test"
        // 可选设置如下属性
        setUsage("/quotation"); // 设置用法，这将会在 /help 中展示
        setDescription("这是一个测试指令"); // 设置描述，也会在 /help 中展示
        setPrefixOptional(true); // 设置指令前缀是可选的，即使用 `test` 也能执行指令而不需要 `/test`
    }

//    注意，当 onCommand(CommandSender, MessageChain) 和 onCommand(CommandContext, MessageChain)
//    被同时覆盖时, 只有 onCommand(CommandContext, MessageChain) 会生效
    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull MessageChain args) {
        // 处理指令
    }
}
