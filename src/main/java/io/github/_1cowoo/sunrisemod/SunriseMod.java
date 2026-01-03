package io.github._1cowoo.sunrisemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class SunriseMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("sunrise");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private double latitude;
    private double longitude;
    private int timezone;
    private TimeAdapter timeAdapter;
    private long lastTick;
    private long lastModified;
    private File configFile;
    private int messageTickCounter = 0;

    @Override
    public void onInitialize() {
        this.configFile = new File("./config/sunrise/realtime.json");
        loadConfig();
        updateTimeAdapter();

        // [방법 변경] 클래스 직접 참조 에러를 피하기 위해 명령어로 규칙 설정
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // 서버의 명령어 관리자를 사용하여 직접 명령어를 실행시킵니다 (가장 안전한 방법)
            var commandSource = server.createCommandSourceStack().withSuppressedOutput();
            var dispatcher = server.getCommands().getDispatcher();
            
            try {
                // 1. 시간 흐름 정지
                dispatcher.execute("gamerule advance_time false", commandSource);
                // 2. 취침 인원 101% 설정
                dispatcher.execute("gamerule players_sleeping_percentage 101", commandSource);
                
                LOGGER.info("게임룰 설정을 완료했습니다.");
            } catch (Exception e) {
                LOGGER.error("게임룰 설정 중 오류 발생: " + e.getMessage());
            }
        });

        // 1. 시간 동기화 로직
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (configFile.exists() && configFile.lastModified() != lastModified) {
                loadConfig();
            }

            if (timeAdapter == null || !timeAdapter.isValid()) {
                updateTimeAdapter();
            }

            long tick = timeAdapter.getCurrentTick();
            if (lastTick != tick) {
                this.lastTick = tick;
                for (ServerLevel level : server.getAllLevels()) {
                    level.setDayTime(tick);
                }
            }

            messageTickCounter++;
            if (messageTickCounter >= 20) {
                messageTickCounter = 0;
                broadcastStatus(server);
            }
        });

        // 2. 커맨드 등록
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TimeCapsuleCommand.register(dispatcher);
        });
    }

    // broadcastStatus 등 나머지 메서드는 이전과 동일하므로 생략 (그대로 두시면 됩니다)
    private void broadcastStatus(net.minecraft.server.MinecraftServer server) {
        if (timeAdapter == null) return;
        LocalTime now = LocalTime.now();
        long targetMillis = timeAdapter.getTo().getTime();
        long nowMillis = System.currentTimeMillis();
        long diffSec = (targetMillis - nowMillis) / 1000;
        if (diffSec < 0) diffSec = 0;
        String remain = String.format("%02d:%02d:%02d", diffSec / 3600, (diffSec % 3600) / 60, diffSec % 60);
        long currentTick = timeAdapter.getCurrentTick();
        String type = (currentTick >= 22835 || currentTick < 13150) ? "일몰" : "일출";
        Component message = Component.literal("현재 시각: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(now.format(TIME_FORMATTER)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" | ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(type + "까지 남은 시간: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(remain).withStyle(ChatFormatting.GRAY));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, true);
        }
    }

    private void updateTimeAdapter() {
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis();
        Date sunrise = SunSet.getSunriseTime(calendar, latitude, longitude, timezone);
        if (time < sunrise.getTime()) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date sunset = SunSet.getSunsetTime(calendar, latitude, longitude, timezone);
            this.timeAdapter = new TimeAdapter(sunset, sunrise, TimeAdapter.Type.NIGHT);
            return;
        }
        Date sunset = SunSet.getSunsetTime(calendar, latitude, longitude, timezone);
        if (time < sunset.getTime()) {
            this.timeAdapter = new TimeAdapter(sunrise, sunset, TimeAdapter.Type.DAY);
            return;
        }
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        sunrise = SunSet.getSunriseTime(calendar, latitude, longitude, timezone);
        this.timeAdapter = new TimeAdapter(sunset, sunrise, TimeAdapter.Type.NIGHT);
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            latitude = 37.5665; longitude = 126.9780; timezone = 9;
            saveConfig();
        } else {
            try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                latitude = json.get("latitude").getAsDouble();
                longitude = json.get("longitude").getAsDouble();
                timezone = json.get("timezone").getAsInt();
                lastModified = configFile.lastModified();
            } catch (Exception e) { LOGGER.error("Config Load Error", e); }
        }
    }

    private void saveConfig() {
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            JsonObject json = new JsonObject();
            json.addProperty("latitude", latitude);
            json.addProperty("longitude", longitude);
            json.addProperty("timezone", timezone);
            GSON.toJson(json, writer);
            lastModified = configFile.lastModified();
        } catch (IOException e) { LOGGER.error("Config Save Error", e); }
    }
}

