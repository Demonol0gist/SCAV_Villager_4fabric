package com.arkit.scavvillager;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 情境扫描：原版 AlertHandler / PillagerVoiceHandler / GrenadePanicHandler / WitnessVoiceHandler 的合并移植。
 *
 * 纯客户端实现——不去改服务端的 AI，只是观察客户端能看到的状态（点燃的 TNT、蓄弩姿态、
 * 受击动画、死亡动画）来决定这只生物现在该喊什么。判定优先级从高到低：
 *   TNT 恐慌 > 掠夺者战斗 / 失去目标 > 村民警觉 > 目击死亡
 */
public final class SituationScanner {
    private static final Set<UUID> IN_COMBAT = new HashSet<>();     // 记住"正在战斗"的掠夺者
    private static final Set<UUID> DEATH_SEEN = new HashSet<>();    // 已经播过目击的死亡事件
    private static final Map<UUID, Long> HURT_AT = new HashMap<>(); // 最近一次受击时间（战斗状态保持）
    private static long tick;

    public static void reset() {
        IN_COMBAT.clear();
        DEATH_SEEN.clear();
        HURT_AT.clear();
        tick = 0;
    }

    /**
     * 配置里凡是以"秒"为单位的冷却，传给调度器前都要换成 tick。
     * （踩过坑：直接把秒数当 tick 传，5 秒冷却实际只有 0.25 秒。）
     */
    private static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static void tick(Minecraft mc) {
        ScavConfig cfg = ScavConfig.get();
        if (!cfg.enableMod || mc.level == null || mc.player == null) {
            return;
        }
        if (++tick % Math.max(1, cfg.checkIntervalTicks) != 0) {
            return;
        }
        ClientLevel level = mc.level;
        double range = Math.max(cfg.villagerSearchRange, cfg.pillagerSearchRange);
        AABB box = mc.player.getBoundingBox().inflate(range);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box);
        if (mobs.isEmpty()) {
            IN_COMBAT.clear();
            return;
        }
        List<PrimedTnt> tnts = level.getEntitiesOfClass(PrimedTnt.class, box);

