package com.codeatlas.core.project;

import com.codeatlas.core.entrypoint.SourceLocation;

import java.util.List;

public record SpringBeanDescriptor(
        String id,
        String kind,
        String beanType,
        List<String> annotations,
        String sourceFile,
        SourceLocation sourceLocation
) {
}
