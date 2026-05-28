package io.github.kotlerdev.frap.core.client;

import java.io.File;
import java.nio.file.Path;

final class FrapCoreBinaryResolver {

    private FrapCoreBinaryResolver() {}

    static String resolve() {
        String env = System.getenv("FRAP_CORE_BIN");
        if (env != null && new File(env).isFile()) {
            return env;
        }

        String userDir = System.getProperty("user.dir");
        String[] relative = {
            "crates/target/release/frap-core-rpc",
            "crates/target/debug/frap-core-rpc",
            "../../../crates/target/release/frap-core-rpc",
            "../../../crates/target/debug/frap-core-rpc",
            "../../../../crates/target/release/frap-core-rpc",
            "../../../../crates/target/debug/frap-core-rpc",
        };

        for (String candidate : relative) {
            File f = Path.of(userDir).resolve(candidate).toFile();
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }

        throw new IllegalStateException(
            "frap-core-rpc not found under " + userDir
                + "; set FRAP_CORE_BIN or run: cargo build --release -p frap-core --bin frap-core-rpc"
        );
    }
}
