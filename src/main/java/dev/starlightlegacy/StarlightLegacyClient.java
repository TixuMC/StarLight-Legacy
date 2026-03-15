package dev.starlightlegacy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class StarlightLegacyClient implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("StarLightLegacy");
    private static final String CONFIG_FILE = "starlightlegacy.properties";

    @Override
    public void onInitializeClient() {
        StarlightConfig config = StarlightConfig.load();
        if (!config.enabled) {
            LOGGER.info("StarLight Legacy tuning is disabled in config.");
            return;
        }

        Object gameInstance = FabricLoader.getInstance().getGameInstance();
        if (gameInstance == null) {
            LOGGER.warn("Game instance unavailable; FPS tuning skipped.");
            return;
        }

        Object options = findOptionsObject(gameInstance);
        if (options == null) {
            LOGGER.warn("Could not find client options object; FPS tuning skipped.");
            return;
        }

        int applied = 0;
        applied += setBoolean(options, Arrays.asList("fancyGraphics", "graphicsMode"), false);
        applied += setBoolean(options, Arrays.asList("enableVsync", "vsync"), false);
        applied += setBoolean(options, Arrays.asList("entityShadows"), false);
        applied += setBoolean(options, Arrays.asList("bobView", "viewBobbing"), false);

        applied += setInt(options, Arrays.asList("particles", "particleSetting"), 2);
        applied += setInt(options, Arrays.asList("mipmapLevels"), 0);
        applied += setInt(options, Arrays.asList("ao", "ambientOcclusion"), 0);
        applied += setInt(options, Arrays.asList("clouds", "ofClouds"), 0);
        applied += setInt(options, Arrays.asList("maxFps", "framerateLimit"), config.maxFps);
        applied += setInt(options, Arrays.asList("viewDistance"), config.viewDistance);

        invokeNoArgMethod(options, Arrays.asList("saveOptions", "write"));

        LOGGER.info("StarLight Legacy applied {} FPS-friendly option override(s).", applied);
    }

    private static Object findOptionsObject(Object gameInstance) {
        for (String fieldName : Arrays.asList("options", "gameSettings", "gameOptions")) {
            try {
                Field field = gameInstance.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(gameInstance);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // ignored on purpose; we try several legacy/new names
            }
        }
        return null;
    }

    private static int setBoolean(Object target, List<String> names, boolean value) {
        Field field = findField(target.getClass(), names);
        if (field == null || field.getType() != boolean.class) {
            return 0;
        }

        try {
            field.setAccessible(true);
            field.setBoolean(target, value);
            return 1;
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static int setInt(Object target, List<String> names, int value) {
        Field field = findField(target.getClass(), names);
        if (field == null || field.getType() != int.class) {
            return 0;
        }

        try {
            field.setAccessible(true);
            field.setInt(target, value);
            return 1;
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static Field findField(Class<?> type, List<String> names) {
        for (String name : names) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep trying
            }
        }
        return null;
    }

    private static void invokeNoArgMethod(Object target, List<String> methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                method.invoke(target);
                return;
            } catch (ReflectiveOperationException ignored) {
                // try next method name
            }
        }
    }

    private static final class StarlightConfig {
        private final boolean enabled;
        private final int maxFps;
        private final int viewDistance;

        private StarlightConfig(boolean enabled, int maxFps, int viewDistance) {
            this.enabled = enabled;
            this.maxFps = maxFps;
            this.viewDistance = viewDistance;
        }

        private static StarlightConfig load() {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path configPath = configDir.resolve(CONFIG_FILE);
            Properties properties = new Properties();

            properties.setProperty("enabled", "true");
            properties.setProperty("max_fps", "120");
            properties.setProperty("view_distance", "8");

            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    properties.load(in);
                } catch (IOException e) {
                    LOGGER.warn("Failed to read config file {}", configPath, e);
                }
            } else {
                try {
                    Files.createDirectories(configDir);
                    try (OutputStream out = Files.newOutputStream(configPath)) {
                        properties.store(out, "StarLight Legacy configuration");
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to create default config file {}", configPath, e);
                }
            }

            boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
            int maxFps = parseInt(properties.getProperty("max_fps"), 120, 30, 260);
            int viewDistance = parseInt(properties.getProperty("view_distance"), 8, 2, 16);

            return new StarlightConfig(enabled, maxFps, viewDistance);
        }

        private static int parseInt(String value, int fallback, int min, int max) {
            if (value == null) {
                return fallback;
            }

            try {
                int parsed = Integer.parseInt(value.trim());
                return Math.max(min, Math.min(max, parsed));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }
}
