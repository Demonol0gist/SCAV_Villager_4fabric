package com.arkit.scavvillager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置。键名与原 Forge 模组保持一致（默认值也照抄原版），老配置文件可以直接沿用。
 * 加载时机：启动时 + 进入世界时（改完不用重启游戏）。
 */
public class ScavConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ScavConfig instance;

    // ---------- 通用 ----------
    public boolean enableMod = true;
    public boolean enableVillager = true;
    public boolean enablePillager = true;
    /** Guard Villagers 联动。Fabric 客户端版暂未实现，保留键名以便配置兼容。 */
    public boolean enableGuard = true;
    public boolean verboseLog = false;

    /** 最多让离玩家最近的几只村民出声（0 = 不限；只影响新语音，已在播的不会掐断）。 */
    public int maxVillagerVoices = 0;
    /** 同上，掠夺者。 */
    public int maxPillagerVoices = 0;

    /** 语音是否绑定实体播放（跟随移动）。关掉就保持原版的固定位置播放。 */
    public boolean followEntities = true;
    public double volume = 1.0;                 // 0.0 ~ 2.0，1.0 = 原音量
    public boolean babyPitch = true;            // 小村民变音（约 1.5 倍音高）
    public int ambientIntervalTicks = 20;       // 同一只生物两次待机语音的最小间隔（0 = 原版节奏）

    // ---------- 警觉 ----------
    public double playerDetectRange = 5.0;      // 村民察觉到玩家的距离
    public int playerAlertCooldownSeconds = 60;
    public int hostileAlertCooldownSeconds = 60;
    public double alertSearchRange = 24.0;      // 搜索可警觉目标的距离

    // ---------- 战斗 ----------
    public int fightGapSeconds = 5;             // 战斗喊话间隔（原模组默认 2，这里放缓一些；想还原改成 2）
    public int fightFleeSeconds = 8;            // 战斗状态的保持时间
    public double fightChaseRange = 8.0;        // 判定"正在追击"的距离
    public boolean clearEnabled = true;         // 失去目标时喊"clear"

    // ---------- TNT ----------
    public double tntDetectRange = 8.0;
    public int tntShoutCooldownSeconds = 5;
    public int tntKeepSeconds = 3;              // 看见 TNT 后恐慌状态的保持时间
    /** 躲避 TNT 的 AI 属于服务端行为，纯客户端版无法实现，保留键名。 */
    public boolean tntFleeEnabled = true;

    // ---------- 目击死亡 ----------
    public boolean witnessEnabled = true;
    public double witnessRange = 10.0;
    public int witnessCooldownSeconds = 5;

    public int checkIntervalTicks = 10;         // 主扫描间隔
    public double villagerSearchRange = 32.0;   // 扫描半径（村民）
    public double pillagerSearchRange = 48.0;   // 扫描半径（掠夺者）
    /** 同上，Guard Villagers 联动用。 */
    public double guardSearchRange = 32.0;
    public int spawnQuietTicks = 60;            // 生物刚出现时的静默时间

    // ---------- 客户端 ----------
    public boolean achievementSounds = true;    // 用模组的成就 / 配方提示音替换原版
    public boolean joinSound = true;            // 进入世界时播入场音频 + 弹提示框
    public int playerDeathCooldownSeconds = 5;
    public int duplicateWindowMillis = 250;     // 同一句语音在该时间窗内重复到达时丢弃
    public boolean error228Enabled = true;      // 228 整活弹窗（每 1200 tick 掷 1% 概率）
    public boolean error228Sound = true;

    public static ScavConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ScavVillagerClient.MODID + ".json");
        ScavConfig cfg = null;
        if (Files.isRegularFile(path)) {
            try {
                cfg = GSON.fromJson(Files.readString(path), ScavConfig.class);
            } catch (Exception e) {
                ScavVillagerClient.LOGGER.warn("[老乡村民] 配置文件解析失败，使用默认值: {}", e.toString());
            }
        }
        if (cfg == null) {
            cfg = new ScavConfig();
        }
        instance = cfg;
        save();                                   // 首次运行时生成；已有文件会被补齐缺失的键
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ScavVillagerClient.MODID + ".json");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            ScavVillagerClient.LOGGER.warn("[老乡村民] 配置写入失败: {}", e.toString());
        }
    }

    public void log(String msg) {
        if (this.verboseLog) {
            ScavVillagerClient.LOGGER.info("[老乡村民] {}", msg);
        }
    }
}
