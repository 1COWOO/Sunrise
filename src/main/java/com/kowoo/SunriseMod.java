package com.kowoo;

import com.kowoo.SunSet;
import com.kowoo.TimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

        // 1. 시간 동기화 로직 (Noonmaru 방식)
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

            // 1초마다 액션바 메시지 출력
            messageTickCounter++;
            if (messageTickCounter >= 20) {
                messageTickCounter = 0;
                broadcastStatus(server);
            }
        });

        // 2. 타임캡슐 커맨드 등록 (이 부분이 살아있어야 합니다)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TimeCapsuleCommand.register(dispatcher);
            LOGGER.info("TimeCapsule 커맨드 등록 완료.");
        });
    }

            private void broadcastStatus(net.minecraft.server.MinecraftServer server) {
        // 클래스명은 TimeAdapter (대문자), 변수명은 timeAdapter (소문자)
        if (timeAdapter == null) return;

        LocalTime now = LocalTime.now();
        
        // Noonmaru 방식: 어댑터의 '종료 시각(to)'에서 '현재 시각'을 뺍니다.
        long targetMillis = timeAdapter.getTo().getTime();
        long nowMillis = System.currentTimeMillis();
        long diffSec = (targetMillis - nowMillis) / 1000;
        
        if (diffSec < 0) diffSec = 0;

        // 남은 시간을 00:00:00 형식으로 변환
        String remain = String.format("%02d:%02d:%02d", diffSec / 3600, (diffSec % 3600) / 60, diffSec % 60);
        
        // 현재 상태 판별 (Noonmaru 틱 기준)
        // 낮 시간(22835~13150틱 사이)이면 다음 목표는 '일몰'
        long currentTick = timeAdapter.getCurrentTick();
        String type = (currentTick >= 22835 || currentTick < 13150) ? "일몰" : "일출";

        // 메시지 조립 (구분선만 노란색, 나머지는 회색)
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
            latitude = 37.5665; longitude = -126.9780; timezone = -9;
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

