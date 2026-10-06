package com.arkit.scavvillager;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 语音调度（原版 ClientVoiceBinder + VoiceChannel 的合并移植）。
 * 职责：
 *   1. 同一只生物同时只播一条语音，**正在播的那条不允许被打断**——
 *      新语音排队等它播完（最多等 3 秒，同一只生物只留最新的一条，后来的覆盖先前的）；
 *   2. duplicateWindowMillis：同一句语音在时间窗内重复到达时丢弃；
 *   3. 每个（生物, 种类）各自的冷却；
 *   4. spawnQuietTicks：刚出现的生物先安静一会儿（只限制待机语音）。
 *
 * 注：原模组按优先级决定能否抢断（VoiceKind.priority 就是照抄它的数值），
 * 本版按需求关掉了抢断，优先级常量保留以备将来需要。
 */
public final class VoiceGovernor {
    private static final Map<UUID, VoiceSound> ACTIVE = new HashMap<>();
    private static final Map<String, Long> LAST_PLAY = new HashMap<>();    // uuid|事件 id → 毫秒
    private static final Map<String, Long> COOLDOWN = new HashMap<>();     // uuid|种类 → 解禁时刻
    private static final Map<UUID, Long> FIRST_SEEN = new HashMap<>();
    /** 排队等当前语音播完的那一条。 */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** 排队有效期：等超过这个时间就丢掉（免得战后才突然冒出一句旧喊话）。 */
    private static final long PENDING_TTL_MILLIS = 3000L;

    private record Pending(Entity entity, SoundEvent event, int kind, int cooldownTicks, long expiresAt) {
    }

    public static void clear() {
        ACTIVE.clear();
        LAST_PLAY.clear();
        COOLDOWN.clear();
        FIRST_SEEN.clear();
        PENDING.clear();
    }

    /** 记录第一次见到这只生物的时间，用于 spawnQuietTicks。 */
    public static long noteSeen(Entity entity) {
        return FIRST_SEEN.computeIfAbsent(entity.getUUID(), id -> Util.getMillis());
    }

    /** 这条语音还在播吗（引擎播完会自己 stop，再补一道 isActive 更保险）。 */
    private static boolean isPlaying(VoiceSound sound) {
        if (sound == null || sound.isStopped()) {
            return false;
        }
        return Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    /**
     * 播一条绑定实体的语音。
     * 如果这只生物正在说话，本条不会被立刻播放、也不会打断前面那条，而是排队等它播完。
     *
     * @param cooldownTicks 该类语音在这只生物身上的冷却（0 = 不限）
     * @return 是否立刻播放了（排队返回 false）
     */
    public static boolean play(Entity entity, SoundEvent event, int kind, int cooldownTicks) {
        ScavConfig cfg = ScavConfig.get();
        if (!cfg.enableMod || entity == null || entity.isRemoved()) {
            return false;
        }
        // 出声名额：只让离玩家最近的 N 只出声（0 = 不限）。不放给原版 → 彻底安静
        if (!VoiceLimiter.allows(entity, cfg)) {
            cfg.log("超出出声名额，跳过: " + Compat.soundId(event).getPath()
                    + "（" + entity.getName().getString() + "）");
            return false;
        }
        if (isPlaying(ACTIVE.get(entity.getUUID()))) {
            if (kind == VoiceKind.DEATH || kind == VoiceKind.TRADE) {
                // 死亡 / 交易成交例外：特批打断——掐掉当前语音、丢掉排队的，立刻播
                VoiceSound current = ACTIVE.get(entity.getUUID());
                if (current != null) {
                    current.finish();
                }
                PENDING.remove(entity.getUUID());
                cfg.log("插播（死亡/成交），打断当前语音: " + Compat.soundId(event).getPath());
                return playNow(entity, event, kind, cooldownTicks);
            }
            // 排队前也要查冷却：否则前面那句一播完、冷却早就过了，会一句接一句连下去
            // （小村民跟着玩家跑、请求不断，听起来就像"没有 CD"）
            if (cooldownTicks > 0) {
                Long until = COOLDOWN.get(cooldownKey(entity, kind));
                if (until != null && Util.getMillis() < until) {
                    cfg.log("冷却未到，丢弃（不排队）: " + Compat.soundId(event).getPath()
                            + "（" + entity.getName().getString() + "，还需 " + (until - Util.getMillis()) / 1000 + " 秒）");
                    return false;
                }
            }
            PENDING.put(entity.getUUID(), new Pending(entity, event, kind, cooldownTicks,
                    Util.getMillis() + PENDING_TTL_MILLIS));
            cfg.log("正在播语音，排队等待: " + Compat.soundId(event).getPath()
                    + "（" + entity.getName().getString() + "）");
            return false;
        }
        return playNow(entity, event, kind, cooldownTicks);
    }

    /** 冷却表的键：同一只生物 + 同一类语音。 */
    private static String cooldownKey(Entity entity, int kind) {
        return entity.getUUID() + "|" + kind;
    }

    /** 每 tick 调用：当前语音播完后，把排队的那条放出来。 */
    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            Pending pending = entry.getValue();
            if (pending.entity().isRemoved() || Util.getMillis() > pending.expiresAt()) {
                it.remove();
                continue;
            }
            if (isPlaying(ACTIVE.get(entry.getKey()))) {
                continue;                       // 还在播，继续等（等到过期就丢）
            }
            it.remove();
            playNow(pending.entity(), pending.event(), pending.kind(), pending.cooldownTicks());
        }
    }

    /** 实际的播放逻辑（不含排队）。 */
    private static boolean playNow(Entity entity, SoundEvent event, int kind, int cooldownTicks) {
        ScavConfig cfg = ScavConfig.get();
        long now = Util.getMillis();
        UUID id = entity.getUUID();
        String kindKey = cooldownKey(entity, kind);

        // 刚出现的生物先别急着说话（只限制待机）
        if (kind == VoiceKind.IDLE && cfg.spawnQuietTicks > 0) {
            long seen = noteSeen(entity);
            if (now - seen < cfg.spawnQuietTicks * 50L) {
                cfg.log("刚出现，暂不说话: " + Compat.soundId(event).getPath()
                        + "（" + entity.getName().getString() + "）");
                return false;
            }
        }

        // 同句重复到达
        String soundKey = id + "|" + Compat.soundId(event);
        if (cfg.duplicateWindowMillis > 0) {
            Long last = LAST_PLAY.get(soundKey);
            if (last != null && now - last < cfg.duplicateWindowMillis) {
                cfg.log("同一句重复到达，丢弃: " + Compat.soundId(event).getPath()
                        + "（" + entity.getName().getString() + "）");
                return false;
            }
        }

        // 本类语音的冷却
        if (cooldownTicks > 0) {
            Long until = COOLDOWN.get(kindKey);
            if (until != null && now < until) {
                cfg.log("冷却未到，丢弃: " + Compat.soundId(event).getPath()
                        + "（" + entity.getName().getString() + "，还需 " + (until - now) / 1000 + " 秒）");
                return false;
            }
        }

        VoiceSound sound = new VoiceSound(entity, event, kind);
        Minecraft.getInstance().getSoundManager().play(sound);
        ACTIVE.put(id, sound);
        LAST_PLAY.put(soundKey, now);
        if (cooldownTicks > 0) {
            COOLDOWN.put(kindKey, now + cooldownTicks * 50L);
        }
        cfg.log("播放语音 " + Compat.soundId(event).getPath() + "（" + entity.getName().getString() + "）");
        return true;
    }
}
