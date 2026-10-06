# -*- coding: utf-8 -*-
"""把 toast 相关代码按 "旧形态(≤1.21.1) / 1.21.2-1.21.4 / ≥1.21.5" 三种形态拆开。

旧形态（ToastComponent + render(GuiGraphics,ToastComponent,long)）→ 放到 v1_20_1 与 v1_21 各自的源码目录
新形态（ToastManager + render(GuiGraphics,Font,long)）→ 放在 common（1.21.2+ 的五个分组共用）
成就提示音的两套实现按分组放各自目录（因为要 @Shadow 的字段不一样）
"""
from pathlib import Path

ROOT = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
COMMON = ROOT / "common/src/main/java/com/arkit/scavvillager"
COMMON_RES = ROOT / "common/src/main/resources"

# ---------- 1) 把旧形态的文件复制到 v1_20_1 / v1_21 ----------
old_introtoast = (COMMON / "IntroToast.java").read_text(encoding="utf-8")
old_toastcomponent_mixin = (COMMON / "mixin/ToastComponentMixin.java").read_text(encoding="utf-8")
old_mixins_json = (COMMON_RES / "scav_villager.mixins.json").read_text(encoding="utf-8")

for old_proj in ["v1_20_1", "v1_21"]:
    base = ROOT / old_proj / "src/main/java/com/arkit/scavvillager"
    (base / "mixin").mkdir(parents=True, exist_ok=True)
    (base / "IntroToast.java").write_text(old_introtoast, encoding="utf-8")
    (base / "mixin/ToastComponentMixin.java").write_text(old_toastcomponent_mixin, encoding="utf-8")
    res = ROOT / old_proj / "src/main/resources"
    res.mkdir(parents=True, exist_ok=True)
    (res / "scav_villager.mixins.json").write_text(old_mixins_json, encoding="utf-8")
    print(f"{old_proj}: 已放入旧形态 IntroToast / ToastComponentMixin / mixins.json")

# ---------- 2) common：换成新形态 ----------
(COMMON / "mixin/ToastComponentMixin.java").unlink(missing_ok=True)
(COMMON_RES / "scav_villager.mixins.json").unlink(missing_ok=True)

NEW_INTRO_TOAST = '''package com.arkit.scavvillager;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * 入场提示框（1.21.2 起的新版 Toast 接口：render(GuiGraphics, Font, long) +
 * getWantedVisibility() + update(ToastManager, long)）。
 * 尺寸 / 配色取自原模组常量：200x32、图标 20x20、文字起始 x=30、文本宽 164、显示 6 秒。
 */
public class IntroToast implements Toast {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_WIDTH = 164;
    private static final int ICON_SIZE = 20;
    private static final int TITLE_COLOR = 0xFFFF55;
    private static final int DESCRIPTION_COLOR = 0xFFFFFF;
    private static final long DISPLAY_TIME_MILLIS = 6000L;

    private static final String TITLE = "成就达成！";
    private static final String[] DESCRIPTIONS = {
            "你听说有群无法无天的的Scav聚集在森林里面搞无政府派对的事吗？",
            "那帮造反的Scav还在蠢蠢欲动，想要在自然保护区里啃下一块自己的地盘...........",
            "有些Scav过了两天没人管的日子就被自由的幻想冲昏了头脑.......",
    };
    private static final ResourceLocation[] ICONS = {
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_jaeger.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_fence.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast.png"),
    };
    /** 同一时间只留一条入场提示。 */
    private static final Object FIRST_SCAV = new Object();

    private static final RandomSource RANDOM = RandomSource.create();

    /** 贴图取不到时就不再尝试，少一张图标总比崩游戏好。 */
    private static boolean iconUnavailable;

    private final ResourceLocation icon;
    private final String description;
    private long lastVisibleTime;

    public IntroToast() {
        this.icon = ICONS[RANDOM.nextInt(ICONS.length)];
        this.description = DESCRIPTIONS[RANDOM.nextInt(DESCRIPTIONS.length)];
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.lastVisibleTime >= DISPLAY_TIME_MILLIS ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void update(ToastManager manager, long timeSinceLastVisible) {
        this.lastVisibleTime = timeSinceLastVisible;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, long timeSinceLastVisible) {
        // 深色面板 + 一圈描边（原模组是自绘样式）
        graphics.fill(0, 0, WIDTH, HEIGHT, 0xF0101010);
        graphics.fill(0, 0, WIDTH, 1, 0xFF505050);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, 0xFF505050);
        graphics.fill(0, 0, 1, HEIGHT, 0xFF505050);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, 0xFF505050);

        if (!iconUnavailable) {
            try {
                graphics.blit(this.icon, 6, 6, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } catch (Throwable t) {
                iconUnavailable = true;
                ScavVillagerClient.LOGGER.warn("[老乡村民] 入场提示框图标加载失败，已跳过图标: {}", t.toString());
            }
        }

        graphics.drawString(font, TITLE, TEXT_X, 7, TITLE_COLOR, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(this.description), TEXT_WIDTH);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), TEXT_X, 18 + i * 10, DESCRIPTION_COLOR, false);
        }
    }

    @Override
    public Object getToken() {
        return FIRST_SCAV;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }
}
'''

