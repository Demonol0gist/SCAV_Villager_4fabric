package com.arkit.scavvillager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 配置界面（用 /scav 打开）。
 *
 * 只用原版按钮控件、不手绘任何东西 —— 这样 1.20.1 到 26.2 的绘制 API 差异都不用管
 * （26.2 把 GUI 绘制改成了"渲染状态提取"模型，手绘界面得写两套）。
 *
 * 每点一下即时生效并写回配置文件（各项都是读取时现取的，不用重启也不用重进世界）。
 */
public class ScavConfigScreen extends Screen {
    private final Screen parent;

    public ScavConfigScreen() {
        this(null);
    }

    public ScavConfigScreen(Screen parent) {
        super(Component.literal("老乡村民 · 配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ScavConfig cfg = ScavConfig.get();
        int left = this.width / 2 - 100;
        int y = 34;

        // —— 228 彩蛋（本次要求新增的开关）——
        y = toggle("228 崩溃彩蛋", cfg.error228Enabled, v -> cfg.error228Enabled = v, left, y);
        // —— 常用开关 ——
        y = toggle("模组总开关", cfg.enableMod, v -> cfg.enableMod = v, left, y);
        y = toggle("村民语音", cfg.enableVillager, v -> cfg.enableVillager = v, left, y);
        y = toggle("掠夺者语音", cfg.enablePillager, v -> cfg.enablePillager = v, left, y);
        y = toggle("成就 / 配方提示音", cfg.achievementSounds, v -> cfg.achievementSounds = v, left, y);
        y = toggle("进入世界的提示音 + 提示框", cfg.joinSound, v -> cfg.joinSound = v, left, y);
        y = toggle("语音跟随实体（关掉回到原版固定位置）", cfg.followEntities, v -> cfg.followEntities = v, left, y);
        y = toggle("详细日志（排错用）", cfg.verboseLog, v -> cfg.verboseLog = v, left, y);

        this.addRenderableWidget(Button.builder(Component.literal("完成"), b -> this.onClose())
                .bounds(left, y + 12, 200, 20).build());
    }

    /** 一个开/关按钮：标题上直接显示状态，点一下即时生效并写盘。 */
    private int toggle(String name, boolean initial, Consumer<Boolean> apply, int left, int y) {
        boolean[] state = {initial};
        Button button = Button.builder(label(name, state[0]), b -> {
            state[0] = !state[0];
            apply.accept(state[0]);
            ScavConfig.save();                    // 即时写回配置文件
            b.setMessage(label(name, state[0]));
        }).bounds(left, y, 200, 20).build();
        this.addRenderableWidget(button);
        return y + 22;
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + "：" + (on ? "开" : "关"));
    }

    @Override
    public void onClose() {
        ScavConfig.save();
        Compat.setScreen(Minecraft.getInstance(), this.parent);
    }
}
