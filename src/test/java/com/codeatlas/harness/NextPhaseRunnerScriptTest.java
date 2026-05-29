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

class NextPhaseRunnerScriptTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void dryRunFindsNextPhaseAndGeneratesPrompt(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next");
        writePhaseIndex(repo,
                "1\tphase-9-done\timplemented\tharness/blueprints/phase-9-done.blueprint.md\tharness/reports/runs/PHASE_9_DONE_REPORT.md\tabc123\n"
                        + "2\tphase-9-next\tnext\tharness/blueprints/phase-9-next.blueprint.md\tharness/reports/runs/PHASE_9_NEXT_REPORT.md\t\n");

        String beforeIndex = Files.readString(repo.resolve("harness/phases/phase-index.tsv"));

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("next phase id: phase-9-next"));
        assertTrue(result.output().contains("prompt path: harness/bin/build/prompts/phase-9-next.codex-prompt.txt"));
        assertFalse(Files.exists(repo.resolve("harness/handoffs/phase-9-next.handoff.md")));
        assertEquals(beforeIndex, Files.readString(repo.resolve("harness/phases/phase-index.tsv")));

        Path prompt = repo.resolve("harness/bin/build/prompts/phase-9-next.codex-prompt.txt");
        assertTrue(Files.isRegularFile(prompt));
        String promptText = Files.readString(prompt);
        assertTrue(promptText.contains("phase-9-next"));
        assertTrue(promptText.contains("harness/blueprints/phase-9-next.blueprint.md"));
        assertTrue(promptText.contains("harness/handoffs/phase-9-next.handoff.md"));
        assertTrue(promptText.contains("harness/validations/phase-9-next.validation.md"));
        assertTrue(promptText.contains("harness/completion/phase-9-next.completion.md"));
        assertTrue(promptText.contains("harness/reports/runs/PHASE_9_NEXT_REPORT.md"));
    }

    @Test
    void failsWhenPhaseIndexIsMissing(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        Files.delete(repo.resolve("harness/phases/phase-index.tsv"));

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Phase index not found: harness/phases/phase-index.tsv"));
    }

    @Test
    void failsWhenNoPhaseIsMarkedNext(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo,
                "1\tphase-9-planned\tplanned\tharness/blueprints/phase-9-planned.blueprint.md\tharness/reports/runs/PHASE_9_PLANNED_REPORT.md\t\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("No phase marked as next in harness/phases/phase-index.tsv"));
    }

    @Test
    void failsWhenMultiplePhasesAreMarkedNext(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next-a");
        writeBlueprint(repo, "phase-9-next-b");
        writePhaseIndex(repo,
                "1\tphase-9-next-a\tnext\tharness/blueprints/phase-9-next-a.blueprint.md\tharness/reports/runs/PHASE_9_NEXT_A_REPORT.md\t\n"
                        + "2\tphase-9-next-b\tnext\tharness/blueprints/phase-9-next-b.blueprint.md\tharness/reports/runs/PHASE_9_NEXT_B_REPORT.md\t\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Multiple phases marked as next in harness/phases/phase-index.tsv"));
        assertTrue(result.output().contains("phase-9-next-a"));
        assertTrue(result.output().contains("phase-9-next-b"));
    }

    private static Path createHarnessRepo(Path tempDir) throws Exception {
        Path repo = Files.createDirectory(tempDir.resolve("repo"));
        requireSuccess(runCommand(repo, "git", "init"));
        requireSuccess(runCommand(repo, "git", "checkout", "-b", "feature/harness-test"));

        copyHarnessFile(repo, "harness/bin/run-phase.sh");
        copyHarnessFile(repo, "harness/bin/run-next-phase.sh");
        repo.resolve("harness/bin/run-phase.sh").toFile().setExecutable(true);
        repo.resolve("harness/bin/run-next-phase.sh").toFile().setExecutable(true);
        copyHarnessFile(repo, "harness/handoffs/handoff.template.md");
        copyHarnessFile(repo, "harness/validations/validation-checklist.template.md");
        copyHarnessFile(repo, "harness/completion/completion-criteria.template.md");
        copyHarnessFile(repo, "harness/prompts/codex-execution-prompt.template.txt");
        writePhaseIndex(repo, "");
        return repo;
    }

    private static void writeBlueprint(Path repo, String phaseId) throws IOException {
        Path blueprint = repo.resolve("harness/blueprints/" + phaseId + ".blueprint.md");
        Files.createDirectories(blueprint.getParent());
        Files.writeString(blueprint, "# " + phaseId + "\n", StandardCharsets.UTF_8);
    }

    private static void writePhaseIndex(Path repo, String rows) throws IOException {
        Path phaseIndex = repo.resolve("harness/phases/phase-index.tsv");
        Files.createDirectories(phaseIndex.getParent());
        Files.writeString(phaseIndex, "order\tid\tstatus\tblueprint\treport\tcommit\n" + rows, StandardCharsets.UTF_8);
    }

    private static void copyHarnessFile(Path repo, String relativePath) throws IOException {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(PROJECT_ROOT.resolve(relativePath), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static CommandResult runRunner(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("./harness/bin/run-next-phase.sh");
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
