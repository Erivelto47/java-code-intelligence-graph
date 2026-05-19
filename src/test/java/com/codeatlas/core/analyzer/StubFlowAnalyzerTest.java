package com.codeatlas.core.analyzer;

import com.codeatlas.core.model.FlowGraph;
import com.codeatlas.core.model.GraphNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubFlowAnalyzerTest {
    @Test
    void createsGraphWithEntrypointNode() {
        FlowGraph graph = new StubFlowAnalyzer().analyze(Path.of("."), "com.company.FooService.method");

        assertEquals("1.0", graph.schemaVersion());
        assertEquals("com.company.FooService.method", graph.entrypoint());
        assertEquals(Instant.EPOCH, graph.generatedAt());
        assertEquals(1, graph.nodes().size());
        assertTrue(graph.edges().isEmpty());

        GraphNode node = graph.nodes().getFirst();
        assertEquals("method:com.company.FooService.method", node.id());
        assertEquals("METHOD", node.kind());
        assertEquals("com.company.FooService.method", node.qualifiedName());
        assertEquals("method", node.displayName());
        assertEquals(true, node.attributes().get("entrypoint"));
    }
}
