package com.arkit.scavvillager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * 入场提示框（原版 IntroToast 的移植）：假装是一条"成就达成"，配一张老乡头像和随机风味文案。
 * 尺寸 / 配色取自原模组常量：200x32、图标 20x20、文字起始 x=30、文本宽 164、显示 6 秒。
 */
public class IntroToast implements Toast {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_WIDTH = 164;
    private static final int ICON_SIZE = 20;
    private static final int TITLE_COLOR = 0xFFFFFF55;   // 注意带上不透明位，否则新渲染管线下文字全透明
    private static final int DESCRIPTION_COLOR = 0xFFFFFFFF;
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

    /** 贴图取不到时就不再尝试（资源包冲突、开发环境等），少一张图标总比崩游戏好。 */
    private static boolean iconUnavailable;

    private final ResourceLocation icon;
    private final String description;

    public IntroToast() {
        this.icon = ICONS[RANDOM.nextInt(ICONS.length)];
        this.description = DESCRIPTIONS[RANDOM.nextInt(DESCRIPTIONS.length)];
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
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

        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, TITLE, TEXT_X, 7, TITLE_COLOR, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(this.description), TEXT_WIDTH);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), TEXT_X, 18 + i * 10, DESCRIPTION_COLOR, false);
        }
        return timeSinceLastVisible >= DISPLAY_TIME_MILLIS ? Visibility.HIDE : Visibility.SHOW;
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
