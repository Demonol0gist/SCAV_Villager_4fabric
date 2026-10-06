package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.ScavClientCommands;
import com.arkit.scavvillager.ScavVillagerClient;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务端发来指令树之后，把模组自己的客户端指令补挂上去
 * —— 这样聊天框里它们是"有效指令"（不标红、有 Tab 补全）。
 */
@Mixin(ClientPacketListener.class)
public class ClientCommandTreeMixin {

    /** 客户端自己的指令表（各版本泛型不同，这里按擦除后的原类型声明）。 */
    @Shadow
    private CommandDispatcher commands;

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void scavVillager$addClientCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (this.commands == null) {
            return;
        }
        try {
            ScavClientCommands.register(this.commands.getRoot());
        } catch (Throwable t) {
            ScavVillagerClient.LOGGER.warn("[老乡村民] 客户端指令注册失败（聊天框里会显示为红色，但功能仍在）: {}", t.toString());
        }
    }
}
