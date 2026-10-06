package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 配方解锁提示音（1.21.2 起 ToastComponent 改名为 ToastManager）。
 * 原版只播通用的 ui.toast.in，这里换成模组的 ui.recipe（并开静音窗口压掉通用音）。
 */
@Mixin(ToastManager.class)
public class ToastManagerMixin {

    @Inject(method = "addToast", at = @At("HEAD"))
    private void scavVillager$recipeSound(Toast toast, CallbackInfo ci) {
        if (toast instanceof RecipeToast) {
            ToastSounds.playRecipe();
        }
    }
}
