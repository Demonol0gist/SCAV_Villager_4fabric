# -*- coding: utf-8 -*-
"""修 1.21.2+ 的 API 变动：
   1) SoundEvent.getLocation() → location()（1.21.2 起 record 化）
   2) Minecraft.getToasts() → getToastManager()
   3) GuiGraphics.blit 三套写法
   4) 1.21.6+ 的 disconnect(Screen) → disconnectWithSavingScreen()
   5) 旧版本项目排除 common 里的新形态 toast 文件（否则类重复）
"""
from pathlib import Path

ROOT = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
COMMON = ROOT / "common/src/main/java/com/arkit/scavvillager"

# ---------------- 1) 共享代码改成走 Compat ----------------
vg = (COMMON / "VoiceGovernor.java").read_text(encoding="utf-8")
vg = vg.replace("event.getLocation().getPath()", "Compat.soundId(event).getPath()")
vg = vg.replace("event.getLocation()", "Compat.soundId(event)")
(COMMON / "VoiceGovernor.java").write_text(vg, encoding="utf-8")
print("VoiceGovernor: getLocation() → Compat.soundId()")

ui = (COMMON / "UiSounds.java").read_text(encoding="utf-8")
ui = ui.replace("mc.getToasts().addToast(new IntroToast());", "Compat.addToast(mc, new IntroToast());")
(COMMON / "UiSounds.java").write_text(ui, encoding="utf-8")
print("UiSounds: getToasts() → Compat.addToast()")

it = (COMMON / "IntroToast.java").read_text(encoding="utf-8")
it = it.replace("graphics.blit(this.icon, 6, 6, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);",
                "Compat.blitToastIcon(graphics, this.icon, 6, 6, ICON_SIZE);")
(COMMON / "IntroToast.java").write_text(it, encoding="utf-8")
print("IntroToast(新形态): blit → Compat.blitToastIcon()")

# ---------------- 2) 各版本 Compat ----------------
OLD_COMPAT = '''package com.arkit.scavvillager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Generic{SCREEN}MessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** 版本差异垫片（{VER}）。 */
public final class Compat {{

    public static ResourceLocation rl(String namespace, String path) {{
        return {RL};
    }}

    /** 声音事件 id（1.21.2 起 SoundEvent 变成 record，getLocation() → location()）。 */
    public static ResourceLocation soundId(SoundEvent event) {{
        return {SOUND_ID};
    }}

    /** 原版"正在退出世界..."界面。 */
    public static Screen quittingScreen(Component message) {{
        return new Generic{SCREEN}MessageScreen(message);
    }}

    /** 退出当前世界并显示指定界面。 */
    public static void quitWorld(Minecraft mc, Screen screen) {{
        {QUIT}
    }}

    /** 弹一条提示框（1.21.2 起 ToastComponent 改名 ToastManager，getToasts() → getToastManager()）。 */
    public static void addToast(Minecraft mc, Toast toast) {{
        {ADD_TOAST}
    }}

    public static void renderBackground(Screen screen, GuiGraphics graphics) {{
        {RENDER_BG}
    }}

    private Compat() {{
    }}
}}
'''

# 各版本参数
BUCKETS = {
    "v1_20_1": dict(ver="1.20.1", screen="Dirt", rl="new ResourceLocation(namespace, path)",
                    sound_id="event.getLocation()", quit=
                    """if (mc.level != null) {
            mc.level.disconnect();
        }
        mc.clearLevel(screen);""",
                    add_toast="mc.getToasts().addToast(toast);",
                    render_bg="screen.renderBackground(graphics);"),
    "v1_21": dict(ver="1.21 / 1.21.1", screen="", rl="ResourceLocation.fromNamespaceAndPath(namespace, path)",
                  sound_id="event.getLocation()", quit="mc.disconnect(screen);",
                  add_toast="mc.getToasts().addToast(toast);",
                  render_bg="screen.renderBackground(graphics, 0, 0, 0.0F);"),
}

NEW_QUIT_125 = "mc.disconnect(screen);"
NEW_QUIT_16 = "mc.disconnectWithSavingScreen();"

BLIT_FUNCTION = """graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, icon,
                x, y, 0.0F, 0.0F, size, size, size, size);"""
BLIT_PIPELINE = """graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon,
                x, y, 0.0F, 0.0F, size, size, size, size);"""

for proj, ver, quit_, blit_ in [
    ("v1_21_2", "1.21.2 / 1.21.3", NEW_QUIT_125, BLIT_FUNCTION),
    ("v1_21_4", "1.21.4", NEW_QUIT_125, BLIT_FUNCTION),
    ("v1_21_5", "1.21.5", NEW_QUIT_125, BLIT_FUNCTION),
    ("v1_21_6", "1.21.6 – 1.21.8", NEW_QUIT_16, BLIT_PIPELINE),
    ("v1_21_9", "1.21.9 – 1.21.11", NEW_QUIT_16, BLIT_PIPELINE),
]:
    BUCKETS[proj] = dict(ver=ver, screen="", rl="ResourceLocation.fromNamespaceAndPath(namespace, path)",
                         sound_id="event.location()", quit=quit_,
                         add_toast="mc.getToastManager().addToast(toast);",
                         render_bg="screen.renderBackground(graphics, 0, 0, 0.0F);",
                         blit=blit_)

for proj, p in BUCKETS.items():
    src = OLD_COMPAT.format(SCREEN=p["screen"], VER=p["ver"], RL=p["rl"], SOUND_ID=p["sound_id"],
                            QUIT=p["quit"], ADD_TOAST=p["add_toast"], RENDER_BG=p["render_bg"])
    # 新形态的五个版本要额外提供画图标的方法
    if "blit" in p:
        src = src.replace("    private Compat() {", f"""    /** 画提示框图标（1.21.2 起 blit 需要 RenderType / RenderPipeline 参数）。 */
    public static void blitToastIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {{
        {p["blit"]}
    }}

    private Compat() {{""")
    path = ROOT / proj / "src/main/java/com/arkit/scavvillager/Compat.java"
    path.write_text(src, encoding="utf-8")
    print(f"{proj}: Compat 已更新（{p['ver']}）")

# ---------------- 3) 旧版本排除 common 里的新形态文件 ----------------
EXCLUDE = """sourceSets {
    main {
        java {
            srcDir rootProject.file('common/src/main/java')
            // common 里的 IntroToast / ToastManagerMixin 是 1.21.2+ 的新形态，
            // 本项目有自己的旧形态版本，这里要排除，否则类重复
            exclude 'com/arkit/scavvillager/IntroToast.java'
            exclude 'com/arkit/scavvillager/mixin/ToastManagerMixin.java'
        }
        resources.srcDir rootProject.file('common/src/main/resources')
    }
}"""

OLD_SOURCESETS = """sourceSets {
    main {
        java.srcDir rootProject.file('common/src/main/java')
        resources.srcDir rootProject.file('common/src/main/resources')
    }
}"""

for proj in ["v1_20_1", "v1_21"]:
    f = ROOT / proj / "build.gradle"
    t = f.read_text(encoding="utf-8")
    if OLD_SOURCESETS in t:
        f.write_text(t.replace(OLD_SOURCESETS, EXCLUDE), encoding="utf-8")
        print(f"{proj}: build.gradle 已加排除规则")
    else:
        print(f"{proj}: ！！没找到 sourceSets 块，需要手动处理")
