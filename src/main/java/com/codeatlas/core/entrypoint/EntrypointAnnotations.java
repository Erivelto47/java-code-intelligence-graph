package com.codeatlas.core.entrypoint;

import java.util.List;

public record EntrypointAnnotations(
        List<String> classLevel,
        List<String> methodLevel
) {
}
