package com.codeatlas.core.project;

import com.codeatlas.core.entrypoint.SourceLocation;

import java.util.List;

public record JavaTypeDescriptor(
        String fullyQualifiedName,
        String packageName,
        String simpleName,
        String kind,
        String sourceFile,
        List<String> annotations,
        List<JavaMethodDescriptor> methods,
        SourceLocation sourceLocation
) {
}
