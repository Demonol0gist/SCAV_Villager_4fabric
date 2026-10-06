package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.TradeWatcher;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.2 形态：ClickType 已改名 ContainerInput。 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void scavVillager(Slot slot, int slotId, int mouseButton, ContainerInput type, CallbackInfo ci) {
        if (slot != null && slot.index == 2 && (Object) this instanceof MerchantScreen) {
            TradeWatcher.notePurchaseClick();
        }
    }
}
