package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavVillagerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 每 tick 驱动一次模组逻辑（相当于原 Forge 模组的 ClientTickEvent）。 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void scavVillager$clientTick(CallbackInfo ci) {
        ScavVillagerClient.onClientTick((Minecraft) (Object) this);
    }
}
