# -*- coding: utf-8 -*-
"""生成 26.2 专用源码：
   1) 把 common 里"非 GUI"的共享代码自动改写（类改名/换包）到 ScavVillager-Fabric-26/src26/java
   2) 手写 26.2 的 GUI 文件（Compat / IntroToast / Error228Screen）与成就提示音 mixin

26.2 的变动（javap 探明）：
   ResourceLocation → Identifier（net.minecraft.resources）
   Util 换包 → net.minecraft.util.Util
   Villager → world.entity.npc.villager，Pillager → world.entity.monster.illager，
   IronGolem → world.entity.animal.golem
   SoundInstance.getLocation() → getIdentifier()
   Minecraft.setScreen → setScreenAndShow
   GuiGraphics → GuiGraphicsExtractor，render 家族 → extract*（两段式渲染）
   Toast：render(GuiGraphics,...) → extractRenderState(GuiGraphicsExtractor, Font, long)
   Minecraft.getToastManager() → Minecraft.gui.toastManager()
"""
from pathlib import Path

COMMON = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric\common\src\main\java\com\arkit\scavvillager")
TOASTNEW = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric\common\toast-new\java\com\arkit\scavvillager")
PROJ = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric-26")
OUT = PROJ / "src26/java/com/arkit/scavvillager"

# GUI 相关的文件 26.2 要手写，不走自动改写
SKIP = {"SoundEngineMixin.java", "IntroToast.java", "Error228Screen.java", "Compat.java", "SoundManagerMixin.java", "ContainerScreenMixin.java"}

REPLACEMENTS = [
    ("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;"),
    ("import net.minecraft.Util;", "import net.minecraft.util.Util;"),
    ("import net.minecraft.world.entity.npc.Villager;", "import net.minecraft.world.entity.npc.villager.Villager;"),
    ("import net.minecraft.world.entity.monster.Pillager;", "import net.minecraft.world.entity.monster.illager.Pillager;"),
    ("import net.minecraft.world.entity.animal.IronGolem;", "import net.minecraft.world.entity.animal.golem.IronGolem;"),
    ("ResourceLocation", "Identifier"),          # 简单名（含泛型/声明）
    (".getLocation()", ".getIdentifier()"),      # SoundInstance 的 id 取值
    (".setScreen(", ".setScreenAndShow("),       # Minecraft 的切换界面
]

def transform(text: str) -> str:
    for a, b in REPLACEMENTS:
        text = text.replace(a, b)
    return text

# 先清空输出目录，避免上次生成残留（会和其他源目录撞成类重复）
if OUT.exists():
    import shutil as _sh
    _sh.rmtree(OUT)

count = 0
for src_dir in (COMMON, TOASTNEW):
    for f in sorted(src_dir.rglob("*.java")):
        if f.name in SKIP:
            continue
        rel = f.relative_to(src_dir)
        dst = OUT / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(transform(f.read_text(encoding="utf-8")), encoding="utf-8")
        count += 1
        print(f"改写 {rel}")

# ---------------- 手写：Compat（26.2） ----------------
(OUT / "Compat.java").write_text('''package com.arkit.scavvillager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** 版本差异垫片（26.2：类改名/换包 + 渲染状态模型）。 */
public final class Compat {

    public static Identifier rl(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    /** 声音事件 id（26.2 里叫 location()）。 */
    public static Identifier soundId(SoundEvent event) {
        return event.location();
    }

    /** 播放实例的音频 id（26.2：SoundInstance.getIdentifier()）。 */
    public static Identifier soundId(net.minecraft.client.resources.sounds.SoundInstance instance) {
        return instance.getIdentifier();
    }

    /** 原版"正在退出世界..."界面。 */
    public static Screen quittingScreen(Component message) {
        return new GenericMessageScreen(message);
    }

    public static void quitWorld(Minecraft mc, Screen screen) {
        mc.disconnectWithSavingScreen();
    }

    /** 弹提示框（26.2：Minecraft.gui.toastManager()）。 */
    public static void addToast(Minecraft mc, Toast toast) {
        mc.gui.toastManager().addToast(toast);
    }

    /** 当前打开的界面（26.2 起 Minecraft.screen 挪到了 Gui.screen()）。 */
    public static Screen currentScreen(Minecraft mc) {
        return mc.gui.screen();
    }

    /** 26.2 的背景改成 extractBackground(提取器, 鼠标X, 鼠标Y, 部分刻)。 */
    public static void renderBackground(Screen screen, GuiGraphicsExtractor graphics,
                                       int mouseX, int mouseY, float partialTick) {
        screen.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    private Compat() {
    }
}
''', encoding="utf-8")
print("手写 Compat.java")

