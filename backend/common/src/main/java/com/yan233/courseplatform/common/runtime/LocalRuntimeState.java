package com.yan233.courseplatform.common.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Keeps local framework state inside the project instead of the user's home directory. */
public final class LocalRuntimeState {
    private LocalRuntimeState() {
    }

    public static void configure(String serviceName) {
        Path projectRoot = findProjectRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
        if (projectRoot == null) {
            return;
        }

        Path runtimeDir = projectRoot.resolve(".runtime").resolve(serviceName);
        try {
            Files.createDirectories(runtimeDir);
            Files.createDirectories(runtimeDir.resolve("nacos-cache"));
            Files.createDirectories(runtimeDir.resolve("nacos-logs"));
            Files.createDirectories(runtimeDir.resolve("sentinel-logs"));
            Files.createDirectories(runtimeDir.resolve("uploads"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create local runtime directory: " + runtimeDir, exception);
        }

        setDefaultPath("JM.SNAPSHOT.PATH", runtimeDir.resolve("nacos-cache"));
        setDefaultPath("JM.LOG.PATH", runtimeDir.resolve("nacos-logs"));
        setDefaultPath("csp.sentinel.log.dir", runtimeDir.resolve("sentinel-logs"));
        setDefaultPath("FILE_STORAGE_DIR", runtimeDir.resolve("uploads"));
        setDefaultValue("JM.LOG.FILE.SIZE", "1MB");
        setDefaultValue("JM.LOG.RETAIN.COUNT", "2");
    }

    private static Path findProjectRoot(Path current) {
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve(".git")) && Files.isDirectory(candidate.resolve("backend"))) {
                return candidate;
            }
        }
        return null;
    }

    private static void setDefaultPath(String property, Path value) {
        setDefaultValue(property, value.toString());
    }

    private static void setDefaultValue(String property, String value) {
        if (isBlank(System.getProperty(property)) && isBlank(System.getenv(toEnvironmentName(property)))) {
            System.setProperty(property, value);
        }
    }

    private static String toEnvironmentName(String property) {
        return property.replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
