package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.TradeWatcher;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记下"玩家点了交易界面的产出槽" —— 那是执行成交的动作。
 * 交易界面的产出槽是索引 2（0/1 是玩家放物品的两个槽），点它 = 这笔交易成立。
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void scavVillager$noteTradeClick(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (slot != null && slot.index == 2 && (Object) this instanceof MerchantScreen) {
            TradeWatcher.notePurchaseClick();
        }
    }
}
