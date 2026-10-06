package com.arkit.scavvillager;

import net.minecraft.util.Util;

/**
 * 辨别 entity.villager.yes 的两个时机。
 *
 * 原版这个事件在两个时刻都会播：
 *   1) 在交易界面**选中某件可交易的商品**（村民：同意）
 *   2) **交易成功**（村民：同意）
 * 只靠事件分不出来，所以这里用玩家点了产出槽作为成交信号：
 * 产出槽（索引 2）里有东西 = 这笔交易已经成立，点它就是在执行交易。
 */
public final class TradeWatcher {
    private static final long PURCHASE_WINDOW_MILLIS = 800L;
    private static long purchaseUntil;

    /** 玩家点了交易界面的产出槽。 */
    public static void notePurchaseClick() {
        purchaseUntil = Util.getMillis() + PURCHASE_WINDOW_MILLIS;
        ScavVillagerClient.LOGGER.info("[老乡村民][诊断] 点了产出槽 → 成交窗口开启 800ms");
    }

    /** 这次 yes 是不是成交（是则消耗掉窗口，避免连续两次都算成交）。 */
    public static boolean consumePurchase() {
        if (Util.getMillis() < purchaseUntil) {
            purchaseUntil = 0L;
            return true;
        }
        return false;
    }

    public static void reset() {
        purchaseUntil = 0L;
    }

    private TradeWatcher() {
    }
}
