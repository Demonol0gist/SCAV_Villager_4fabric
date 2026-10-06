package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端放音前拦一道：村民 / 掠夺者的语音改成绑定实体播放（跟随移动）。
 *
 * 1.21.6 起 SoundManager.play 有返回值（SoundEngine.PlayResult），取消时要返回 NOT_STARTED；
 * playDelayed 仍是 void。（26.2 那份单独在 ScavVillager-Fabric-26 里，因为那边 getLocation() 改名成了 getIdentifier()）
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (ToastSounds.shouldMute(instance.getLocation())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void scavVillager(SoundInstance instance, int delay, CallbackInfo ci) {
        if (VoiceBinder.tryRebind(instance, delay)) {
            ci.cancel();
        }
    }
}
