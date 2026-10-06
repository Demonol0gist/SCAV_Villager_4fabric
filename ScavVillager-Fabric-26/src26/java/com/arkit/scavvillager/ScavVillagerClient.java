package com.arkit.scavvillager;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口。
 *
 * 纯客户端实现：不注册任何服务端内容，服务器（包括原版服务器）不需要装这个模组，
 * 只有装了模组的玩家听得到这套语音。素材与原 Forge 模组一致（assets/scav_villager）。
 */
public class ScavVillagerClient implements ClientModInitializer {
    public static final String MODID = "scav_villager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final int MAX_ERROR_LOGS = 5;
    private static int errorCount;

    @Override
    public void onInitializeClient() {
        ScavConfig.load();
        LOGGER.info("[老乡村民] Fabric 客户端版已加载：语音跟随 / 警觉喊话 / 战斗喊话 / TNT 恐慌 / 目击死亡 / UI 音效");
    }

    /** 由 MinecraftMixin 每 tick 调用。 */
    public static void onClientTick(Minecraft mc) {
        try {
            tickInternal(mc);
        } catch (Throwable t) {
            // 音效模组绝不能把游戏搞崩：出错就记日志并停用当 tick 的逻辑。
            if (errorCount < MAX_ERROR_LOGS) {
                errorCount++;
                LOGGER.warn("[老乡村民] 客户端逻辑出错（已忽略，不影响游戏）"
                        + (errorCount == MAX_ERROR_LOGS ? "（后续同类错误不再打印）" : ""), t);
            }
        }
    }

    private static void tickInternal(Minecraft mc) {
        Scheduler.tick();
        VoiceGovernor.tick();          // 当前语音播完后，放出排队等待的那条
        if (mc.level == null || mc.player == null) {
            VoiceGovernor.clear();
            SituationScanner.reset();
            UiSounds.reset();
            return;
        }
        UiSounds.tick(mc);
        SituationScanner.tick(mc);
    }
}
