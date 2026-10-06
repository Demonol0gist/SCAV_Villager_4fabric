package com.arkit.scavvillager;

import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;

/**
 * 提示音调度（原版 ClientUiEvents 里 ToastAddEvent 那部分的移植）。
 *
 * 原版 1.20.1 / 1.21.1 的 Toast 对象本身没有"该播什么声音"的接口：
 *   - 挑战成就的号角由 AdvancementToast.render 自己播；
 *   - 通用的 ui.toast.in / out 由 Toast.Visibility.playSound 播（所有提示共用）。
 * 所以这里做两件事：
 *   1. 播模组自己的提示音时，开一个静音窗口把原版的通用音压掉（原模组 MUTE_TOAST_IN_MILLIS = 1500）；
 *   2. 配合 AdvancementToastMixin 把号角换成 ui.achievement_challenge / ui.achievement_common。
 */
public final class ToastSounds {
    /** 原版常量：压掉通用提示音的时间窗。 */
    public static final long MUTE_TOAST_IN_MILLIS = 1500L;

    private static long muteToastInUntil;

    /** 成就提示音（挑战 / 普通）。 */
    public static void playAchievement(boolean challenge) {
        if (!ScavConfig.get().achievementSounds) {
            return;
        }
        openMuteWindow();
        UiSounds.playUi(challenge ? SoundIds.UI_ACHIEVEMENT_CHALLENGE : SoundIds.UI_ACHIEVEMENT_COMMON);
    }

    /** 配方解锁提示音（延后 RECIPE_DELAY_TICKS 再播，同原模组）。 */
    public static void playRecipe() {
        if (!ScavConfig.get().achievementSounds) {
            return;
        }
        openMuteWindow();
        UiSounds.playRecipeDelayed();
    }

    private static void openMuteWindow() {
        muteToastInUntil = Util.getMillis() + MUTE_TOAST_IN_MILLIS;
    }

    /** 静音窗口内到来的原版通用提示音（ui.toast.in）要被压掉。 */
    public static boolean shouldMute(Identifier soundId) {
        return soundId != null
                && "minecraft".equals(soundId.getNamespace())
                && "ui.toast.in".equals(soundId.getPath())
                && Util.getMillis() < muteToastInUntil;
    }

    public static void reset() {
        muteToastInUntil = 0L;
    }

    private ToastSounds() {
    }
}
