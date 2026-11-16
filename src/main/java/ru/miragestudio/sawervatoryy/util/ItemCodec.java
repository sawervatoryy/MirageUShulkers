package ru.miragestudio.sawervatoryy.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * MirageUShulkers — нативная сериализация ItemStack (Data Components 1.21.4) в Base64.
 *
 * @author MirageStudio, sawervatoryy
 */
public final class ItemCodec {

    private ItemCodec() {
    }

    public static String encode(ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return Base64.getEncoder().encodeToString(copy.serializeAsBytes());
    }

    /** @return предмет либо null, если строка повреждена. */
    public static ItemStack decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception ex) {
            return null;
        }
    }
}
