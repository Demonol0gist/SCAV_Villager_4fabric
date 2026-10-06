package com.arkit.scavvillager;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.Commands;

/**
 * 把模组的客户端指令挂进**客户端自己的指令树**。
 *
 * 客户端聊天框里"哪些指令有效"的清单来自服务端（登录时发的指令树），
 * 而我们的 /scav 和 /228 是纯客户端拦截的 —— 不挂上去的话，聊天框里会显示成红色"不存在的指令"，
 * 也没有 Tab 补全。挂上去只是让客户端认得它们（变白、可补全），
 * 真正的处理还在 ClientPacketListenerMixin 里截住，服务端收不到。
 *
 * 注：这里只用到 brigadier 的通用 API（CommandDispatcher.getRoot / CommandNode.addChild）
 * 和 Commands.literal —— 1.20.1 到 26.2 都是同一套签名，所以不用分版本。
 */
public final class ScavClientCommands {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(RootCommandNode root) {
        // /scav  → 打开配置界面；/scav config 同义
        root.addChild(Commands.literal("scav")
                .executes(ctx -> 1)
                .then(Commands.literal("config").executes(ctx -> 1))
                .build());
        // /228 → 直接弹一次彩蛋；/228 test 同义
        root.addChild(Commands.literal("228")
                .executes(ctx -> 1)
                .then(Commands.literal("test").executes(ctx -> 1))
                .build());
    }

    private ScavClientCommands() {
    }
}
