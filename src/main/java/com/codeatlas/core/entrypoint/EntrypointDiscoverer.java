package com.codeatlas.core.entrypoint;

import java.nio.file.Path;

public interface EntrypointDiscoverer {
    EntrypointIndex discover(Path projectPath);
}
