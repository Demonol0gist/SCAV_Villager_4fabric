package com.arkit.scavvillager;

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
