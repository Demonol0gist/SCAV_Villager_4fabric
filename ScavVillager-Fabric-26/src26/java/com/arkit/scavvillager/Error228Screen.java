package com.arkit.scavvillager;

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
    private static final int COLOR_BACKDROP = 0xFF101010;   // 不透明
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
        waitForLevelGone(0);
    }

    /** 等世界真的卸载完再显示标题界面（不能只等固定 tick 数）。 */
    private void waitForLevelGone(int tries) {
        Scheduler.after(2, () -> {
            if (this.minecraft == null) {
                return;
            }
            if (this.minecraft.level != null && tries < 100) {
                waitForLevelGone(tries + 1);
            } else {
                Compat.setScreen(this.minecraft, new TitleScreen());
            }
        });
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
