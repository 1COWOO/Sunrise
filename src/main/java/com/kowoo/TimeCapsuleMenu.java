package com.kowoo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class TimeCapsuleMenu extends DispenserMenu {

    private final SimpleContainer container;
    private final UUID owner;
    private final boolean readOnly; // 읽기 전용 여부 저장

    // 자기 자신용 (수정 가능)
    public TimeCapsuleMenu(int id, ServerPlayer player, SimpleContainer container) {
        this(id, player, container, player.getUUID(), false);
    }

    // 다른 플레이어용 또는 직접 지정용
    public TimeCapsuleMenu(int id, ServerPlayer viewer, SimpleContainer container, UUID owner, boolean readOnly) {
        super(id, viewer.getInventory(), container);
        this.container = container;
        this.owner = owner;
        this.readOnly = readOnly;

        // 아이템 변경 리스너 (읽기 전용이 아닐 때만 저장)
        if (!readOnly) {
            this.container.addListener(inv -> {
                if (inv instanceof SimpleContainer sc) {
                    TimeCapsuleStorage.save(this.owner, sc);
                }
            });
        }
    }

    // 핵심: 아이템을 클릭하거나 옮기려고 할 때 호출됨
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (this.readOnly) return ItemStack.EMPTY; // 읽기 전용이면 퀵 무브 차단
        return super.quickMoveStack(player, index);
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (this.readOnly && slotId >= 0 && slotId < 9) return; // 타임캡슐 슬롯(0~8) 클릭 차단
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !readOnly && super.canTakeItemForPickAll(stack, slot); // 더블클릭으로 가져가기 차단
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 읽기 전용이 아닐 때만 최종 저장
        if (!readOnly && player instanceof ServerPlayer) {
            TimeCapsuleStorage.save(owner, container);
        }
    }
}