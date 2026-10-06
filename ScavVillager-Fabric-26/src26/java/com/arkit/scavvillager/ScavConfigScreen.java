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
    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int STEP = 20;

    /** 名额选项：0 = 不限。 */
    private static final int[] VOICE_LIMITS = {0, 1, 2, 3, 5, 8, 12};

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
        int left = this.width / 2 - BTN_W / 2;
        int y = 24;

        // —— 228 彩蛋 ——
        y = toggle("228 崩溃彩蛋", cfg.error228Enabled, v -> cfg.error228Enabled = v, left, y);
        // —— 常用开关 ——
        y = toggle("模组总开关", cfg.enableMod, v -> cfg.enableMod = v, left, y);
        y = toggle("村民语音", cfg.enableVillager, v -> cfg.enableVillager = v, left, y);
        y = toggle("掠夺者语音", cfg.enablePillager, v -> cfg.enablePillager = v, left, y);
        y = toggle("成就 / 配方提示音", cfg.achievementSounds, v -> cfg.achievementSounds = v, left, y);
        y = toggle("进世界提示音+提示框", cfg.joinSound, v -> cfg.joinSound = v, left, y);
        y = toggle("语音跟随实体", cfg.followEntities, v -> cfg.followEntities = v, left, y);
        // —— 出声名额（只让离你最近的 N 只出声；点一下循环切换）——
        y = cycle("最多出声的村民", VOICE_LIMITS, cfg.maxVillagerVoices, v -> cfg.maxVillagerVoices = v, left, y);
        y = cycle("最多出声的掠夺者", VOICE_LIMITS, cfg.maxPillagerVoices, v -> cfg.maxPillagerVoices = v, left, y);
        y = toggle("详细日志（排错用）", cfg.verboseLog, v -> cfg.verboseLog = v, left, y);

        this.addRenderableWidget(Button.builder(Component.literal("完成"), b -> this.onClose())
                .bounds(left, y + 4, BTN_W, BTN_H).build());
    }

    /** 一个开/关按钮：标题上直接显示状态，点一下即时生效并写盘。 */
    private int toggle(String name, boolean initial, Consumer<Boolean> apply, int left, int y) {
        boolean[] state = {initial};
        Button button = Button.builder(label(name, state[0]), b -> {
            state[0] = !state[0];
            apply.accept(state[0]);
            ScavConfig.save();
            b.setMessage(label(name, state[0]));
        }).bounds(left, y, BTN_W, BTN_H).build();
        this.addRenderableWidget(button);
        return y + STEP;
    }

    /** 一个数值按钮：点一下在候选档位里循环。 */
    private int cycle(String name, int[] values, int current, Consumer<Integer> apply, int left, int y) {
        int start = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                start = i;
            }
        }
        int[] state = {start};
        Button button = Button.builder(cycleLabel(name, values[state[0]]), b -> {
            state[0] = (state[0] + 1) % values.length;
            apply.accept(values[state[0]]);
            ScavConfig.save();
            b.setMessage(cycleLabel(name, values[state[0]]));
        }).bounds(left, y, BTN_W, BTN_H).build();
        this.addRenderableWidget(button);
        return y + STEP;
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + "：" + (on ? "开" : "关"));
    }

    private static Component cycleLabel(String name, int value) {
        return Component.literal(name + "：" + (value <= 0 ? "不限" : value));
    }

    @Override
    public void onClose() {
        ScavConfig.save();
        Compat.setScreen(Minecraft.getInstance(), this.parent);
    }
}