# ---------------- 手写：IntroToast（26.2 形态） ----------------
(OUT / "IntroToast.java").write_text('''package com.arkit.scavvillager;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * 入场提示框（26.2 形态：extractRenderState(GuiGraphicsExtractor, Font, long)）。
 * 尺寸 / 配色取自原模组常量：200x32、文字起始 x=30、文本宽 164、显示 6 秒。
 * 26.2 的贴图绘制需要 RenderPipeline 参数，这里就不画头像了，只留面板 + 文字。
 */
public class IntroToast implements Toast {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_WIDTH = 164;
    private static final int ICON_SIZE = 20;
    private static final int TITLE_COLOR = 0xFFFFFF55;
    private static final int DESCRIPTION_COLOR = 0xFFFFFFFF;
    private static final long DISPLAY_TIME_MILLIS = 6000L;

    private static final String TITLE = "成就达成！";
    private static final String[] DESCRIPTIONS = {
            "你听说有群无法无天的的Scav聚集在森林里面搞无政府派对的事吗？",
            "那帮造反的Scav还在蠢蠢欲动，想要在自然保护区里啃下一块自己的地盘...........",
            "有些Scav过了两天没人管的日子就被自由的幻想冲昏了头脑.......",
    };
    private static final Identifier[] ICONS = {
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_jaeger.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_fence.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast.png"),
    };
    private static final Object FIRST_SCAV = new Object();
    private static final RandomSource RANDOM = RandomSource.create();

    /** 贴图取不到时就不再尝试，少一张图标总比崩游戏好。 */
    private static boolean iconUnavailable;

    private final Identifier icon;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long timeSinceLastVisible) {
        // 深色面板 + 一圈描边（原模组是自绘样式）
        graphics.fill(0, 0, WIDTH, HEIGHT, 0xF0101010);
        graphics.fill(0, 0, WIDTH, 1, 0xFF505050);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, 0xFF505050);
        graphics.fill(0, 0, 1, HEIGHT, 0xFF505050);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, 0xFF505050);

        if (!iconUnavailable) {
            try {
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.icon,
                        6, 6, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } catch (Throwable t) {
                iconUnavailable = true;
                ScavVillagerClient.LOGGER.warn("[老乡村民] 入场提示框图标加载失败，已跳过图标: {}", t.toString());
            }
        }

        graphics.text(font, TITLE, TEXT_X, 7, TITLE_COLOR, false);
        graphics.textWithWordWrap(font, Component.literal(this.description), TEXT_X, 18, TEXT_WIDTH, DESCRIPTION_COLOR);
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
''', encoding="utf-8")
print("手写 IntroToast.java")

