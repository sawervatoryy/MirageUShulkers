package ru.miragestudio.sawervatoryy.loot;

import org.bukkit.inventory.ItemStack;

/**
 * MirageUShulkers — запись лута: предмет + шанс выпадения (%) + диапазон количества.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class LootEntry {

    private final ItemStack item;
    private double chance;
    private int minAmount;
    private int maxAmount;

    public LootEntry(ItemStack item, double chance, int minAmount, int maxAmount) {
        this.item = item.clone();
        this.item.setAmount(1);
        this.chance = chance;
        int cap = Math.max(1, this.item.getMaxStackSize());
        this.minAmount = Math.max(1, Math.min(minAmount, cap));
        this.maxAmount = Math.max(this.minAmount, Math.min(maxAmount, cap));
    }

    public ItemStack item() {
        return item;
    }

    public double chance() {
        return chance;
    }

    public void setChance(double chance) {
        double clamped = Math.max(0.1, Math.min(100.0, chance));
        this.chance = Math.round(clamped * 100.0) / 100.0;
    }

    public int minAmount() {
        return minAmount;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public int amountCap() {
        return Math.max(1, item.getMaxStackSize());
    }

    public void setMinAmount(int value) {
        this.minAmount = Math.max(1, Math.min(value, amountCap()));
        if (this.maxAmount < this.minAmount) {
            this.maxAmount = this.minAmount;
        }
    }

    public void setMaxAmount(int value) {
        this.maxAmount = Math.max(1, Math.min(value, amountCap()));
        if (this.minAmount > this.maxAmount) {
            this.minAmount = this.maxAmount;
        }
    }
}
