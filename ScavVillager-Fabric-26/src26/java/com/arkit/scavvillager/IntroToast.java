package com.arkit.scavvillager;

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
