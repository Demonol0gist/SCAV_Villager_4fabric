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
 * 成就提示音（26.2：AdvancementToast 仍然自己实现 getSoundEvent()）。
 * 挑战成就 → ui.achievement_challenge，其余（原版没声音）→ ui.achievement_common。
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
