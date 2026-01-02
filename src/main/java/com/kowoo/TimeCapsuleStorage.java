package com.kowoo;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class TimeCapsuleStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("sunrise-storage");
    private static final int SIZE = 9;

    // SunriseMod와 위치를 맞추기 위해 "./config/realtime.json"과 같은 루트 폴더 기준 설정
    private static Path getPath(UUID uuid) {
        Path dir = Path.of("./config/sunrise/timecapsules");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
        } catch (Exception e) {
            LOGGER.error("디렉토리 생성 실패: ", e);
        }
        return dir.resolve(uuid + ".dat");
    }

    public static SimpleContainer load(UUID uuid) {
        SimpleContainer inv = new SimpleContainer(SIZE);
        Path path = getPath(uuid);

        if (!Files.exists(path)) return inv;

        try {
            // NbtIo.read는 파일이 비어있거나 깨졌을 때 에러를 낼 수 있음
            CompoundTag root = NbtIo.read(path);
            if (root == null) return inv;

            for (int i = 0; i < SIZE; i++) {
                String key = "Slot" + i;
                if (root.contains(key)) {
                    int slot = i;
                    DataResult<ItemStack> result = ItemStack.CODEC.parse(NbtOps.INSTANCE, root.get(key));
                    result.resultOrPartial(err -> LOGGER.error("아이템 로드 중 오류: {}", err))
                          .ifPresent(stack -> inv.setItem(slot, stack));
                }
            }
        } catch (Exception e) {
            LOGGER.error("타임캡슐 로드 중 예외 발생: ", e);
        }

        return inv;
    }

    public static void save(UUID uuid, SimpleContainer inv) {
        CompoundTag root = new CompoundTag();

        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                int slot = i;
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                        .resultOrPartial(err -> LOGGER.error("아이템 저장 중 오류: {}", err))
                        .ifPresent(tag -> root.put("Slot" + slot, tag));
            }
        }

        try {
            // NbtIo.write는 비압축 방식입니다. 용량을 줄이려면 writeCompressed를 사용해도 좋습니다.
            NbtIo.write(root, getPath(uuid));
        } catch (Exception e) {
            LOGGER.error("타임캡슐 저장 중 예외 발생: ", e);
        }
    }
}

