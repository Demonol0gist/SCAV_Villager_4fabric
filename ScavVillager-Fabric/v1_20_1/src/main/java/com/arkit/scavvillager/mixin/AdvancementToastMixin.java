package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.ToastSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
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

/**
 * 成就提示音（1.20.1 版）。
 * 原版只会给挑战成就播号角，这里在 render 开头抢先处理：
 *   - 置位 playedSound，让原版那句号角不再播；
 *   - 挑战成就播 ui.achievement_challenge，普通成就补播 ui.achievement_common。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Shadow
    @Final
    private Advancement advancement;

    @Shadow
    private boolean playedSound;

    @Inject(method = "render", at = @At("HEAD"))
    private void scavVillager$achievementSound(GuiGraphics graphics, ToastComponent toastComponent,
                                              long timeSinceLastVisible, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (this.playedSound || !ScavConfig.get().achievementSounds) {
            return;
        }
        DisplayInfo display = this.advancement.getDisplay();
        boolean challenge = display != null && display.getFrame() == FrameType.CHALLENGE;
        this.playedSound = true;
        ToastSounds.playAchievement(challenge);
    }
}
