package com.codeatlas.core.project;

import java.nio.file.Path;

public interface ProjectIndexer {
    ProjectIndex index(Path projectPath);
}
