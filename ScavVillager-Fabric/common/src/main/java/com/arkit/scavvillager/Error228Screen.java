package com.arkit.scavvillager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * 228 整活弹窗（原版 Error228Screen 的移植）。
 * 塔科夫风格的"严重错误"：面板尺寸、配色都取自原模组常量，点"确认"会被踢回主菜单。
 * 默认开启（与原模组一致），配置里 error228Enabled = false 可以关掉。
 */
public class Error228Screen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 100;
    private static final int COLOR_PANEL = 0xFF0A0A0A;
    private static final int COLOR_BACKDROP = 0xFF101010;   // 不透明：半透明会把外面（模糊过的）世界透出来
    private static final int COLOR_TITLE = 0xFFC6C6C6;
    private static final int COLOR_TEXT = 0xFFC0C0BC;
    private static final int COLOR_BUTTON = 0xFF828282;
    private static final int COLOR_BUTTON_HOVER = 0xFF9C9C9C;
    private static final int COLOR_BUTTON_TEXT = 0xFF101010;
    private static final int BUTTON_WIDTH = 29;
    private static final int BUTTON_HEIGHT = 14;
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
        this.addRenderableWidget(new FlatButton(left + (PANEL_WIDTH - BUTTON_WIDTH) / 2, top + BUTTON_TOP,
                BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal(CONFIRM), b -> this.quitWorld()));
    }

    /** 点确认 → 退出世界回主菜单（原版："正在退出世界..."界面 → 标题界面）。 */
    private void quitWorld() {
        if (this.minecraft == null) {
            return;
        }
        Compat.quitWorld(this.minecraft, Compat.quittingScreen(Component.literal(QUITTING)));
        waitForLevelGone(0);
    }

    /**
     * 等世界真的卸载完再显示标题界面。
     * 不能只等固定 tick 数：卸载流程结束得更晚会把界面覆盖回去，结果卡在"正在退出世界"。
     */
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 不调用原版的 renderBackground：它会给世界加菜单背景模糊，看起来像蒙了一层滤镜。
        // 这里直接用自绘的深色底 + 面板，界面是清晰的。
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        // 用不带投影的 drawString：drawCenteredString 会加阴影，文字看着发虚
        String line1 = "228: " + TITLE + " [" + this.glitchCode + "]";
        String line2 = this.glitchCode.toUpperCase();
        graphics.drawString(this.font, line1, this.width / 2 - this.font.width(line1) / 2, top + 22, COLOR_TITLE, false);
        graphics.drawString(this.font, line2, this.width / 2 - this.font.width(line2) / 2, top + 42, COLOR_TEXT, false);
        // 注意：这里**不能**调 super.render(...) —— 原版 Screen#render 的第一件事就是 renderBackground()，
        // 会把（被游戏模糊过的）世界和暗色渐变重新盖到我们上面，界面就又糊了（按钮因为是随后画的所以是清楚的）。
        // 所以自己把控件画一遍即可（Screen.renderables 是 private，走公开的 children()）。
        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;      // 塔科夫式：只能点确认
    }

    private static class FlatButton extends Button {
        FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = this.isHoveredOrFocused() ? COLOR_BUTTON_HOVER : COLOR_BUTTON;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, COLOR_BUTTON_TEXT);
        }
    }
}
