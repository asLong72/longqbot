package site.longint.controller.impl;

import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import site.longint.Longqbot;
import site.longint.configs.AIConfig;
import site.longint.configs.BasicConfig;
import site.longint.controller.Controller;
import site.longint.utils.MethodPointerUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * AI 对话功能(DeepSeek API, OpenAI 兼容接口)
 *
 * 设计决策(2026-09-04, 后续可按需调整):
 *  - 触发: 群内被 @bot 即触发(需 groupEnable + 群白名单["AI"]>0);另有手动指令 "#AI <内容>"。
 *  - 冷却(2026-09-04 改版): 按 群+人 维度动态计算——
 *      cd = clamp(5 * log10(bot 上次回复该用户的内容字符数), coolDownMinSeconds=5, coolDownMaxSeconds=30);
 *      无上次回复(首次)按下限 5 秒。
 *  - 违禁词: forbiddenWords 子串命中 / forbiddenRegex 正则命中 -> 发违禁提示, 不回复。
 *  - 广告甄别: adFeatures 子串命中 -> 发广告提示, 不调用 AI。
 *  - 上下文(2026-09-04 改版): 
 *      ① 群全局队列(globalChat): 记录该群最近 N(AIConfig.globalHistoryMax, 默认10)条消息(含bot回复);
 *      ② 交互用户队列(interactors): 记录"10分钟(AIConfig.interactWindowMinutes)内与bot交互过
 *         (使用指令#开头 或 @机器人)"的每位用户最近 N(AIConfig.personalHistoryMax, 默认10)条发言;
 *      ③ bot 真实发送队列(botSent): 记录 bot 最近真实发送到QQ的 N(AIConfig.botSentMax, 默认10)条消息
 *         (由 sendMessage 成功后的 appendBotGlobal 写入, 保证真实发送)。
 *      最终上下文 = 三队列合并去重(①全部 + ②该用户近 contextMinutes 分钟 + ③bot 近 contextMinutes 分钟),
 *      每条带时间戳(MM-dd HH:mm, 精确到分钟)并按时间升序排序后发送给 DeepSeek。
 *  - API 请求内容: system(人设+时间+违禁主题提醒) + user(三队列合并上下文 + 本次内容)。
 *
 * 说明: 本功能为被动 @ 触发, 不走 #指令 主路由; 但为与现有框架一致, 仍注册为 Controller,
 *      支持在 #帮助 中列出、用群白名单启用, 并提供 "#AI" 手动指令与 "状态" 子功能。
 */
public class AIController extends Controller {
    public static final AIController INSTANCE = new AIController();

    /** 群内单条消息记录 */
    static class Rec {
        final long time;
        final String nick;   // 发言人(群昵称/昵称; bot 记为 "[bot]")
        final long senderId;
        final String text;
        Rec(long time, String nick, long senderId, String text) {
            this.time = time; this.nick = nick; this.senderId = senderId; this.text = text;
        }
    }

    /** 交互用户: 最近一次交互时间 + 个人最近消息队列(尾=最新) */
    static class InterUser {
        volatile long lastInteract;
        final ConcurrentLinkedDeque<Rec> msgs = new ConcurrentLinkedDeque<>();
        InterUser(long lastInteract) { this.lastInteract = lastInteract; }
    }

    // ① 群 -> 全局最近消息队列(所有成员, 尾=最新)
    private static final ConcurrentHashMap<Long, ConcurrentLinkedDeque<Rec>> globalChat = new ConcurrentHashMap<>();
    // ② 群 -> 用户 -> 交互用户(含个人最近消息, 尾=最新)
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<Long, InterUser>> interactors = new ConcurrentHashMap<>();
    // "群ID:用户ID" -> 上次成功调用AI的时间戳(冷却用)
    private static final ConcurrentHashMap<String, Long> lastCall = new ConcurrentHashMap<>();
    // "群ID:用户ID" -> bot 上次回复该用户的内容长度(字符数, 动态冷却用)
    private static final ConcurrentHashMap<String, Integer> lastReplyLen = new ConcurrentHashMap<>();
    // ③ 群 -> bot 最近真实发送到QQ的消息队列(尾=最新), 上限 botSentMax
    private static final ConcurrentHashMap<Long, ConcurrentLinkedDeque<Rec>> botSent = new ConcurrentHashMap<>();

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private AIController() {
        super("AI", false);
    }

    @Override
    protected void register() {
        subFuncs = new LinkedHashMap<>();
        try {
            subFuncs.put("功能介绍", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "info", Event.class, String[].class));
            subFuncs.put("状态", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "state", GroupMessageEvent.class, String[].class));
            subFuncs.put("测试", MethodPointerUtil.getMethodwithTwoParams(INSTANCE, "test", GroupMessageEvent.class, String[].class));
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
    }

    // ================= 框架接口 =================

    @Override
    public void onCall(Event event, String[] args) {
        if (subFuncs == null) {
            Longqbot.INSTANCE.getLogger().warning(keyword + ": subFuncs is null");
            register();
        }
        if (args != null && args.length != 0) {
            Method subFunc = subFuncs.getOrDefault(args[0], null);
            if (subFunc != null) {
                try {
                    subFunc.invoke(INSTANCE, event, args);
                } catch (Exception e) {
                    Longqbot.INSTANCE.getLogger().error(e.toString());
                }
                return;
            }
            // 手动指令: #AI <内容...>  -> 把全文当对话文本(不带@也触发)
            String text = String.join(" ", args);
            if (event instanceof GroupMessageEvent) {
                handleChat((GroupMessageEvent) event, text, false);
            }
        } else {
            info(event, args);
        }
    }

    @Override
    public void info(Event event, String[] args) {
        String help = AIController.INSTANCE.getKeyword() + "功能介绍: ";
        if (subFuncs == null) register();
        for (String subFuncName : subFuncs.keySet()) {
            String discription;
            if (AIConfig.INSTANCE.getFuncsDiscription() == null) {
                AIConfig.INSTANCE.setFuncsDiscription(new LinkedHashMap<>());
            }
            if (AIConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, null) == null) {
                discription = "暂无详细描述";
                AIConfig.INSTANCE.getFuncsDiscription().put(subFuncName, discription);
            } else {
                discription = AIConfig.INSTANCE.getFuncsDiscription().getOrDefault(subFuncName, "");
            }
            help += "\n" + subFuncName + ": " + discription;
        }
        help += "\n\n使用方式: 在群内 @机器人 并附上想说的话, 即可与AI对话。";
        ((GroupMessageEvent) event).getSubject().sendMessage(help);
    }

    /** 状态: 配置摘要 + 当前群AI启用状态 + 历史条数 */
    public void state(GroupMessageEvent event, String[] args) {
        String keyOk = (AIConfig.INSTANCE.getApiKey() == null || AIConfig.INSTANCE.getApiKey().isEmpty())
                ? "未配置(需在AIConfig.yml填apiKey)" : "已配置";
        Long groupId = event.getSubject().getId();
        boolean enabled = isGroupAIEnabled(groupId);
        int wlState = 0;
        try {
            wlState = BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(groupId, Collections.emptyMap()).getOrDefault(keyword, 0);
        } catch (Exception ignored) { }
        Long globalCount = 0L;
        int interactorCount = 0;
        Long personalTotal = 0L;
        Long botCount = 0L;
        try {
            ConcurrentLinkedDeque<Rec> g = globalChat.get(groupId);
            globalCount = (g == null) ? 0L : (long) g.size();
            ConcurrentLinkedDeque<Rec> b = botSent.get(groupId);
            botCount = (b == null) ? 0L : (long) b.size();
            Map<Long, InterUser> im = interactors.get(groupId);
            if (im != null) {
                interactorCount = im.size();
                for (InterUser iu : im.values()) personalTotal += iu.msgs.size();
            }
        } catch (Exception ignored) { }

        String msg = "AI功能状态: \n"
                + " - API Key: " + keyOk + "\n"
                + " - 模型: " + safe(AIConfig.INSTANCE.getModel()) + "\n"
                + " - 触发冷却: 动态 5*log10(上次回复长度), 范围 ["
                + AIConfig.INSTANCE.getCoolDownMinSeconds() + ", " + AIConfig.INSTANCE.getCoolDownMaxSeconds() + "] 秒(按人)\n"
                + " - 违禁词数量: " + safeList(AIConfig.INSTANCE.getForbiddenWords()).size()
                + " + 违禁正则: " + safeList(AIConfig.INSTANCE.getForbiddenRegex()).size() + "\n"
                + " - 广告特征数量: " + safeList(AIConfig.INSTANCE.getAdFeatures()).size() + "\n"
                + " - 本群启用级别: " + wlState + (enabled ? " (生效)" : " (未生效)") + "\n"
                + " - 群全局上下文条数: " + globalCount + " / " + AIConfig.INSTANCE.getGlobalHistoryMax() + "\n"
                + " - bot真实发送记录: " + botCount + " / " + AIConfig.INSTANCE.getBotSentMax() + "\n"
                + " - 交互用户数(" + AIConfig.INSTANCE.getInteractWindowMinutes() + "分钟窗口): " + interactorCount + ", 个人上下文总条数: " + personalTotal
                + "\n - 最终上下文窗口: 个人/bot 最近 " + AIConfig.INSTANCE.getContextMinutes() + " 分钟, 全局最新" + AIConfig.INSTANCE.getGlobalHistoryMax() + "条, 按时间排序带时间戳";
        event.getSubject().sendMessage(new PlainText(msg));
    }

    public void test(GroupMessageEvent event, String[] args) {
        event.getSubject().sendMessage(new PlainText("AI功能测试: 可发 '@机器人 你好' 试对话"));
    }

    // ================= 被动触发入口(由 Longqbot.GroupMSGListener 调用) =================

    /** 该群是否启用了 AI(群白名单 > 0) */
    public static boolean isGroupAIEnabled(Long groupId) {
        try {
            Map<String, Integer> wl = BasicConfig.INSTANCE.getGroupWhiteList().getOrDefault(groupId, null);
            return wl != null && wl.getOrDefault("AI", 0) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 记录群消息(供AI上下文)。仅记录启用了AI的群, 且跳过bot自身发言。 */
    /**
     * 记录群消息(供AI上下文):
     *  ① 全局队列(globalChat): 记录该群所有成员文本消息(含 bot 自身, 由消息入口/appendBotGlobal 写入), 上限 globalHistoryMax;
     *  ② 交互用户队列(interactors): 若本条为"与bot交互"(#指令 或 @bot), 将发送者登记为交互用户并刷新其 10 分钟活跃窗口,
     *     之后该用户发言持续进入其个人队列, 上限 personalHistoryMax。
     * 注: bot 真实发送的回复由 appendBotGlobal 写入 globalChat 与 botSent 队列。
     */
    public static void record(GroupMessageEvent event) {
        try {
            Long groupId = event.getSubject().getId();
            if (!isGroupAIEnabled(groupId)) return;
            String text = extractPlainText(event);
            if (text == null || text.trim().isEmpty()) return;

            long botId = event.getBot().getId();
            long senderId = event.getSender().getId();
            String nick = displayName(event, senderId);
            long now = System.currentTimeMillis();

            // ① 全局队列(所有消息; 包括 bot 自己)
            ConcurrentLinkedDeque<Rec> gq = globalChat.computeIfAbsent(groupId, k -> new ConcurrentLinkedDeque<>());
            synchronized (gq) {
                gq.addLast(new Rec(now, nick, senderId, text));
                trimTo(gq, AIConfig.INSTANCE.getGlobalHistoryMax());
            }

            // ② 交互用户个人队列(bot 自身消息不算交互)
            if (senderId == botId) return;
            boolean interact = containsAtBot(event)
                    || text.startsWith("#") || text.startsWith("＃");
            Map<Long, InterUser> im = interactors.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
            InterUser iu = im.get(senderId);
            if (interact) {
                if (iu == null) {
                    iu = new InterUser(now);
                    im.put(senderId, iu);
                } else {
                    iu.lastInteract = now;
                }
            }
            if (iu != null) {
                synchronized (iu.msgs) {
                    iu.msgs.addLast(new Rec(now, nick, senderId, text));
                    trimTo(iu.msgs, AIConfig.INSTANCE.getPersonalHistoryMax());
                }
            }
            // 惰性清理: 移除超过活跃窗口未再交互的用户
            long winMs = (long) AIConfig.INSTANCE.getInteractWindowMinutes() * 60_000L;
            for (java.util.Iterator<Map.Entry<Long, InterUser>> it = im.entrySet().iterator(); it.hasNext(); ) {
                if (now - it.next().getValue().lastInteract > winMs) it.remove();
            }
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error("AI record error: " + e);
        }
    }

    /** @bot 触发主入口(调用方已确认 群启用 + 白名单>0 + 消息含@bot) */
    public void onGroupAt(GroupMessageEvent event) {
        String text = extractPlainText(event);
        if (text == null || text.trim().isEmpty()) {
            return; // 纯@无内容不触发
        }
        handleChat(event, text.trim(), true);
    }

    // ================= 核心对话流程 =================

    private void handleChat(GroupMessageEvent event, String userText, boolean atTrigger) {
        Long groupId = event.getSubject().getId();
        Long userId = event.getSender().getId();
        // 1. 冷却检查(群+人, 动态: 依上次回复长度, 范围[min,max])
        String cdKey = groupId + ":" + userId;
        long now = System.currentTimeMillis();
        int cdSec = coolDownSecFor(cdKey);
        Long last = lastCall.get(cdKey);
        if (last != null && now - last < cdSec * 1000L) {
            Longqbot.INSTANCE.getLogger().info("AI 冷却中: " + cdKey + " (" + cdSec + "s)");
            return; // 冷却中静默
        }
        // 2. 违禁预检(用户内容)
        String banReason = matchForbidden(userText);
        if (banReason != null) {
            reply(event, safe(AIConfig.INSTANCE.getBannedReply(), "该内容涉及违规, 已拒绝回复"));
            return;
        }
        // 3. 广告甄别(用户内容)
        if (matchAd(userText)) {
            reply(event, safe(AIConfig.INSTANCE.getAdReply(), "检测到疑似广告内容, 已忽略"));
            return;
        }
        // 4. API Key 检查
        String apiKey = AIConfig.INSTANCE.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Longqbot.INSTANCE.getLogger().warning("AI apiKey 未配置");
            reply(event, "AI 尚未配置(apiKey 为空), 请联系管理员在 AIConfig.yml 中配置。");
            return;
        }

        // 5. 取上下文(群全局 + 该用户个人; record 已在消息入口写入本条, 取时自动排除本条)
        String context = collectContext(groupId, userId, userText);

        // 6. 异步调用 DeepSeek
        lastCall.put(cdKey, now);
        final String userMsg = userText;
        final boolean isAt = atTrigger;
        pool.execute(() -> {
            try {
                String replyText = callDeepSeek(event, context, userMsg);
                if (replyText == null || replyText.trim().isEmpty()) {
                    reply(event, "AI 没有返回内容, 请稍后再试");
                    return;
                }
                // 违禁检查(AI回复)
                if (matchForbidden(replyText) != null) {
                    reply(event, safe(AIConfig.INSTANCE.getBannedReply(), "该内容涉及违规, 已拒绝回复"));
                    return;
                }
                reply(event, replyText.trim());
                // 若此前冷却中本次仍记了调用时间, 这里不再重复处理
            } catch (Exception e) {
                Longqbot.INSTANCE.getLogger().error("AI call error: " + e);
                reply(event, "AI 调用出错, 请稍后再试: " + e.getMessage());
            }
        });
    }

    private void reply(GroupMessageEvent event, String text) {
        try {
            event.getSubject().sendMessage(new At(event.getSender().getId()).plus(new PlainText("\n" + text)));
            // bot 真实发送已由 GroupMessagePostSendEvent -> onBotSent 统一记录到 全局/botSent 队列
            // 记录本次回复长度, 用于该用户下次触发时的动态冷却计算
            try {
                String cdKey = event.getSubject().getId() + ":" + event.getSender().getId();
                lastReplyLen.put(cdKey, (text == null) ? 0 : text.length());
            } catch (Exception ignored) { }
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error("AI reply error: " + e);
        }
    }

    /**
     * 由 Longqbot 订阅的 GroupMessagePostSendEvent 回调:
     *  bot 向群真实发送消息后触发(无论来自哪个功能: 问答/欢迎/AI/经典图等),
     *  若该群启用了 AI, 则将发送的纯文本计入 globalChat 与 botSent 队列。
     */
    public static void onBotSent(GroupMessagePostSendEvent event) {
        try {
            if (event.getException() != null) return; // 发送失败不算真实发送
            net.mamoe.mirai.contact.Group group = event.getTarget();
            if (group == null) return;
            long groupId = group.getId();
            if (!isGroupAIEnabled(groupId)) return;
            String text = extractPlainTextOf(event.getMessage());
            if (text == null || text.trim().isEmpty()) return;
            appendBotGlobal(groupId, group.getBot().getId(), text.trim());
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error("AI onBotSent error: " + e);
        }
    }

    /** 从 MessageChain 提取纯文本段拼接(At/Image/其它忽略) */
    private static String extractPlainTextOf(MessageChain chain) {
        if (chain == null) return "";
        StringBuilder sb = new StringBuilder();
        for (SingleMessage m : chain) {
            if (m instanceof PlainText) {
                sb.append(((PlainText) m).getContent());
            }
        }
        return sb.toString();
    }

    /**
     * 计算某用户(群:人)当前触发冷却秒数:
     *  基于 bot 上次回复该用户的内容长度 len: 5*log10(len);
     *  限制在 [coolDownMinSeconds, coolDownMaxSeconds] 内, 无记录(首次)取下限。
     */
    private static int coolDownSecFor(String cdKey) {
        int min = AIConfig.INSTANCE.getCoolDownMinSeconds();
        int max = AIConfig.INSTANCE.getCoolDownMaxSeconds();
        if (min < 1) min = 1;
        if (max < min) max = min;
        Integer len = lastReplyLen.get(cdKey);
        if (len == null || len <= 0) return min;
        double v = 5.0 * Math.log10(len);
        int sec = (int) Math.round(v);
        if (sec < min) sec = min;
        if (sec > max) sec = max;
        return sec;
    }

    /**
     * 记录 bot 真实发送到QQ群的消息(在 sendMessage 成功后调用):
     *  写入 群全局队列(globalChat, 含bot) 与 botSent 队列(仅 AI 启用群)。
     */
    private static void appendBotGlobal(Long groupId, long botId, String text) {
        try {
            if (!isGroupAIEnabled(groupId)) return;
            if (text == null || text.trim().isEmpty()) return;
            Rec rec = new Rec(System.currentTimeMillis(), "[bot]", botId, text.trim());
            ConcurrentLinkedDeque<Rec> gq = globalChat.computeIfAbsent(groupId, k -> new ConcurrentLinkedDeque<>());
            synchronized (gq) {
                gq.addLast(rec);
                trimTo(gq, AIConfig.INSTANCE.getGlobalHistoryMax());
            }
            ConcurrentLinkedDeque<Rec> bs = botSent.computeIfAbsent(groupId, k -> new ConcurrentLinkedDeque<>());
            synchronized (bs) {
                bs.addLast(rec);
                trimTo(bs, AIConfig.INSTANCE.getBotSentMax());
            }
            Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] botSent+ len=" + text.trim().length()
                    + " global=" + gq.size() + " botSent=" + bs.size()
                    + " text=" + truncate(text.trim(), 200));
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
    }

    // ================= 上下文收集(群全局 + 交互用户 + bot输出 三队列合并) =================

    /**
     * 组装发送给 AI 的最终上下文(三个队列合并去重, 按时间排序, 时间戳精确到分钟):
     *  ① 群内全局最新 globalHistoryMax 条消息(globalChat, 含bot);
     *  ② 正在对话用户最近 contextMinutes 分钟内的发言(interactors 个人队列);
     *  ③ bot 最近 contextMinutes 分钟内的真实输出(botSent 队列)。
     * 均排除"本条"(record 已在消息入口写入本条)。
     * @return null 表示无任何上下文
     */
    private String collectContext(Long groupId, Long userId, String userText) {
        long now = System.currentTimeMillis();
        int ctxMin = AIConfig.INSTANCE.getContextMinutes();
        long ctxMs = (long) ctxMin * 60_000L;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        // 用 (time|senderId|text) 去重后统一按 time 排序
        Map<String, Rec> merged = new LinkedHashMap<>();

        // ① 群内全局(已有 globalHistoryMax 上限, 头旧尾新)
        ConcurrentLinkedDeque<Rec> gq = globalChat.get(groupId);
        if (gq != null) {
            for (Rec r : gq) {
                if (r.senderId == userId && r.text.equals(userText)) continue;
                merged.putIfAbsent(r.time + "|" + r.senderId + "|" + r.text, r);
            }
        }
        // ② 交互用户个人(contextMinutes 内)
        Map<Long, InterUser> im = interactors.get(groupId);
        InterUser iu = (im == null) ? null : im.get(userId);
        if (iu != null && now - iu.lastInteract <= (long) AIConfig.INSTANCE.getInteractWindowMinutes() * 60_000L) {
            for (Rec r : iu.msgs) {
                if (now - r.time > ctxMs) continue;
                if (r.senderId == userId && r.text.equals(userText)) continue;
                merged.putIfAbsent(r.time + "|" + r.senderId + "|" + r.text, r);
            }
        }
        // ③ bot 最近 contextMinutes 分钟真实输出
        ConcurrentLinkedDeque<Rec> bs = botSent.get(groupId);
        if (bs != null) {
            for (Rec r : bs) {
                if (now - r.time > ctxMs) continue;
                merged.putIfAbsent(r.time + "|" + r.senderId + "|" + r.text, r);
            }
        }
        if (merged.isEmpty()) {
            Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] collectContext EMPTY group=" + groupId
                    + " user=" + userId + " gqSize=" + (gq == null ? 0 : gq.size())
                    + " botSentSize=" + (bs == null ? 0 : bs.size())
                    + " interactSize=" + (iu == null ? 0 : iu.msgs.size())
                    + " ctxMin=" + ctxMin);
            return null;
        }

        List<Rec> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparingLong(r -> r.time)); // 旧->新
        StringBuilder sb = new StringBuilder();
        for (Rec r : list) {
            String ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.time), ZoneId.systemDefault()).format(fmt);
            sb.append('[').append(ts).append("] ").append(r.nick).append(": ").append(r.text).append('\n');
        }
        sb.setLength(sb.length() - 1);
        String result = sb.toString();
        Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] collectContext merged=" + list.size()
                + " gqSize=" + (gq == null ? 0 : gq.size())
                + " botSentSize=" + (bs == null ? 0 : bs.size())
                + " interactSize=" + (iu == null ? 0 : iu.msgs.size())
                + "\n---context-begin---\n" + result + "\n---context-end---");
        return result;
    }

    /** 裁剪队列至最大条数(从头部淘汰最旧) */
    private static void trimTo(ConcurrentLinkedDeque<Rec> dq, int max) {
        if (max <= 0) return;
        while (dq.size() > max) dq.pollFirst();
    }

    /** 发言人显示名: bot 记 [bot], 其余取群名片/昵称, 取不到用QQ号 */
    private static String displayName(GroupMessageEvent event, long senderId) {
        if (senderId == event.getBot().getId()) return "[bot]";
        try {
            net.mamoe.mirai.contact.Member m = event.getGroup().get(senderId);
            if (m != null) {
                String c = m.getNameCard();
                if (c == null || c.trim().isEmpty()) c = m.getNick();
                if (c != null && !c.trim().isEmpty()) return c.trim();
            }
        } catch (Exception ignored) { }
        return String.valueOf(senderId);
    }

    // ================= 违禁词 / 广告 =================

    /** 返回命中的违禁说明; 未命中返回 null */
    private String matchForbidden(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            for (String w : safeList(AIConfig.INSTANCE.getForbiddenWords())) {
                if (w != null && !w.isEmpty() && text.contains(w)) return "命中违禁词: " + w;
            }
            for (String re : safeList(AIConfig.INSTANCE.getForbiddenRegex())) {
                if (re == null || re.isEmpty()) continue;
                try {
                    if (Pattern.compile(re).matcher(text).find()) return "命中违禁正则: " + re;
                } catch (PatternSyntaxException ignored) {
                    Longqbot.INSTANCE.getLogger().warning("违禁正则语法错误, 已忽略: " + re);
                }
            }
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
        return null;
    }

    /** 广告特征子串命中判定 */
    private boolean matchAd(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            for (String f : safeList(AIConfig.INSTANCE.getAdFeatures())) {
                if (f != null && !f.isEmpty() && text.contains(f)) {
                    Longqbot.INSTANCE.getLogger().info("AI 命中广告特征: " + f);
                    return true;
                }
            }
        } catch (Exception e) {
            Longqbot.INSTANCE.getLogger().error(e.toString());
        }
        return false;
    }

    // ================= DeepSeek 调用(jsoup, OpenAI 兼容) =================

    private String callDeepSeek(GroupMessageEvent event, String context, String userMsg) throws IOException {
        String apiKey = AIConfig.INSTANCE.getApiKey();
        String apiUrl = safe(AIConfig.INSTANCE.getApiUrl(), "https://api.deepseek.com/chat/completions");
        String model = safe(AIConfig.INSTANCE.getModel(), "deepseek-chat");
        String aiName = safe(AIConfig.INSTANCE.getAiName(), "AI");

        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // system: 人设 + 时间 + 违禁主题提醒(本地已预检, 这里让AI也知晓, 处理"主题相关"的语义层面)
        StringBuilder sys = new StringBuilder();
        sys.append("你是在QQ群里的").append(aiName).append("。").append(safe(AIConfig.INSTANCE.getSystemPrompt(), ""))
           .append("\n当前时间: ").append(nowStr);
        List<String> fw = safeList(AIConfig.INSTANCE.getForbiddenWords());
        if (!fw.isEmpty()) {
            sys.append("\n注意: 若用户内容的主题与下列违禁话题相关, 请仅回复: ")
               .append(safe(AIConfig.INSTANCE.getBannedReply(), "该内容涉及违规"))
               .append("。违禁词: ").append(String.join("、", fw));
        }

        // user: 群聊上下文 + 本次内容(已去除@)
        StringBuilder user = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            user.append("以下是相关群聊上下文(仅作参考背景):\n").append(context).append("\n\n");
        }
        user.append("本次要说的话: ").append(userMsg);

        String body = "{\"model\":" + jsonStr(model)
                + ",\"messages\":[{\"role\":\"system\",\"content\":" + jsonStr(sys.toString())
                + "},{\"role\":\"user\",\"content\":" + jsonStr(user.toString())
                + "}],\"stream\":false}";

        Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] API request full body len=" + body.length());
        Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] ====SYS====\n" + sys + "\n====END-SYS====");
        Longqbot.INSTANCE.getLogger().info("[AI-DEBUG] ====USER====\n" + user + "\n====END-USER====");

        Connection.Response resp = Jsoup.connect(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json;charset=utf-8")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(90000)
                .requestBody(body)
                .method(Connection.Method.POST)
                .execute();

        String respBody = resp.body();
        if (resp.statusCode() != 200) {
            throw new IOException("DeepSeek HTTP " + resp.statusCode() + ": " + truncate(respBody, 200));
        }
        String content = extractContent(respBody);
        if (content == null) {
            throw new IOException("无法解析 DeepSeek 响应: " + truncate(respBody, 200));
        }
        return content;
    }

    /** 从 OpenAI 兼容响应 JSON 中提取 choices[0].message.content */
    private static String extractContent(String json) {
        if (json == null) return null;
        String marker = "\"content\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int start = idx + marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'u': {
                        if (i + 5 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                                i += 4;
                            } catch (Exception e) {
                                sb.append('u');
                            }
                        }
                        break;
                    }
                    default: sb.append(n);
                }
                i++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** JSON 字符串转义 */
    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ================= 工具 =================

    /** 提取消息中纯文本并去除 @ 与其它消息段(图片等忽略) */
    private static String extractPlainText(GroupMessageEvent event) {
        StringBuilder sb = new StringBuilder();
        for (SingleMessage m : event.getMessage()) {
            if (m instanceof PlainText) {
                sb.append(((PlainText) m).getContent());
            }
            // At/Image/其它忽略
        }
        String s = sb.toString();
        // 去除可能的At残留在contentToString中不适用(我们已按元素取)
        return s;
    }

    /** 消息中是否 @ 了 bot */
    public static boolean containsAtBot(GroupMessageEvent event) {
        long botId = event.getBot().getId();
        for (SingleMessage m : event.getMessage()) {
            if (m instanceof At && ((At) m).getTarget() == botId) {
                return true;
            }
        }
        return false;
    }

    // 供其它包(如测试/日志)取本群上下文总条数(全局+交互个人+bot输出)
    public static int historySize(Long groupId) {
        int total = 0;
        ConcurrentLinkedDeque<Rec> gq = globalChat.get(groupId);
        if (gq != null) total += gq.size();
        ConcurrentLinkedDeque<Rec> bs = botSent.get(groupId);
        if (bs != null) total += bs.size();
        Map<Long, InterUser> im = interactors.get(groupId);
        if (im != null) {
            for (InterUser iu : im.values()) total += iu.msgs.size();
        }
        return total;
    }

    private static String safe(String s, String def) {
        return (s == null || s.trim().isEmpty()) ? def : s.trim();
    }
    private static String safe(String s) {
        return s == null ? "" : s;
    }
    private static List<String> safeList(List<String> l) {
        return l == null ? Collections.emptyList() : l;
    }
    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }
}
