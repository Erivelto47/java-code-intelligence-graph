package com.codeatlas.core.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProjectIndexHints(
        Map<String, List<String>> implementationsByInterface,
        Set<String> springBeans,
        Set<String> repositories,
        Set<String> clients,
        int implementationMappingCount
) {
    public ProjectIndexHints {
        implementationsByInterface = copyImplementationMap(implementationsByInterface);
        springBeans = copySet(springBeans);
        repositories = copySet(repositories);
        clients = copySet(clients);
    }

    public static ProjectIndexHints empty() {
        return new ProjectIndexHints(Map.of(), Set.of(), Set.of(), Set.of(), 0);
    }

    public static ProjectIndexHints from(ProjectIndex index) {
        if (index == null) {
            return empty();
        }

        Map<String, List<String>> implementationsByInterface = new LinkedHashMap<>();
        for (ImplementationDescriptor implementation : index.implementations()) {
            implementationsByInterface
                    .computeIfAbsent(implementation.interfaceName(), ignored -> new ArrayList<>())
                    .addAll(implementation.implementations());
        }

        Set<String> springBeans = new LinkedHashSet<>();
        index.springBeans().forEach(bean -> springBeans.add(bean.beanType()));

        return new ProjectIndexHints(
                implementationsByInterface,
                springBeans,
                new LinkedHashSet<>(index.repositories()),
                new LinkedHashSet<>(index.clients()),
                index.implementations().size()
        );
    }

    public List<String> implementations(String interfaceName) {
        return implementationsByInterface.getOrDefault(interfaceName, List.of());
    }

    public boolean isSpringBean(String qualifiedName) {
        return springBeans.contains(qualifiedName);
    }

    public boolean isRepository(String qualifiedName) {
        return repositories.contains(qualifiedName);
    }

    public boolean isClient(String qualifiedName) {
        return clients.contains(qualifiedName);
    }

    private static Map<String, List<String>> copyImplementationMap(Map<String, List<String>> implementationsByInterface) {
        if (implementationsByInterface == null || implementationsByInterface.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : implementationsByInterface.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Set<String> copySet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
