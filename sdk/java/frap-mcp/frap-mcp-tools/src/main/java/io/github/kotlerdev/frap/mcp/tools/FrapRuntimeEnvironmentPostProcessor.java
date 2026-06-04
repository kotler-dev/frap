package io.github.kotlerdev.frap.mcp.tools;

import io.github.kotlerdev.frap.core.runtime.FrapRuntimePaths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supplies default {@code frap.runtime.dir}, {@code frap.io.work-dir}, and log file
 * paths before Spring Boot logging initializes.
 */
public class FrapRuntimeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULTS_NAME = "frapRuntimeDefaults";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final Path runtimeDir = FrapRuntimePaths.resolveRuntimeDir();
        final Map<String, Object> defaults = new LinkedHashMap<>();

        if (!StringUtils.hasText(environment.getProperty(FrapRuntimePaths.RUNTIME_DIR_PROPERTY))) {
            defaults.put(FrapRuntimePaths.RUNTIME_DIR_PROPERTY, runtimeDir.toString());
        }
        if (!StringUtils.hasText(environment.getProperty(FrapRuntimePaths.WORK_DIR_PROPERTY))) {
            defaults.put(FrapRuntimePaths.WORK_DIR_PROPERTY, FrapRuntimePaths.resolveWorkDir(null).toString());
        }
        if (!StringUtils.hasText(environment.getProperty("logging.file.name"))) {
            defaults.put("logging.file.name", FrapRuntimePaths.resolveDefaultLogFile().toString());
        }

        if (!defaults.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(DEFAULTS_NAME, defaults));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
