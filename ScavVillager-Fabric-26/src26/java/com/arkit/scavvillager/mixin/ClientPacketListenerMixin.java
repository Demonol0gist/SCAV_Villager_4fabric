package com.arkit.scavvillager.mixin;

import com.arkit.scavvillager.Compat;
import com.arkit.scavvillager.ScavConfigScreen;
import com.arkit.scavvillager.ScavVillagerClient;
import com.arkit.scavvillager.UiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 客户端命令 /228 test：手动弹一次 228 弹窗（对应原模组的 RegisterClientCommandsEvent）。 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void scavVillager$error228Command(String command, CallbackInfo ci) {
        if ("scav".equals(command) || "scav config".equals(command)) {
            Minecraft.getInstance().execute(() ->
                    Compat.setScreen(Minecraft.getInstance(), new ScavConfigScreen(Compat.currentScreen(Minecraft.getInstance()))));
            ci.cancel();                        // 不发到服务端
            return;
        }
        if ("228 test".equals(command) || "228".equals(command)) {
            ScavVillagerClient.LOGGER.info("[老乡村民] /228 test → 弹出弹窗");
            UiSounds.showError228(Minecraft.getInstance());
            ci.cancel();                        // 不发到服务端
        }
    }
}
