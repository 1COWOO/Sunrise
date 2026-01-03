package com.kowoo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SunriseMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("sunrise-mod");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private int tickCounter = 0;
    private Config config;

    // 설정을 위한 내부 클래스
    public static class Config {
        public int utcOffset = -9; // 기본값 -9 (로직상 UTC+9로 변환됨)
    }

    @Override
    public void onInitialize() {
        // 1. 설정 로드
        loadConfig();

        // 2. 서버 시작 시 게임룰 자동 설정
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var commandSource = server.createCommandSourceStack().withSuppressedOutput();
            var dispatcher = server.getCommands().getDispatcher();
            try {
                dispatcher.execute("gamerule advance_time false", commandSource);
                dispatcher.execute("gamerule players_sleeping_percentage 101", commandSource);
                LOGGER.info("RealTime: 게임룰 설정");
            } catch (Exception e) {
                LOGGER.error("RealTime: 게임룰 설정 실패: " + e.getMessage());
            }
        });

        // 3. 시간 동기화 및 액션바 출력 (1초마다)
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() != ServerLevel.OVERWORLD) return;

            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            // 설정값 -9를 +9로 반전시켜 적용
            int actualOffset = config.utcOffset * -1;
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.ofHours(actualOffset));
            
            // 마인크래프트 시간 적용
            long mcTime = ((now.getHour() + 18) % 24) * 1000L + (now.getMinute() * 1000L / 60L);
            level.setDayTime(mcTime);

            // 일출/일몰 남은 시간 계산
            String type;
            OffsetDateTime targetTime;
            if (now.getHour() >= 6 && now.getHour() < 18) {
                type = "일몰";
                targetTime = now.withHour(18).withMinute(0).withSecond(0).withNano(0);
            } else {
                type = "일출";
                targetTime = now.withHour(6).withMinute(0).withSecond(0).withNano(0);
                if (now.getHour() >= 18) targetTime = targetTime.plusDays(1);
            }

            long diffSec = Duration.between(now, targetTime).getSeconds();
            String remain = String.format("%02d:%02d:%02d", diffSec / 3600, (diffSec % 3600) / 60, diffSec % 60);

            // 제공해주신 포맷 적용
            Component message = Component.literal("현재 시각: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(now.format(TIME_FORMATTER)).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" | ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(type + "까지 남은 시간: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(remain).withStyle(ChatFormatting.GRAY));

            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.displayClientMessage(message, true);
            }
        });

        // 4. 명령어 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            TimeCapsuleCommand.register(dispatcher); 
        });
    }

    private void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("sunrise");
        File configDir = configPath.toFile();
        File configFile = new File(configDir, "realtime.json");

        if (!configDir.exists()) configDir.mkdirs();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, Config.class);
                if (config == null) config = new Config();
            } catch (Exception e) {
                config = new Config();
            }
        } else {
            config = new Config();
            saveConfig(configFile);
        }
    }

    private void saveConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            LOGGER.error("설정 저장 실패: " + e.getMessage());
        }
    }
}

