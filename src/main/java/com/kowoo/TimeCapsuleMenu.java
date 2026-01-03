package com.kowoo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class TimeCapsuleMenu extends DispenserMenu {

    private final SimpleContainer container;
    private final UUID owner;
    private final boolean readOnly;

    public TimeCapsuleMenu(int id, ServerPlayer player, SimpleContainer container) {
        this(id, player, container, player.getUUID(), false);
    }

    public TimeCapsuleMenu(int id, ServerPlayer viewer, SimpleContainer container, UUID owner, boolean readOnly) {
        super(id, viewer.getInventory(), container);
        this.container = container;
        this.owner = owner;
        this.readOnly = readOnly;

        // 리스너를 통한 실시간 저장
        if (!this.readOnly) {
            this.container.addListener(inv -> {
                TimeCapsuleStorage.save(this.owner, (SimpleContainer) inv);
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // 컨테이너가 유효한지 확인 (기본적으로 true 반환하도록 설정 가능)
        return true; 
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 읽기 전용이면 아이템 이동(Shift+클릭) 전면 차단
        if (this.readOnly) return ItemStack.EMPTY; 
        return super.quickMoveStack(player, index);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // 1. 읽기 전용일 때 타임캡슐 슬롯(0~8) 조작 차단
        if (this.readOnly && slotId >= 0 && slotId < 9) return;
        
        // 2. 읽기 전용일 때 자신의 인벤토리에서 타임캡슐로 넣는 행위(QUICK_MOVE 등) 차단
        if (this.readOnly && clickType == ClickType.QUICK_MOVE) return;

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 창이 닫힐 때 최종적으로 한 번 더 저장 (데이터 유실 방지)
        if (!this.readOnly) {
            TimeCapsuleStorage.save(this.owner, this.container);
        }
    }
}
