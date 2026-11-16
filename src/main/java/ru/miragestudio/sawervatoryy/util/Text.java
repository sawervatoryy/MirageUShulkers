package ru.miragestudio.sawervatoryy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MirageUShulkers — форматирование текста (Adventure / MiniMessage + legacy & и &#RRGGBB).
 *
 * @author MirageStudio, sawervatoryy
 */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("[&§]#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");
    private static final Map<Character, String> TAGS = new HashMap<>();

    static {
        TAGS.put('0', "<black>");
        TAGS.put('1', "<dark_blue>");
        TAGS.put('2', "<dark_green>");
        TAGS.put('3', "<dark_aqua>");
        TAGS.put('4', "<dark_red>");
        TAGS.put('5', "<dark_purple>");
        TAGS.put('6', "<gold>");
        TAGS.put('7', "<gray>");
        TAGS.put('8', "<dark_gray>");
        TAGS.put('9', "<blue>");
        TAGS.put('a', "<green>");
        TAGS.put('b', "<aqua>");
        TAGS.put('c', "<red>");
        TAGS.put('d', "<light_purple>");
        TAGS.put('e', "<yellow>");
        TAGS.put('f', "<white>");
        TAGS.put('k', "<obfuscated>");
        TAGS.put('l', "<bold>");
        TAGS.put('m', "<strikethrough>");
        TAGS.put('n', "<underlined>");
        TAGS.put('o', "<italic>");
        TAGS.put('r', "<reset>");
    }

    private Text() {
    }

    /** Конвертирует классические &-коды и &#RRGGBB в теги MiniMessage. */
    public static String legacyToMini(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher hex = HEX.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(sb, Matcher.quoteReplacement("<#" + hex.group(1) + ">"));
        }
        hex.appendTail(sb);

        Matcher legacy = LEGACY.matcher(sb.toString());
        StringBuilder out = new StringBuilder();
        while (legacy.find()) {
            char c = Character.toLowerCase(legacy.group(1).charAt(0));
            String tag = TAGS.getOrDefault(c, "");
            legacy.appendReplacement(out, Matcher.quoteReplacement(tag));
        }
        legacy.appendTail(out);
        return out.toString();
    }

    /** Компонент из строки с &-кодами, &#RRGGBB и тегами MiniMessage. */
    public static Component parse(String input) {
        return MM.deserialize(legacyToMini(input));
    }

    /** Компонент для названия/лора предмета — без курсива по умолчанию. */
    public static Component item(String input) {
        return parse(input).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> itemLines(List<String> lines) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            result.add(item(line));
        }
        return result;
    }

    /** Простой текст без форматирования. */
    public static String plain(Component component) {
        if (component == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Простой текст из строки с цветовыми кодами. */
    public static String plain(String input) {
        return plain(parse(input));
    }

    /** Красиво форматирует число: 5 → "5", 2.5 → "2.5". */
    public static String num(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        String s = String.format(Locale.US, "%.2f", value);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
