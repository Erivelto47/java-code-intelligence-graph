package com.codeatlas.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CoreDependencyBoundaryTest {
    @Test
    void coreDoesNotImportAdaptersOrIntellijPsi() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java/com/codeatlas/core"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("com.codeatlas.adapter"), path + " must not import adapters");
                assertFalse(source.contains("com.codeatlas.cli"), path + " must not import CLI");
                assertFalse(source.contains("com.codeatlas.output"), path + " must not import output");
                assertFalse(source.contains("com.fasterxml.jackson"), path + " must not import Jackson");
                assertFalse(source.contains("com.intellij"), path + " must not import IntelliJ PSI");
            }
        }
    }
}
