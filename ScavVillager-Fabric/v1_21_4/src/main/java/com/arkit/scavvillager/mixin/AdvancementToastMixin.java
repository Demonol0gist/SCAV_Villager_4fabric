package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.ToastSounds;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 成就提示音（1.21.2 / 1.21.4 形态：AdvancementToast 里有 playedSound 标志位）。
 * 在 update 开头抢先置位 playedSound，让原版那句号角不再播，改播模组音效：
 * 挑战成就 → ui.achievement_challenge，普通成就 → ui.achievement_common。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Shadow
    @Final
    private AdvancementHolder advancement;

    @Shadow
    private boolean playedSound;

    @Inject(method = "update", at = @At("HEAD"))
    private void scavVillager$achievementSound(ToastManager manager, long timeSinceLastVisible, CallbackInfo ci) {
        if (this.playedSound || !ScavConfig.get().achievementSounds) {
            return;
        }
        Optional<DisplayInfo> display = this.advancement.value().display();
        boolean challenge = display.map(info -> info.getType() == AdvancementType.CHALLENGE).orElse(false);
        this.playedSound = true;
        ToastSounds.playAchievement(challenge);
    }
}
