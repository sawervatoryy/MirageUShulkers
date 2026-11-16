package ru.miragestudio.sawervatoryy.util;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MirageUShulkers — настраиваемые звуки и частицы.
 * Формат звука:   ИМЯ;громкость;высота     (ENTITY_ENDER_DRAGON_GROWL;1.0;1.0 или minecraft:entity.ender_dragon.growl)
 * Формат частицы: ИМЯ;количество;разброс;скорость
 *
 * @author MirageStudio, sawervatoryy
 */
public final class Effects {

    private static Map<String, Sound> soundIndex;

    private Effects() {
    }

    public record SoundSpec(Sound sound, float volume, float pitch) {
    }

    public record ParticleSpec(Particle particle, int count, double offset, double speed) {
    }

    public static final class EffectSet {
        public static final EffectSet EMPTY = new EffectSet(List.of(), List.of());

        private final List<SoundSpec> sounds;
        private final List<ParticleSpec> particles;

        public EffectSet(List<SoundSpec> sounds, List<ParticleSpec> particles) {
            this.sounds = sounds;
            this.particles = particles;
        }

        /** Звуки и частицы для всех игроков рядом. */
        public void play(Location loc) {
            playSounds(loc);
            playParticles(loc);
        }

        public void playSounds(Location loc) {
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            for (SoundSpec s : sounds) {
                loc.getWorld().playSound(loc, s.sound(), SoundCategory.BLOCKS, s.volume(), s.pitch());
            }
        }

        /** Звуки только одному игроку. */
        public void playSounds(Player player, Location loc) {
            if (player == null || loc == null) {
                return;
            }
            for (SoundSpec s : sounds) {
                player.playSound(loc, s.sound(), SoundCategory.BLOCKS, s.volume(), s.pitch());
            }
        }

        public void playParticles(Location loc) {
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            for (ParticleSpec p : particles) {
                try {
                    loc.getWorld().spawnParticle(p.particle(), loc, p.count(), p.offset(), p.offset(), p.offset(), p.speed());
                } catch (Exception ignored) {
                    // частицы, требующие дополнительных данных, пропускаются
                }
            }
        }
    }

    public static EffectSet load(ConfigurationSection root, String path, Logger log) {
        ConfigurationSection sec = root.getConfigurationSection(path);
        if (sec == null) {
            return EffectSet.EMPTY;
        }
        List<SoundSpec> sounds = new ArrayList<>();
        for (String raw : sec.getStringList("sounds")) {
            SoundSpec spec = parseSound(raw);
            if (spec == null) {
                log.warning("Неизвестный звук в " + path + ": " + raw);
            } else {
                sounds.add(spec);
            }
        }
        List<ParticleSpec> particles = new ArrayList<>();
        for (String raw : sec.getStringList("particles")) {
            ParticleSpec spec = parseParticle(raw);
            if (spec == null) {
                log.warning("Неизвестная частица в " + path + ": " + raw);
            } else {
                particles.add(spec);
            }
        }
        return new EffectSet(sounds, particles);
    }

    private static SoundSpec parseSound(String raw) {
        String[] p = raw.split(";");
        Sound sound = findSound(p[0].trim());
        if (sound == null) {
            return null;
        }
        float volume = p.length > 1 ? parseFloat(p[1], 1.0f) : 1.0f;
        float pitch = p.length > 2 ? parseFloat(p[2], 1.0f) : 1.0f;
        return new SoundSpec(sound, volume, pitch);
    }

    private static ParticleSpec parseParticle(String raw) {
        String[] p = raw.split(";");
        Particle particle;
        try {
            particle = Particle.valueOf(p[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        int count = p.length > 1 ? (int) parseFloat(p[1], 10) : 10;
        double offset = p.length > 2 ? parseFloat(p[2], 0.3f) : 0.3;
        double speed = p.length > 3 ? parseFloat(p[3], 0.02f) : 0.02;
        return new ParticleSpec(particle, Math.max(1, count), offset, speed);
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("minecraft:", "").replaceAll("[^a-z0-9]", "");
    }

    private static Sound findSound(String name) {
        if (soundIndex == null) {
            Map<String, Sound> idx = new HashMap<>();
            for (Sound s : Registry.SOUNDS) {
                NamespacedKey key = Registry.SOUNDS.getKey(s);
                if (key != null) {
                    idx.put(normalize(key.getKey()), s);
                }
            }
            soundIndex = idx;
        }
        return soundIndex.get(normalize(name));
    }
}
