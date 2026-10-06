package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavConfig;
import com.arkit.scavvillager.SoundIds;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 成就提示音（1.21.5+ 形态：AdvancementToast 自带 getSoundEvent()）。
 * 直接替换返回值：原版只在挑战成就时返回号角，这里把挑战换成 ui.achievement_challenge，
 * 其余（原版返回 null、没声音）补上 ui.achievement_common。
 */
@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Inject(method = "getSoundEvent", at = @At("RETURN"), cancellable = true)
    private void scavVillager$achievementSound(CallbackInfoReturnable<SoundEvent> cir) {
        if (!ScavConfig.get().achievementSounds) {
            return;
        }
        SoundEvent vanilla = cir.getReturnValue();
        if (vanilla == SoundEvents.UI_TOAST_CHALLENGE_COMPLETE) {
            cir.setReturnValue(SoundIds.UI_ACHIEVEMENT_CHALLENGE);
        } else {
            cir.setReturnValue(SoundIds.UI_ACHIEVEMENT_COMMON);
        }
    }
}
