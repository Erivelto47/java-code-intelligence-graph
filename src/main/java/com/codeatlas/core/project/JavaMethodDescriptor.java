package com.codeatlas.core.project;

import com.codeatlas.core.entrypoint.SourceLocation;

import java.util.List;

public record JavaMethodDescriptor(
        String name,
        String signature,
        String visibility,
        String returnType,
        List<String> annotations,
        SourceLocation sourceLocation
) {
}