        // 先把本轮新出现的死亡挑出来，供"目击"判定使用
        List<LivingEntity> deaths = new ArrayList<>();
        for (Mob mob : mobs) {
            if (isVoiceMob(mob) && mob.deathTime > 0 && DEATH_SEEN.add(mob.getUUID())) {
                deaths.add(mob);
            }
        }
        for (Mob mob : mobs) {
            if (!isVoiceMob(mob) || mob.isDeadOrDying()) {
                continue;
            }
            boolean villager = mob instanceof Villager;
            if (villager ? !cfg.enableVillager : !cfg.enablePillager) {
                continue;
            }
            VoiceGovernor.noteSeen(mob);
            evaluate(mob, villager, tnts, deaths, cfg);
        }
    }

    private static void evaluate(Mob mob, boolean villager, List<PrimedTnt> tnts, List<LivingEntity> deaths, ScavConfig cfg) {
        // 1) 点燃的 TNT：最高优先级（每只生物各自 5 秒冷却，tntShoutCooldownSeconds）
        if (cfg.tntDetectRange > 0 && nearestTnt(mob, tnts) != null) {
            VoiceGovernor.play(mob, villager ? SoundIds.VILLAGER_GRENADE : SoundIds.PILLAGER_GRENADE,
                    VoiceKind.GRENADE, secondsToTicks(cfg.tntShoutCooldownSeconds));
            return;                  // 恐慌中，本轮不再考虑其他语音
        }

        // 2) 掠夺者：发现敌人 / 战斗喊话 / 失去目标
        if (!villager) {
            // "发现敌人"是察觉，不是开打：60 秒冷却，不会吵
            if (enemyNear(mob, cfg.alertSearchRange)) {
                VoiceGovernor.play(mob, SoundIds.PILLAGER_ALERT, VoiceKind.ALERT, secondsToTicks(cfg.playerAlertCooldownSeconds));
            }
            // 真打起来了才喊战吼（蓄弩 / 挨打），脱离后喊 clear
            if (isFighting(mob, cfg)) {
                IN_COMBAT.add(mob.getUUID());
                VoiceGovernor.play(mob, SoundIds.PILLAGER_FIGHT, VoiceKind.FIGHT, secondsToTicks(cfg.fightGapSeconds));
            } else if (IN_COMBAT.remove(mob.getUUID()) && cfg.clearEnabled) {
                // 用 fightFleeSeconds 当冷却：否则在判定边缘来回走会反复喊"失去目标"
                VoiceGovernor.play(mob, SoundIds.PILLAGER_CLEAR, VoiceKind.CLEAR, secondsToTicks(cfg.fightFleeSeconds));
            }
        } else {
            // 3) 村民：察觉到玩家（近距离）或敌意生物
            if (playerNear(mob, cfg.playerDetectRange)) {
                VoiceGovernor.play(mob, SoundIds.VILLAGER_ALERT, VoiceKind.ALERT, secondsToTicks(cfg.playerAlertCooldownSeconds));
            } else if (hostileNear(mob, cfg.alertSearchRange)) {
                VoiceGovernor.play(mob, SoundIds.VILLAGER_ALERT, VoiceKind.ALERT, secondsToTicks(cfg.hostileAlertCooldownSeconds));
            }
        }

        // 4) 目击同伴死亡
        if (cfg.witnessEnabled && witnessesDeath(mob, deaths, cfg.witnessRange)) {
            VoiceGovernor.play(mob, villager ? SoundIds.VILLAGER_WITNESS : SoundIds.PILLAGER_WITNESS,
                    VoiceKind.WITNESS, secondsToTicks(cfg.witnessCooldownSeconds));
        }
    }

    private static boolean isVoiceMob(Mob mob) {
        return mob instanceof Villager || mob instanceof Pillager;
    }

    /**
     * 战斗判定：正在蓄弩（真的在瞄准射击）、或最近挨过打（状态保持 fightFleeSeconds）。
     *
     * 注意：**不要**把"附近有玩家"算作战斗。原模组是服务端按真实攻击目标判定的，
     * 客户端看不到攻击目标；早先版本拿"附近 8 格内有玩家"当替代，导致掠夺者只要站在
     * 玩家附近就每 fightGapSeconds 秒喊一句战吼，触发率远高于原版。
     */
    private static boolean isFighting(Mob mob, ScavConfig cfg) {
        if (mob.hurtTime > 0) {
            HURT_AT.put(mob.getUUID(), Util.getMillis());
        }
        if (mob instanceof Pillager pillager && pillager.isChargingCrossbow()) {
            return true;
        }
        Long hurt = HURT_AT.get(mob.getUUID());
        if (hurt == null) {
            return false;
        }
        if (Util.getMillis() - hurt < cfg.fightFleeSeconds * 1000L) {
            return true;
        }
        HURT_AT.remove(mob.getUUID());
        return false;
    }

    private static PrimedTnt nearestTnt(Mob mob, List<PrimedTnt> tnts) {
        double range = ScavConfig.get().tntDetectRange;
        for (PrimedTnt tnt : tnts) {
            if (!tnt.isRemoved() && tnt.getFuse() > 0 && tnt.distanceToSqr(mob) <= range * range) {
                return tnt;
            }
        }
        return null;
    }

    private static boolean playerNear(Mob mob, double range) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.distanceToSqr(mob) <= range * range;
    }

    private static boolean hostileNear(Mob mob, double range) {
        List<Entity> entities = mob.level().getEntities(mob, mob.getBoundingBox().inflate(range));
        for (Entity entity : entities) {
            if (entity instanceof Enemy && entity.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** 对掠夺者来说的敌人：玩家、村民、铁傀儡。 */
    private static boolean enemyNear(Mob mob, double range) {
        List<Entity> entities = mob.level().getEntities(mob, mob.getBoundingBox().inflate(range));
        for (Entity entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }
            if (entity instanceof Player || entity instanceof Villager || entity instanceof IronGolem) {
                return true;
            }
        }
        return false;
    }

    private static boolean witnessesDeath(Mob mob, List<LivingEntity> deaths, double range) {
        for (LivingEntity dead : deaths) {
            if (dead == mob || dead.getClass() != mob.getClass()) {
                continue;
            }
            if (dead.distanceToSqr(mob) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private SituationScanner() {
    }
}
