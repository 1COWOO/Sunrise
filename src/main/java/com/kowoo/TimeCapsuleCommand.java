package com.kowoo;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import java.util.UUID;

public class TimeCapsuleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("timecapsule")
            .requires(source -> source.getEntity() instanceof ServerPlayer) 
            // 기본 명령어: 자기 자신 열기 (권한 없음)
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                open(player, player.getUUID(), player.getScoreboardName());
                return 1;
            })

            // open 서브커맨드
            .then(
                Commands.literal("open")
                    // 🔒 OP 레벨 2 이상만 허용
                    .requires(source ->
        source.getEntity() instanceof ServerPlayer
        && source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
    )

                    .then(
                        Commands.argument("targets", GameProfileArgument.gameProfile())
                            .executes(ctx -> {
                                ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                                var profiles = GameProfileArgument.getGameProfiles(ctx, "targets");

                                if (profiles.isEmpty()) {
                                    ctx.getSource().sendFailure(
                                        Component.literal("플레이어를 찾을 수 없습니다.")
                                    );
                                    return 0;
                                }

                                var target = profiles.iterator().next();
                                open(viewer, target.id(), target.name());
                                return 1;
                            })
                    )
            )
    );
}

    private static void open(ServerPlayer viewer, UUID targetUUID, String targetName) {
        var inv = TimeCapsuleStorage.load(targetUUID);
        boolean readOnly = !viewer.getUUID().equals(targetUUID);

        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInv, player) -> new TimeCapsuleMenu(id, (ServerPlayer) player, inv, targetUUID, readOnly),
            Component.literal(readOnly ? "타임 캡슐(보기 전용) - " + targetName : "타임 캡슐 - " + targetName)
        ));
    }
}

