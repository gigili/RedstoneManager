package dev.igorilic.redstonemanager.util;

import dev.igorilic.redstonemanager.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LinkerGroup {
    private final String groupName;
    private boolean isPowered = false;
    private List<ItemStack> items = new ArrayList<>();

    public LinkerGroup(String groupName) {
        this.groupName = groupName;
    }

    public LinkerGroup(String groupName, boolean isPowered) {
        this.groupName = groupName;
        this.isPowered = isPowered;
    }

    public LinkerGroup(String groupName, boolean isPowered, List<ItemStack> stacks) {
        this.groupName = groupName;
        this.isPowered = isPowered;
        this.items = stacks;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean powered) {
        this.isPowered = powered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinkerGroup that)) return false;
        return groupName.equals(that.groupName) && isPowered == that.isPowered && items.size() == that.items.size();
    }

    @Override
    public int hashCode() {
        return groupName.hashCode();
    }

    @Override
    public String toString() {
        return groupName;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public void addItem(ItemStack stack) {
        items.add(stack);
    }

    public void removeItem(ItemStack stack) {
        int index = findLinkerIndex(stack);
        if (index != -1) {
            items.remove(index);
        }
    }

    public int findLinkerIndex(ItemStack target) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack linker = items.get(i);
            if (linker.has(ModDataComponents.ITEM_UUID) && target.has(ModDataComponents.ITEM_UUID)) {
                if (Objects.equals(linker.get(ModDataComponents.ITEM_UUID), target.get(ModDataComponents.ITEM_UUID))) {
                    return i;
                }
            } else if (ItemStack.isSameItemSameComponents(items.get(i), target)) {
                return i;
            }
        }
        return -1;
    }
}
