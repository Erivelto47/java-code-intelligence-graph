package com.codeatlas.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintRunnerScriptTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void validBlueprintCreatesDerivedArtifactsWhenMissing(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-example");

        CommandResult result = runRunner(repo, "harness/blueprints/phase-9-example.blueprint.md");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(Files.isRegularFile(repo.resolve("harness/handoffs/phase-9-example.handoff.md")));
        assertTrue(Files.isRegularFile(repo.resolve("harness/validations/phase-9-example.validation.md")));
        assertTrue(Files.isRegularFile(repo.resolve("harness/completion/phase-9-example.completion.md")));
        assertTrue(Files.isDirectory(repo.resolve("harness/reports/runs")));
        assertTrue(result.output().contains("runtime report path: harness/reports/runs/PHASE_9_EXAMPLE_REPORT.md"));

        String handoff = Files.readString(repo.resolve("harness/handoffs/phase-9-example.handoff.md"));
        assertTrue(handoff.contains("Phase id: `phase-9-example`"));
        assertTrue(handoff.contains("Primary blueprint path: `harness/blueprints/phase-9-example.blueprint.md`"));
        assertTrue(handoff.contains("Runtime report path: `harness/reports/runs/PHASE_9_EXAMPLE_REPORT.md`"));
        assertTrue(handoff.contains("source of truth"));
    }

    @Test
    void existingDerivedArtifactsAreLeftUnchanged(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-existing");
        Path handoff = repo.resolve("harness/handoffs/phase-9-existing.handoff.md");
        Path validation = repo.resolve("harness/validations/phase-9-existing.validation.md");
        Path completion = repo.resolve("harness/completion/phase-9-existing.completion.md");
        Files.writeString(handoff, "existing handoff\n");
        Files.writeString(validation, "existing validation\n");
        Files.writeString(completion, "existing completion\n");

        CommandResult result = runRunner(repo, "harness/blueprints/phase-9-existing.blueprint.md");

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("existing handoff\n", Files.readString(handoff));
        assertEquals("existing validation\n", Files.readString(validation));
        assertEquals("existing completion\n", Files.readString(completion));
        assertTrue(result.output().contains("exists, left unchanged: harness/handoffs/phase-9-existing.handoff.md"));
        assertTrue(result.output().contains("exists, left unchanged: harness/validations/phase-9-existing.validation.md"));
        assertTrue(result.output().contains("exists, left unchanged: harness/completion/phase-9-existing.completion.md"));
    }

    @Test
    void missingBlueprintFailsClearly(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);

        CommandResult result = runRunner(repo, "harness/blueprints/missing.blueprint.md");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Blueprint not found: harness/blueprints/missing.blueprint.md"));
    }

    @Test
    void dryRunDoesNotCreateDerivedArtifacts(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-dry-run");

        CommandResult result = runRunner(repo, "--dry-run", "harness/blueprints/phase-9-dry-run.blueprint.md");

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(Files.exists(repo.resolve("harness/handoffs/phase-9-dry-run.handoff.md")));
        assertFalse(Files.exists(repo.resolve("harness/validations/phase-9-dry-run.validation.md")));
        assertFalse(Files.exists(repo.resolve("harness/completion/phase-9-dry-run.completion.md")));
        assertFalse(Files.exists(repo.resolve("harness/reports/runs")));
        assertTrue(result.output().contains("Dry run: no files or directories will be created."));
        assertTrue(result.output().contains("would create: harness/handoffs/phase-9-dry-run.handoff.md"));
        assertTrue(result.output().contains("runtime report path: harness/reports/runs/PHASE_9_DRY_RUN_REPORT.md"));
    }

    private static Path createHarnessRepo(Path tempDir) throws Exception {
        Path repo = Files.createDirectory(tempDir.resolve("repo"));
        requireSuccess(runCommand(repo, "git", "init"));
        requireSuccess(runCommand(repo, "git", "checkout", "-b", "feature/harness-test"));

        copyHarnessFile(repo, "harness/bin/run-phase.sh");
        repo.resolve("harness/bin/run-phase.sh").toFile().setExecutable(true);
        copyHarnessFile(repo, "harness/handoffs/handoff.template.md");
        copyHarnessFile(repo, "harness/validations/validation-checklist.template.md");
        copyHarnessFile(repo, "harness/completion/completion-criteria.template.md");
        return repo;
    }

    private static void writeBlueprint(Path repo, String phaseId) throws IOException {
        Path blueprint = repo.resolve("harness/blueprints/" + phaseId + ".blueprint.md");
        Files.createDirectories(blueprint.getParent());
        Files.writeString(blueprint, "# " + phaseId + "\n", StandardCharsets.UTF_8);
    }

    private static void copyHarnessFile(Path repo, String relativePath) throws IOException {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(PROJECT_ROOT.resolve(relativePath), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static CommandResult runRunner(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("./harness/bin/run-phase.sh");
        command.addAll(List.of(args));
        return runCommand(repo, command.toArray(String[]::new));
    }

    private static CommandResult runCommand(Path directory, String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output);
    }

    private static void requireSuccess(CommandResult result) {
        assertEquals(0, result.exitCode(), result.output());
    }

    private record CommandResult(int exitCode, String output) {
    }
}
