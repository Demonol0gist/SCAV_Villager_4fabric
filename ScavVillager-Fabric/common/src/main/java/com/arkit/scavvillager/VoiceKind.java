package com.arkit.scavvillager;

/**
 * 语音种类、优先级与"音量等级"。
 * 数值照抄原模组：ClientVoiceBinder.KIND_* / PRIORITY_*、VoiceChannel.LEVEL_*。
 */
public final class VoiceKind {
    public static final int OTHER = 0;
    public static final int IDLE = 1;
    public static final int HURT = 2;
    public static final int FIGHT = 3;
    public static final int ALERT = 4;
    public static final int WITNESS = 5;
    public static final int CLEAR = 6;
    public static final int DEATH = 7;
    public static final int GRENADE = 8;
    /** 交易成交：要立刻出声，也享受打断特权（和死亡同级处理）。 */
    public static final int TRADE = 9;

    /** 同一只生物同时只播一条语音；优先级更高者可以打断正在播的。 */
    public static int priority(int kind) {
        return switch (kind) {
            case GRENADE -> 40;                     // PRIORITY_GRENADE
            case TRADE -> 36;                       // 介于死亡(35)与手雷之间
            case DEATH -> 35;                       // PRIORITY_DEATH
            case HURT -> 30;                        // 原版没有单独常量，取死亡与战斗之间
            case FIGHT, ALERT, CLEAR -> 20;         // PRIORITY_COMBAT
            default -> 0;                           // PRIORITY_IDLE
        };
    }

    /** 原版 VoiceChannel.LEVEL_*，等级高的语音会让低等级的等待。 */
    public static int level(int kind) {
        return switch (kind) {
            case DEATH -> 4;                        // LEVEL_DEATH
            case GRENADE -> 3;                      // LEVEL_GRENADE
            case WITNESS -> 2;                      // LEVEL_WITNESS
            case FIGHT, ALERT, CLEAR, HURT -> 1;    // LEVEL_FIGHT_ALERT
            default -> 0;                           // LEVEL_IDLE
        };
    }

    /** 死亡语音要留在原地播完（原版 BoundVoiceSound.keepAfterDeath）。 */
    public static boolean keepAfterDeath(int kind) {
        return kind == DEATH;
    }

    private VoiceKind() {
    }
}
