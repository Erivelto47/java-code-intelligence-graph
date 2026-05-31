package com.codeatlas.application.decision;

import java.nio.file.Path;

public final class DecisionOutputPathResolver {
    public Path resolve(Path projectPath, String entrypoint, Path explicitOutputDirectory) {
        if (explicitOutputDirectory != null) {
            return explicitOutputDirectory;
        }
        return defaultOutputDirectory(projectPath, entrypoint);
    }

    private static Path defaultOutputDirectory(Path projectPath, String entrypoint) {
        int methodSeparator = entrypoint.lastIndexOf('.');
        int firstSeparator = entrypoint.indexOf('.');
        if (firstSeparator <= 0
                || methodSeparator <= firstSeparator
                || methodSeparator >= entrypoint.length() - 1) {
            throw new IllegalArgumentException("Missing or invalid required argument: --entrypoint");
        }
        String classQualifiedName = entrypoint.substring(0, methodSeparator);
        String methodName = entrypoint.substring(methodSeparator + 1);
        int classSeparator = classQualifiedName.lastIndexOf('.');
        String packageName = classQualifiedName.substring(0, classSeparator);
        String className = classQualifiedName.substring(classSeparator + 1);

        Path outputDirectory = projectPath.resolve(".code-atlas").resolve("decisions");
        for (String packageSegment : packageName.split("\\.")) {
            outputDirectory = outputDirectory.resolve(packageSegment);
        }
        return outputDirectory.resolve(className).resolve(methodName);
    }
}
