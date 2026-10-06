package com.arkit.scavvillager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** 版本差异垫片（1.21.9 – 1.21.11）。 */
public final class Compat {

    public static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    /** 声音事件 id（1.21.2 起 SoundEvent 变成 record，getLocation() → location()）。 */
    public static ResourceLocation soundId(SoundEvent event) {
        return event.location();
    }

    /** 原版"正在退出世界..."界面。 */
    public static Screen quittingScreen(Component message) {
        return new GenericMessageScreen(message);
    }

    /** 退出当前世界并显示指定界面。 */
    public static void quitWorld(Minecraft mc, Screen screen) {
        mc.disconnectWithSavingScreen();
    }

    /** 弹一条提示框（1.21.2 起 ToastComponent 改名 ToastManager，getToasts() → getToastManager()）。 */
    public static void addToast(Minecraft mc, Toast toast) {
        mc.getToastManager().addToast(toast);
    }

    public static void renderBackground(Screen screen, GuiGraphics graphics) {
        screen.renderBackground(graphics, 0, 0, 0.0F);
    }

    /** 画提示框图标（1.21.2 起 blit 需要 RenderType / RenderPipeline 参数）。 */
    public static void blitToastIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon,
                x, y, 0.0F, 0.0F, size, size, size, size);
    }


    /** 当前打开的界面（26.2 起 Minecraft.screen 挪到了 Gui.screen()）。 */
    public static Screen currentScreen(Minecraft mc) {
        return mc.screen;
    }


    /** 切换界面（26.2 起 setScreen 改名 setScreenAndShow）。 */
    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreenAndShow(screen);
    }

    private Compat() {
    }
}
