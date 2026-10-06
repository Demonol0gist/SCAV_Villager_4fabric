package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 配方解锁提示音：原版只播通用的 ui.toast.in，这里换成模组的 ui.recipe
 * （并开静音窗口压掉通用的那条，对应原模组的 ToastAddEvent 处理）。
 */
@Mixin(ToastComponent.class)
public class ToastComponentMixin {

    @Inject(method = "addToast", at = @At("HEAD"))
    private void scavVillager$recipeSound(Toast toast, CallbackInfo ci) {
        if (toast instanceof RecipeToast) {
            ToastSounds.playRecipe();
        }
    }
}
