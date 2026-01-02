package com.kowoo;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

import java.util.UUID;

public class TimeCapsuleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("timecapsule")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    // 본인 정보는 Player 객체에서 바로 가져옵니다.
                    open(player, player.getUUID(), player.getGameProfile().name());
                    return 1;
                })
                .then(
                    Commands.literal("open")
                        .then(
                            Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(ctx -> {
                                    ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                                    
                                    // Collection<?>로 받아서 타입 충돌을 방지합니다.
                                    var profiles = GameProfileArgument.getGameProfiles(ctx, "targets");
                                    
                                    if (profiles.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("플레이어를 찾을 수 없습니다."));
                                        return 0;
                                    }

                                    // GameProfile로 캐스팅하지 않고, 인터페이스의 메서드를 직접 호출합니다.
                                    // NameAndId 인터페이스는 보통 id()와 name() 메서드를 가집니다.
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

