package com.kowoo;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class TimeCapsuleStorage {

    private static final int SIZE = 9;

    private static Path getPath(UUID uuid) {
        Path dir = Path.of("config", "sunrise", "timecapsules");
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dir.resolve(uuid + ".dat");
    }

    public static SimpleContainer load(UUID uuid) {
        SimpleContainer inv = new SimpleContainer(SIZE);
        Path path = getPath(uuid);

        if (!Files.exists(path)) return inv;

        try {
            CompoundTag root = NbtIo.read(path);

            for (int i = 0; i < SIZE; i++) {
                if (root.contains("Slot" + i)) {
                    int slot = i;
                    DataResult<ItemStack> result = ItemStack.CODEC.parse(NbtOps.INSTANCE, root.get("Slot" + i));
                    result.result().ifPresent(stack -> inv.setItem(slot, stack));

                    result.error().ifPresent(err -> System.err.println("NBT 로드 오류: " + err));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                        .result()
                        .ifPresent(tag -> root.put("Slot" + slot, tag));
            }
        }

        try {
            NbtIo.write(root, getPath(uuid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
