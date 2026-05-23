package com.codeatlas.core.entrypoint;

public record EntrypointDescriptor(
        String id,
        EntrypointKind kind,
        String httpMethod,
        String path,
        String className,
        String methodName,
        String javaEntrypoint,
        EntrypointAnnotations annotations,
        SourceLocation sourceLocation
) {
}