# ---------------- 手写：Error228Screen（26.2 形态） ----------------
(OUT / "Error228Screen.java").write_text('''package com.arkit.scavvillager;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * 228 整活弹窗（26.2 形态：绘制走渲染状态提取，按钮用标准 Button）。
 * 面板尺寸、配色取自原模组常量，点"确认"退出世界回主菜单。
 */
public class Error228Screen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 100;
    private static final int COLOR_PANEL = 0xFF0A0A0A;
    private static final int COLOR_BACKDROP = 0xB0101010;
    private static final int COLOR_TITLE = 0xFFC6C6C6;
    private static final int COLOR_TEXT = 0xFFC0C0BC;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_TOP = 73;

    private static final String TITLE = "严重错误";
    private static final String CONFIRM = "确认";
    private static final String QUITTING = "正在退出世界...";
    private static final String HEX_CHARS = "0123456789abcdef";

    private final String glitchCode;

    public Error228Screen() {
        super(Component.literal(TITLE));
        this.glitchCode = randomHex(RandomSource.create(), 8);
    }

    private static String randomHex(RandomSource random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(CONFIRM), b -> this.quitWorld())
                .bounds(left + (PANEL_WIDTH - BUTTON_WIDTH) / 2, top + BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /** 点确认 → 退出世界回主菜单。 */
    private void quitWorld() {
        if (this.minecraft == null) {
            return;
        }
        Compat.quitWorld(this.minecraft, Compat.quittingScreen(Component.literal(QUITTING)));
        Scheduler.after(25, () -> this.minecraft.setScreenAndShow(new TitleScreen()));
    }

    /** 背景阶段绘制：面板 + 标题（控件由基类随后画在上面）。 */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.centeredText(this.font, Component.literal("228: " + TITLE + " [" + this.glitchCode + "]"),
                this.width / 2, top + 22, COLOR_TITLE);
        graphics.centeredText(this.font, Component.literal(this.glitchCode.toUpperCase()),
                this.width / 2, top + 42, COLOR_TEXT);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;      // 塔科夫式：只能点确认
    }
}
''', encoding="utf-8")
print("手写 Error228Screen.java")

# ---------------- 手写：成就提示音 mixin（26.2 形态 B 仍适用） ----------------
(OUT / "mixin/AdvancementToastMixin.java").write_text('''package com.arkit.scavvillager.mixin;

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
 * 成就提示音（26.2：AdvancementToast 仍然自己实现 getSoundEvent()）。
 * 挑战成就 → ui.achievement_challenge，其余（原版没声音）→ ui.achievement_common。
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
''', encoding="utf-8")
print("手写 AdvancementToastMixin.java")
print(f"\n共改写 {count} 个共享文件，手写 4 个 26.2 专用文件")


# ---------------- 手写：SoundManagerMixin（26.2：play() 返回 SoundEngine.PlayResult） ----------------
(OUT / "mixin/SoundManagerMixin.java").write_text('''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.Compat;
import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端放音前拦一道：村民 / 掠夺者的语音改成绑定实体播放（跟随移动）。
 *
 * 26.2 形态：SoundManager.play 有返回值（SoundEngine.PlayResult），取消时要返回 NOT_STARTED；
 * playDelayed 仍是 void。
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ToastSounds.shouldMute(Compat.soundId(instance))) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, int delay, CallbackInfo ci) {
        if (VoiceBinder.tryRebind(instance, delay)) {
            ci.cancel();
        }
    }
}
''', encoding="utf-8")
print("手写 SoundManagerMixin.java（26.2 形态）")


# ---------------- 手写：ContainerScreenMixin（26.2：ClickType → ContainerInput） ----------------
(OUT / "mixin/ContainerScreenMixin.java").write_text('''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.TradeWatcher;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.2 形态：ClickType 已改名 ContainerInput。 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void scavVillager(Slot slot, int slotId, int mouseButton, ContainerInput type, CallbackInfo ci) {
        if (slot != null && slot.index == 2 && (Object) this instanceof MerchantScreen) {
            TradeWatcher.notePurchaseClick();
        }
    }
}
''', encoding="utf-8")
print("手写 ContainerScreenMixin.java（26.2 形态）")


# ---------------- 手写：SoundEngineMixin（26.2：getIdentifier + PlayResult） ----------------
(OUT / "mixin/SoundEngineMixin.java").write_text('''package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 兜底拦截（26.2：getIdentifier + PlayResult）。 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ToastSounds.shouldMute(instance.getIdentifier())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
''', encoding="utf-8")
print("手写 SoundEngineMixin.java（26.2 形态）")
