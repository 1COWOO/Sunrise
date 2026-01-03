package com.kowoo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SunriseMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("sunrise-mod");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File configFile;
    private Config config;
    private int tickCounter = 0;

    public static class Config {
        public int timezone = -9; // -9 입력 시 한국 시간(UTC+9) 작동
    }

    @Override
    public void onInitialize() {
        // 요청하신 방식의 경로 설정
        this.configFile = new File("./config/sunrise/realtime.json");
        loadConfig();

        // 1. 서버 시작 시 게임룰 설정
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var source = server.createCommandSourceStack().withSuppressedOutput();
            var dispatcher = server.getCommands().getDispatcher();
            try {
                dispatcher.execute("gamerule advance_time false", source);
                dispatcher.execute("gamerule players_sleeping_percentage 101", source);
                LOGGER.info("게임룰 설정 완료.");
            } catch (Exception e) {
                LOGGER.error("게임룰 설정 중 오류: " + e.getMessage());
            }
        });

        // 2. 시간 업데이트 로직
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != ServerLevel.OVERWORLD) return;

            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            // timezone -9 -> UTC+9 변환 로직
            int actualOffset = config.timezone * -1;
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(actualOffset));

            // 월드 시간 적용
            long mcTime = ((now.getHour() + 18) % 24) * 1000L + (now.getMinute() * 1000L / 60L);
            level.setDayTime(mcTime);

            // 메시지 계산 및 출력
            broadcastStatus(level, now);
        });

        // 3. 명령어 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            TimeCapsuleCommand.register(dispatcher);
        });
    }

    private void broadcastStatus(ServerLevel level, OffsetDateTime now) {
        String type;
        OffsetDateTime target;

        if (now.getHour() >= 6 && now.getHour() < 18) {
            type = "일몰";
            target = now.withHour(18).withMinute(0).withSecond(0).withNano(0);
        } else {
            type = "일출";
            target = now.withHour(6).withMinute(0).withSecond(0).withNano(0);
            if (now.getHour() >= 18) target = target.plusDays(1);
        }

        long diffSec = Duration.between(now, target).getSeconds();
        String remain = String.format("%02d:%02d:%02d", diffSec / 3600, (diffSec % 3600) / 60, diffSec % 60);

        Component message = Component.literal("현재 시각: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(now.format(TIME_FORMATTER)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" | ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(type + "까지 남은 시간: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(remain).withStyle(ChatFormatting.GRAY));

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.displayClientMessage(message, true);
        }
    }

    private void loadConfig() {
        try {
            File dir = configFile.getParentFile();
            if (!dir.exists()) dir.mkdirs();

            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    config = GSON.fromJson(reader, Config.class);
                }
            }
            
            if (config == null) {
                config = new Config();
                saveConfig();
            }
        } catch (Exception e) {
            config = new Config();
            LOGGER.error("설정 로드 실패: " + e.getMessage());
        }
    }

    private void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            LOGGER.error("설정 저장 실패: " + e.getMessage());
        }
    }
}

