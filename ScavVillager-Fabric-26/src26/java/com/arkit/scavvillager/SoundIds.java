package com.arkit.scavvillager;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * 声音事件。
 * 原 Forge 模组需要在注册表里注册，Fabric 客户端只要构造出同 id 的 SoundEvent 即可播放
 * （客户端播放时会按 id 去 sounds.json 里找音频，不校验注册表），所以这里不需要注册。
 * 只列出 sounds.json 里真实存在的事件，避免构造出空事件。
 */
public final class SoundIds {
    public static final String MOD = "scav_villager";

    private static SoundEvent ev(String path) {
        return SoundEvent.createVariableRangeEvent(Compat.rl(MOD, path));
    }

    // ---- 村民（模组命名空间里定义了这些事件）----
    public static final SoundEvent VILLAGER_AMBIENT = ev("entity.villager.ambient");
    public static final SoundEvent VILLAGER_HURT = ev("entity.villager.hurt");
    public static final SoundEvent VILLAGER_DEATH = ev("entity.villager.death");
    public static final SoundEvent VILLAGER_YES = ev("entity.villager.yes");
    public static final SoundEvent VILLAGER_NO = ev("entity.villager.no");
    public static final SoundEvent VILLAGER_ALERT = ev("entity.villager.alert");
    public static final SoundEvent VILLAGER_GRENADE = ev("entity.villager.grenade");
    public static final SoundEvent VILLAGER_WITNESS = ev("entity.villager.witness");
    /** 砍价语音（选中商品时播；文件来自原模组的 haggle1..25）。 */
    public static final SoundEvent VILLAGER_HAGGLE = ev("entity.villager.haggle");

    // ---- 掠夺者 ----
    public static final SoundEvent PILLAGER_ALERT = ev("entity.pillager.alert");
    public static final SoundEvent PILLAGER_FIGHT = ev("entity.pillager.fight");
    public static final SoundEvent PILLAGER_CLEAR = ev("entity.pillager.clear");
    public static final SoundEvent PILLAGER_GRENADE = ev("entity.pillager.grenade");
    public static final SoundEvent PILLAGER_WITNESS = ev("entity.pillager.witness");

    // ---- UI ----
    public static final SoundEvent UI_JOIN = ev("ui.join");
    public static final SoundEvent UI_RECIPE = ev("ui.recipe");
    public static final SoundEvent UI_PLAYER_DEATH = ev("ui.player_death");
    public static final SoundEvent UI_ACHIEVEMENT_COMMON = ev("ui.achievement_common");
    public static final SoundEvent UI_ACHIEVEMENT_CHALLENGE = ev("ui.achievement_challenge");
    public static final SoundEvent UI_ERROR_228 = ev("ui.error_228");

    private SoundIds() {
    }
}
