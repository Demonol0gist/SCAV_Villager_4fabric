package com.arkit.scavvillager;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Function;

/**
 * 把原版发来的村民 / 掠夺者语音改成"绑定实体"播放。
 *
 * 服务端触发语音时发给客户端的是固定坐标的音源包，所以原版语音不会跟着生物走；
 * 这里在客户端放音前拦一道：以音源坐标为圆心找到说话的那只生物，改成跟随实体播放。
 * 这是原 Forge 模组最核心的效果（原版 EntityVoiceBinder + BoundVoiceSound）。
 */
public final class VoiceBinder {
    /** 原版 EntityVoiceBinder.SEARCH_RADIUS：定位说话者的搜索半径。 */
    private static final double SEARCH_RADIUS = 3.5;

    /** 诊断用：记录村民/掠夺者音效到达了哪一层拦截（仅 26.2 的 mixin 调用）。 */
    public static void logArrival(String layer, net.minecraft.resources.Identifier id) {
        if (id == null) {
            return;
        }
        String path = id.getPath();
        if (path.startsWith("entity.villager.") || path.startsWith("entity.pillager.")) {
            ScavVillagerClient.LOGGER.info("[老乡村民][诊断] {} 层接到: {}", layer, path);
        }
    }

    /** 一条映射规则：哪个原版事件 → 哪种生物的哪种语音。 */
    private record Rule(Class<? extends Entity> type, int kind, Function<ScavConfig, Integer> cooldown) {
    }

    private static Rule ruleOf(String path) {
        return switch (path) {
            // 进交易界面（村民：交易）、卖不了（村民：拒绝）→ 砍价语音
            case "entity.villager.ambient", "entity.villager.celebrate", "entity.villager.trade",
                 "entity.villager.no" ->
                    new Rule(Villager.class, VoiceKind.IDLE, cfg -> cfg.ambientIntervalTicks);
            // 购买成交（村民：同意）→ 单独一句"成交"音效，每笔都响，且可以插播
            case "entity.villager.yes" -> new Rule(Villager.class, VoiceKind.TRADE, cfg -> 0);
            case "entity.villager.hurt" -> new Rule(Villager.class, VoiceKind.HURT, cfg -> 0);
            case "entity.villager.death" -> new Rule(Villager.class, VoiceKind.DEATH, cfg -> 0);
            case "entity.pillager.ambient", "entity.pillager.celebrate" ->
                    new Rule(Pillager.class, VoiceKind.IDLE, cfg -> cfg.ambientIntervalTicks);
            case "entity.pillager.hurt" -> new Rule(Pillager.class, VoiceKind.HURT, cfg -> 0);
            case "entity.pillager.death" -> new Rule(Pillager.class, VoiceKind.DEATH, cfg -> 0);
            default -> null;
        };
    }

    /**
     * @param delayTicks 原版对远处音源做的延迟（playDelayed），绑定后照旧延迟播放
     * @return true 表示已接管这条语音（调用方应取消原版播放）
     */
    public static boolean tryRebind(SoundInstance instance, int delayTicks) {
        ScavConfig cfg = ScavConfig.get();
        if (!cfg.enableMod || !cfg.followEntities || instance instanceof VoiceSound) {
            return false;
        }
        Identifier loc = instance.getIdentifier();
        if (loc == null || !"minecraft".equals(loc.getNamespace())) {
            return false;                       // 只管原版事件，模组自己的事件不拦
        }
        Rule rule = ruleOf(loc.getPath());
        if (rule == null) {
            return false;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        Entity speaker = findSpeaker(level, rule.type(), instance.getX(), instance.getY(), instance.getZ());
        if (speaker == null) {
            if (loc.getPath().startsWith("entity.villager.") || loc.getPath().startsWith("entity.pillager.")) {
                ScavVillagerClient.LOGGER.info("[老乡村民][诊断] 收到 {} 但附近找不到说话者（位置 {}, {}, {}）→ 原样播放",
                        loc.getPath(), (int) instance.getX(), (int) instance.getY(), (int) instance.getZ());
            }
            return false;                       // 找不到说话者，保持原版行为
        }
        // entity.villager.yes 有两个时机：选中可交易商品 / 交易成功。
        // 只有"玩家刚点了产出槽"那一次算成交（播成交音），其余（选中时）播砍价语音。
        if ("entity.villager.yes".equals(loc.getPath())) {
            boolean purchase = TradeWatcher.consumePurchase();
            ScavVillagerClient.LOGGER.info("[老乡村民][诊断] {} 分流 → {}（生物 {}）",
                    loc.getPath(), purchase ? "成交=deal" : "选中=砍价语音", speaker.getName().getString());
            SoundEvent event = purchase ? SoundEvent.createVariableRangeEvent(loc) : SoundIds.VILLAGER_HAGGLE;
            int kind = purchase ? VoiceKind.TRADE : VoiceKind.IDLE;
            int cooldown = purchase ? 0 : cfg.ambientIntervalTicks;
            if (delayTicks > 0) {
                Scheduler.after(delayTicks, () -> VoiceGovernor.play(speaker, event, kind, cooldown));
            } else {
                VoiceGovernor.play(speaker, event, kind, cooldown);
            }
            return true;
        }

        SoundEvent event = SoundEvent.createVariableRangeEvent(loc);
        int cooldown = rule.cooldown().apply(cfg);
        if (delayTicks > 0) {
            Scheduler.after(delayTicks, () -> VoiceGovernor.play(speaker, event, rule.kind(), cooldown));
        } else {
            VoiceGovernor.play(speaker, event, rule.kind(), cooldown);
        }
        return true;
    }

    private static Entity findSpeaker(ClientLevel level, Class<? extends Entity> type, double x, double y, double z) {
        AABB box = new AABB(x - SEARCH_RADIUS, y - SEARCH_RADIUS, z - SEARCH_RADIUS,
                x + SEARCH_RADIUS, y + SEARCH_RADIUS, z + SEARCH_RADIUS);
        List<? extends Entity> candidates = level.getEntitiesOfClass(type, box);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            if (entity.isRemoved()) {
                continue;
            }
            double dist = entity.distanceToSqr(x, y, z);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    private VoiceBinder() {
    }
}
