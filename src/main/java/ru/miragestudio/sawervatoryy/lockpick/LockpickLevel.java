package ru.miragestudio.sawervatoryy.lockpick;

import org.bukkit.inventory.ItemStack;

/**
 * MirageUShulkers — настройки одного уровня отмычки (1–5 LVL).
 *
 * @param level     уровень 1..5
 * @param name      отображаемое название (с цветовыми кодами)
 * @param redChance шанс Красного шалкера, %
 * @param minItems  минимум слотов лута
 * @param maxItems  максимум слотов лута
 * @param reference эталонный предмет из MirageDungeons (может быть null, пока не привязан)
 * @author MirageStudio, sawervatoryy
 */
public record LockpickLevel(int level, String name, double redChance, int minItems, int maxItems, ItemStack reference) {
}
