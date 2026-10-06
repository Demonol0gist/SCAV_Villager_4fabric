package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 兜底拦截（26.2：getIdentifier + PlayResult）。 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ToastSounds.shouldMute(instance.getIdentifier())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
