# -*- coding: utf-8 -*-
"""修入场提示框：
   1) 文字颜色缺 alpha 位（0xFFFF55 / 0xFFFFFF → 加 FF）——新渲染管线不会自动补，导致文字全透明
   2) 26.2 的提示框把头像一个补回来（用原版同款 RenderPipelines.GUI_TEXTURED）
"""
from pathlib import Path

ROOT = Path(r"C:\Users\Admin\Downloads\SCAV\ScavVillager-Fabric")
GEN = Path(r"C:\Users\Admin\Downloads\SCAV\_toolchain\make_26_sources.py")

# ---------- 1) 三个静态 IntroToast 文件：补 alpha ----------
for f in [ROOT / "common/toast-new/java/com/arkit/scavvillager/IntroToast.java",
          ROOT / "v1_20_1/src/main/java/com/arkit/scavvillager/IntroToast.java",
          ROOT / "v1_21/src/main/java/com/arkit/scavvillager/IntroToast.java"]:
    t = f.read_text(encoding="utf-8")
    t = t.replace("private static final int TITLE_COLOR = 0xFFFF55;",
                  "private static final int TITLE_COLOR = 0xFFFFFF55;   // 注意带上不透明位，否则新渲染管线下文字全透明")
    t = t.replace("private static final int DESCRIPTION_COLOR = 0xFFFFFF;",
                  "private static final int DESCRIPTION_COLOR = 0xFFFFFFFF;")
    f.write_text(t, encoding="utf-8")
    print(f"{f.relative_to(ROOT.parent)}: 文字颜色已补 alpha ✓")

# ---------- 2) 26.2 模板：补 alpha + 恢复头像 ----------
src = GEN.read_text(encoding="utf-8")

# 2a) 颜色
src = src.replace("private static final int TITLE_COLOR = 0xFFFF55;",
                  "private static final int TITLE_COLOR = 0xFFFFFF55;")
src = src.replace("private static final int DESCRIPTION_COLOR = 0xFFFFFF;",
                  "private static final int DESCRIPTION_COLOR = 0xFFFFFFFF;")

# 2b) 加上头像用的字段与素材表
old_fields = """    private static final Object FIRST_SCAV = new Object();
    private static final RandomSource RANDOM = RandomSource.create();

    private final String description;"""
new_fields = """    private static final ResourceLocation[] ICONS = {
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_jaeger.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast_fence.png"),
            Compat.rl(SoundIds.MOD, "textures/gui/intro_toast.png"),
    };
    private static final Object FIRST_SCAV = new Object();
    private static final RandomSource RANDOM = RandomSource.create();

    /** 贴图取不到时就不再尝试，少一张图标总比崩游戏好。 */
    private static boolean iconUnavailable;

    private final Identifier icon;
    private final String description;"""
assert old_fields in src, "26.2 模板里没找到字段锚点"
src = src.replace(old_fields, new_fields, 1)

src = src.replace("""    public IntroToast() {
        this.description = DESCRIPTIONS[RANDOM.nextInt(DESCRIPTIONS.length)];
    }""",
"""    public IntroToast() {
        this.icon = ICONS[RANDOM.nextInt(ICONS.length)];
        this.description = DESCRIPTIONS[RANDOM.nextInt(DESCRIPTIONS.length)];
    }""")

# 2c) 恢复画头像（原版按钮/容器界面用的就是这个管线）
old_body = """        graphics.text(font, TITLE, TEXT_X, 7, TITLE_COLOR, false);"""
new_body = """        if (!iconUnavailable) {
            try {
                graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.icon,
                        6, 6, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            } catch (Throwable t) {
                iconUnavailable = true;
                ScavVillagerClient.LOGGER.warn("[老乡村民] 入场提示框图标加载失败，已跳过图标: {}", t.toString());
            }
        }

        graphics.text(font, TITLE, TEXT_X, 7, TITLE_COLOR, false);"""
assert old_body in src, "26.2 模板里没找到绘制锚点"
src = src.replace(old_body, new_body, 1)

# 2d) 补上 ICON_SIZE 常量与 import（模板里原来没有头像，所以 ICON_SIZE 可能缺）
if "private static final int ICON_SIZE" not in src:
    src = src.replace("    private static final int TEXT_WIDTH = 164;",
                      "    private static final int TEXT_WIDTH = 164;\n    private static final int ICON_SIZE = 20;")
if "import net.minecraft.resources.Identifier;" not in src:
    src = src.replace("import net.minecraft.network.chat.Component;",
                      "import net.minecraft.network.chat.Component;\nimport net.minecraft.resources.Identifier;")

GEN.write_text(src, encoding="utf-8")
print("make_26_sources.py: 颜色已补 alpha + 头像已恢复 ✓")
