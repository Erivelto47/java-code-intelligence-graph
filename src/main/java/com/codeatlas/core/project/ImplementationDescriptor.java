package com.codeatlas.core.project;

import java.util.List;

public record ImplementationDescriptor(
        String interfaceName,
        List<String> implementations
) {
}
