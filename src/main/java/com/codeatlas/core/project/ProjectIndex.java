package com.codeatlas.core.project;

import com.codeatlas.core.entrypoint.EntrypointDescriptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProjectIndex(
        String schemaVersion,
        Instant generatedAt,
        ProjectDescriptor project,
        String language,
        List<String> frameworks,
        List<String> sourceRoots,
        List<JavaTypeDescriptor> classes,
        List<JavaTypeDescriptor> interfaces,
        List<ImplementationDescriptor> implementations,
        List<SpringBeanDescriptor> springBeans,
        List<String> controllers,
        List<String> repositories,
        List<String> clients,
        List<EntrypointDescriptor> entrypoints,
        List<UnresolvedProjectSymbol> unresolved,
        Map<String, Object> metadata
) {
}
