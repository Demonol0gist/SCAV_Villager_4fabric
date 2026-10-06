package com.arkit.scavvillager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

/**
 * 界面音效（原版 ClientUiEvents 的移植）：
 *   - 进入世界：播 3 遍入场音频 + 弹一次入场提示框（joinSound 开关）
 *   - 本地玩家死亡：播死亡提示音（playerDeathCooldownSeconds 冷却）
 *   - 成就 / 配方提示音走 mixin（见 AdvancementToastMixin / RecipeToastMixin）
 *   - 228 整活弹窗：每 1200 tick 掷 1% 概率
 */
public final class UiSounds {
    /** 原版 ClientUiEvents 常量。 */
    public static final int JOIN_AUDIO_GAP_TICKS = 96;
    public static final int JOIN_AUDIO_TOTAL_PLAYS = 3;
    public static final int RECIPE_DELAY_TICKS = 8;
    private static final int ERROR228_INTERVAL_TICKS = 1200;
    private static final float ERROR228_CHANCE = 0.01F;

    private static final RandomSource RANDOM = RandomSource.create();

    private static boolean inWorld;
    private static int joinPlaysLeft;
    private static int deathCooldownTicks;
    private static boolean playerWasDead;
    private static int error228Countdown = ERROR228_INTERVAL_TICKS;

    public static void reset() {
        inWorld = false;
        joinPlaysLeft = 0;
        deathCooldownTicks = 0;
        playerWasDead = false;
    }

    public static void tick(Minecraft mc) {
        boolean nowInWorld = mc.level != null && mc.player != null;
        ScavConfig cfg = ScavConfig.get();

        if (nowInWorld && !inWorld) {
            ScavConfig.load();                  // 进世界时重载配置，改完不用重启
            cfg = ScavConfig.get();
            if (cfg.joinSound) {
                joinPlaysLeft = JOIN_AUDIO_TOTAL_PLAYS;
                Scheduler.after(20, UiSounds::playJoinSound);
                Compat.addToast(mc, new IntroToast());
            }
        }
        inWorld = nowInWorld;

        if (!nowInWorld) {
            return;
        }

        // 本地玩家死亡提示音
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !playerWasDead && deathCooldownTicks <= 0) {
            playUi(SoundIds.UI_PLAYER_DEATH);
            deathCooldownTicks = cfg.playerDeathCooldownSeconds * 20;
            cfg.log("本地玩家死亡 → 播放死亡提示音（仅自己可闻）");
        }
        playerWasDead = dead;
        if (deathCooldownTicks > 0) {
            deathCooldownTicks--;
        }

        // 228 整活：每 1200 tick 掷 1%（原版 Error228Handler.INTERVAL_TICKS / CHANCE）
        if (cfg.error228Enabled) {
            if (--error228Countdown <= 0) {
                error228Countdown = ERROR228_INTERVAL_TICKS;
                if (Compat.currentScreen(mc) == null && mc.player.isAlive() && RANDOM.nextFloat() < ERROR228_CHANCE) {
                    ScavVillagerClient.LOGGER.info("[老乡村民] 228 判定命中 → 弹出严重错误弹窗");
                    showError228(mc);
                }
            }
        }
    }

    private static void playJoinSound() {
        if (joinPlaysLeft <= 0) {
            return;
        }
        joinPlaysLeft--;
        playUi(SoundIds.UI_JOIN);
        if (joinPlaysLeft > 0) {
            Scheduler.after(JOIN_AUDIO_GAP_TICKS, UiSounds::playJoinSound);
        }
    }

    /** 界面音：非定位播放（SimpleSoundInstance.forUI），只自己听得到。 */
    public static void playUi(SoundEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F));
    }

    public static void showError228(Minecraft mc) {
        ScavConfig cfg = ScavConfig.get();
        if (cfg.error228Sound) {
            playUi(SoundIds.UI_ERROR_228);
        }
        Scheduler.after(1, () -> Compat.setScreen(mc, new Error228Screen()));
    }

    /** 配方提示音：原版延后 RECIPE_DELAY_TICKS 再播。 */
    public static void playRecipeDelayed() {
        Scheduler.after(RECIPE_DELAY_TICKS, () -> playUi(SoundIds.UI_RECIPE));
    }

    private UiSounds() {
    }
}
