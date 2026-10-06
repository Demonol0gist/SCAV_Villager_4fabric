package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.ToastSounds;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 成就提示音（1.21.1 版）。
 * 与 1.20.1 版逻辑相同，差别只在 1.21 的 API：AdvancementHolder.value() → Advancement.display()（Optional）
 * → DisplayInfo.getType() == AdvancementType.CHALLENGE。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Shadow
    @Final
    private AdvancementHolder advancement;

    @Shadow
    private boolean playedSound;

    @Inject(method = "render", at = @At("HEAD"))
    private void scavVillager$achievementSound(GuiGraphics graphics, ToastComponent toastComponent,
                                              long timeSinceLastVisible, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (this.playedSound || !ScavConfig.get().achievementSounds) {
            return;
        }
        Optional<DisplayInfo> display = this.advancement.value().display();
        boolean challenge = display.map(info -> info.getType() == AdvancementType.CHALLENGE).orElse(false);
        this.playedSound = true;
        ToastSounds.playAchievement(challenge);
    }
}
