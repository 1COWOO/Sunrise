package com.kowoo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalTime;

public class SunriseMod implements ModInitializer {

    // 20틱(1초)을 체크하기 위한 카운터 변수
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        // 1. 월드 틱 이벤트 등록
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            // 서버 객체가 없거나 클라이언트 사이드 월드라면 종료
            if (level.getServer() == null) return;

            // 틱 카운터 증가 및 20틱 체크 (약 1초)
            tickCounter++;
            if (tickCounter < 20) {
                return;
            }
            tickCounter = 0; // 20틱이 되면 카운터 초기화

            // --- 여기서부터 1초에 한 번만 실행됩니다 ---
            LocalTime now = LocalTime.now();
            long mcTime = ((now.getHour() + 18) % 24) * 1000 + (now.getMinute() * 1000 / 60);
            level.setDayTime(mcTime);

            long currentTicks = mcTime % 24000;
            long targetTicks = 23000;
            long ticksUntilSunrise = (currentTicks < targetTicks)
                    ? (targetTicks - currentTicks)
                    : (24000 - currentTicks + targetTicks);

            int totalMinutesLeft = (int) (ticksUntilSunrise * 60 / 1000);
            int hours = totalMinutesLeft / 60;
            int minutes = totalMinutesLeft % 60;

            MutableComponent message = buildSunriseMessage(now.getHour(), now.getMinute(), hours, minutes);

            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                // 액션바(overlay)에 메시지 출력
                player.sendSystemMessage(message, true);
            }
        });

        // 2. 명령어 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            TimeCapsuleCommand.register(dispatcher); 
        });
    }

    private MutableComponent buildSunriseMessage(int hour, int minute, int hoursLeft, int minutesLeft) {
        MutableComponent message = Component.literal(String.format("현재: %02d:%02d", hour, minute))
                .withStyle(ChatFormatting.GRAY);

        message.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));

        if (hoursLeft == 0 && minutesLeft == 0) {
            message.append(Component.literal("☀️ 해가 뜨고 있습니다!").withStyle(ChatFormatting.GOLD));
        } else {
            message.append(Component.literal(
                    String.format("일출까지: %d시간 %d분", hoursLeft, minutesLeft))
                    .withStyle(ChatFormatting.GRAY));
        }

        return message;
    }
}