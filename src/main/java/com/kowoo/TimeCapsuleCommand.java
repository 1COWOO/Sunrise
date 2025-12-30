package com.kowoo;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class TimeCapsuleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("timecapsule")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    // 본인 타임캡슐 열기
                    open(player, player.getUUID(), player.getGameProfile().name());
                    return 1;
                })
                .then(
                    Commands.literal("open")
                        .then(
                            Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    ServerPlayer viewer = ctx.getSource().getPlayerOrException();

                                    var profiles = GameProfileArgument.getGameProfiles(ctx, "targets");
                                    if (profiles.isEmpty()) {
                                        viewer.sendSystemMessage(Component.literal("플레이어를 찾을 수 없습니다."));
                                        return 0;
                                    }

                                    var targetProfile = profiles.iterator().next();
                                    UUID targetUUID = targetProfile.id();       // AuthLib GameProfile에서 id() 사용
                                    String targetName = targetProfile.name();  // 이름 가져오기

                                    open(viewer, targetUUID, targetName);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void open(ServerPlayer viewer, UUID targetUUID, String targetName) {
        var inv = TimeCapsuleStorage.load(targetUUID);
        boolean readOnly = !viewer.getUUID().equals(targetUUID);

        viewer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (id, playerInv, player) -> new TimeCapsuleMenu(id, viewer, inv, targetUUID, readOnly),
            Component.literal(readOnly ? "타임 캡슐(보기 전용) - " + targetName : "타임 캡슐 - " + targetName)
        ));
    }
}
