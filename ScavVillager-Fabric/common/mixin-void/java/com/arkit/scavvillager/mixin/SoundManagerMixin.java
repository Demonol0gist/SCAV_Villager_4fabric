package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ToastSounds;
import com.arkit.scavvillager.VoiceBinder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端放音前拦一道：村民 / 掠夺者的语音改成绑定实体播放（跟随移动）。
 * 对应原 Forge 模组监听的 PlaySoundEvent。
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavVillager$bindVoice(SoundInstance instance, CallbackInfo ci) {
        // 模组提示音播放前后的静音窗口：压掉原版通用提示音 ui.toast.in
        if (instance != null && ToastSounds.shouldMute(instance.getLocation())) {
            ci.cancel();
            return;
        }
        if (VoiceBinder.tryRebind(instance, 0)) {
            ci.cancel();
        }
    }

    /** 原版对远处音源走 playDelayed，也要拦，延迟照旧保留。 */
    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void scavVillager$bindVoiceDelayed(SoundInstance instance, int delay, CallbackInfo ci) {
        if (VoiceBinder.tryRebind(instance, delay)) {
            ci.cancel();
        }
    }
}