NEW_TOAST_MANAGER_MIXIN = '''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 配方解锁提示音（1.21.2 起 ToastComponent 改名为 ToastManager）。
 * 原版只播通用的 ui.toast.in，这里换成模组的 ui.recipe（并开静音窗口压掉通用音）。
 */
@Mixin(ToastManager.class)
public class ToastManagerMixin {

    @Inject(method = "addToast", at = @At("HEAD"))
    private void scavVillager$recipeSound(Toast toast, CallbackInfo ci) {
        if (toast instanceof RecipeToast) {
            ToastSounds.playRecipe();
        }
    }
}
'''

(COMMON / "IntroToast.java").write_text(NEW_INTRO_TOAST, encoding="utf-8")
(COMMON / "mixin/ToastManagerMixin.java").write_text(NEW_TOAST_MANAGER_MIXIN, encoding="utf-8")
print("common: 已换成新形态 IntroToast + ToastManagerMixin（旧的已删除）")

# ---------- 3) 各 1.21.x 分组：成就提示音的两套实现 + mixins.json ----------
SHAPE_A = '''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.ToastSounds;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 成就提示音（1.21.2 / 1.21.4 形态：AdvancementToast 里有 playedSound 标志位）。
 * 在 update 开头抢先置位 playedSound，让原版那句号角不再播，改播模组音效：
 * 挑战成就 → ui.achievement_challenge，普通成就 → ui.achievement_common。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Shadow
    @Final
    private AdvancementHolder advancement;

    @Shadow
    private boolean playedSound;

    @Inject(method = "update", at = @At("HEAD"))
    private void scavVillager$achievementSound(ToastManager manager, long timeSinceLastVisible, CallbackInfo ci) {
        if (this.playedSound || !ScavConfig.get().achievementSounds) {
            return;
        }
        Optional<DisplayInfo> display = this.advancement.value().display();
        boolean challenge = display.map(info -> info.getType() == AdvancementType.CHALLENGE).orElse(false);
        this.playedSound = true;
        ToastSounds.playAchievement(challenge);
    }
}
'''

SHAPE_B = '''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.SoundIds;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 成就提示音（1.21.5+ 形态：AdvancementToast 自带 getSoundEvent()）。
 * 直接替换返回值：原版只在挑战成就时返回号角，这里把挑战换成 ui.achievement_challenge，
 * 其余（原版返回 null、没声音）补上 ui.achievement_common。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Inject(method = "getSoundEvent", at = @At("RETURN"), cancellable = true)
    private void scavVillager$achievementSound(CallbackInfoReturnable<SoundEvent> cir) {
        if (!ScavConfig.get().achievementSounds) {
            return;
        }
        SoundEvent vanilla = cir.getReturnValue();
        if (vanilla == SoundEvents.UI_TOAST_CHALLENGE_COMPLETE) {
            cir.setReturnValue(SoundIds.UI_ACHIEVEMENT_CHALLENGE);
        } else {
            cir.setReturnValue(SoundIds.UI_ACHIEVEMENT_COMMON);
        }
    }
}
'''

MIXINS_JSON = '''{{
  "required": false,
  "minVersion": "0.8",
  "package": "com.arkit.scavvillager.mixin",
  "compatibilityLevel": "JAVA_17",
  "client": [
    "ClientPacketListenerMixin",
    "MinecraftMixin",
    "SoundManagerMixin",
    "ToastManagerMixin",
    "AdvancementToastMixin"
  ],
  "injectors": {{
    "defaultRequire": 0
  }},
  "refmap": "scav_villager.refmap.json"
}}
'''

SHAPE_A_GROUPS = ["v1_21_2", "v1_21_4"]
SHAPE_B_GROUPS = ["v1_21_5", "v1_21_6", "v1_21_9"]

for proj in SHAPE_A_GROUPS + SHAPE_B_GROUPS:
    mixin_dir = ROOT / proj / "src/main/java/com/arkit/scavvillager/mixin"
    mixin_dir.mkdir(parents=True, exist_ok=True)
    shape = SHAPE_A if proj in SHAPE_A_GROUPS else SHAPE_B
    (mixin_dir / "AdvancementToastMixin.java").write_text(shape, encoding="utf-8")
    (ROOT / proj / "src/main/resources/scav_villager.mixins.json").write_text(MIXINS_JSON, encoding="utf-8")
    print(f"{proj}: AdvancementToastMixin = {'形态A(playedSound)' if proj in SHAPE_A_GROUPS else '形态B(getSoundEvent)'}")
