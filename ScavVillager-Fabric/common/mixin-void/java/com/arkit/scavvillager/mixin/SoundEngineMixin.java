package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 兜底拦截：万一上层 SoundManager 没接住（各版本签名差异），声音到引擎这一层也能被接管。
 * ≤1.21.5 的 play 是 void。
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager$bindVoice(SoundInstance instance, CallbackInfo ci) {
        if (ToastSounds.shouldMute(instance.getLocation())) {
            ci.cancel();
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            ci.cancel();
        }
    }
}
