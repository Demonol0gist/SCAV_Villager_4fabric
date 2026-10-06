package com.arkit.scavvillager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 出声名额限制（配置项 maxVillagerVoices / maxPillagerVoices，0 = 不限）。
 *
 * 只允许**离玩家最近的 N 只**村民（或掠夺者）出声 —— 村里人一多就不至于一片嘈杂。
 * 名单由 SituationScanner 每 checkIntervalTicks 刷新一次（顺手复用它的扫描结果，不额外遍历世界），
 * 所有绑定语音都要经过 VoiceGovernor.play，在那里查这张名单即可。
 *
 * 已经在播的语音不会被掐断，只是不再有新的句子进来。
 */
public final class VoiceLimiter {
    private static final Set<UUID> VILLAGERS = new HashSet<>();
    private static final Set<UUID> PILLAGERS = new HashSet<>();

    /** 扫描器每次扫完调用：按离玩家的距离取最近 N 只。 */
    public static void refresh(List<Mob> nearby, Player player, ScavConfig cfg) {
        VILLAGERS.clear();
        PILLAGERS.clear();
        if (player == null) {
            return;
        }
        if (cfg.maxVillagerVoices > 0) {
            pick(nearby, player, Villager.class, cfg.maxVillagerVoices, VILLAGERS);
        }
        if (cfg.maxPillagerVoices > 0) {
            pick(nearby, player, Pillager.class, cfg.maxPillagerVoices, PILLAGERS);
        }
    }

    private static void pick(List<Mob> nearby, Player player, Class<? extends Mob> type, int limit, Set<UUID> out) {
        List<Mob> candidates = new ArrayList<>();
        for (Mob mob : nearby) {
            if (type.isInstance(mob) && !mob.isRemoved()) {
                candidates.add(mob);
            }
        }
        candidates.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        for (int i = 0; i < candidates.size() && i < limit; i++) {
            out.add(candidates.get(i).getUUID());
        }
    }

    /** 这只生物现在有出声名额吗（对应开关为 0 时永远 true）。 */
    public static boolean allows(Entity entity, ScavConfig cfg) {
        if (entity instanceof Villager) {
            return cfg.maxVillagerVoices <= 0 || VILLAGERS.contains(entity.getUUID());
        }
        if (entity instanceof Pillager) {
            return cfg.maxPillagerVoices <= 0 || PILLAGERS.contains(entity.getUUID());
        }
        return true;
    }

    public static void clear() {
        VILLAGERS.clear();
        PILLAGERS.clear();
    }

    private VoiceLimiter() {
    }
}
