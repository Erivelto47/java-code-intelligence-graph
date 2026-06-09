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
    void dryRunAcceptsMinimalIndexDerivesPathsAndGeneratesPrompt(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next");
        writePhaseIndex(repo, "1\tphase-9-next\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("next phase id: phase-9-next"));
        assertTrue(result.output().contains("blueprint: harness/blueprints/phase-9-next.blueprint.md"));
        assertTrue(result.output().contains("derived handoff: harness/handoffs/phase-9-next.handoff.md"));
        assertTrue(result.output().contains("derived validation: harness/validations/phase-9-next.validation.md"));
        assertTrue(result.output().contains("derived completion: harness/completion/phase-9-next.completion.md"));
        assertTrue(result.output().contains("report path: harness/reports/runs/PHASE_9_NEXT_REPORT.md"));
        assertTrue(result.output().contains("prompt path: harness/bin/build/prompts/phase-9-next.codex-prompt.txt"));
        assertFalse(Files.exists(repo.resolve("harness/handoffs/phase-9-next.handoff.md")));

        Path prompt = repo.resolve("harness/bin/build/prompts/phase-9-next.codex-prompt.txt");
        assertTrue(Files.isRegularFile(prompt));
        String promptText = Files.readString(prompt);
        assertTrue(promptText.contains("phase-9-next"));
        assertTrue(promptText.contains("harness/phases/phase-index.tsv"));
        assertTrue(promptText.contains("harness/blueprints/phase-9-next.blueprint.md"));
        assertTrue(promptText.contains("harness/handoffs/phase-9-next.handoff.md"));
        assertTrue(promptText.contains("harness/validations/phase-9-next.validation.md"));
        assertTrue(promptText.contains("harness/completion/phase-9-next.completion.md"));
        assertTrue(promptText.contains("harness/reports/runs/PHASE_9_NEXT_REPORT.md"));
        assertTrue(promptText.contains("./harness/bin/update-phase-index-status.sh start phase-9-next"));
        assertTrue(promptText.contains("./harness/bin/update-phase-index-status.sh validation phase-9-next"));
        assertTrue(promptText.contains("Do not mark this phase as `implemented`."));
        assertTrue(promptText.contains("Do not promote another phase to `next`."));
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
    void failsWhenNextBlueprintIsMissing(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo, "1\tphase-9-missing\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Blueprint not found for next phase phase-9-missing"));
        assertTrue(result.output().contains("harness/blueprints/phase-9-missing.blueprint.md"));
    }

    @Test
    void failsWhenMultiplePhasesAreMarkedNext(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next-a");
        writeBlueprint(repo, "phase-9-next-b");
        writePhaseIndex(repo,
                "1\tphase-9-next-a\tnext\tTBD\n"
                        + "2\tphase-9-next-b\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Multiple phases marked as next in harness/phases/phase-index.tsv"));
        assertTrue(result.output().contains("phase-9-next-a"));
        assertTrue(result.output().contains("phase-9-next-b"));
    }

    @Test
    void validationStatusBlocksNextPhaseExecution(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-validation");
        writeBlueprint(repo, "phase-9-next");
        writePhaseIndex(repo,
                "1\tphase-9-validation\tvalidation\tabc123\n"
                        + "2\tphase-9-next\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Cannot run next phase."));
        assertTrue(result.output().contains("Phase waiting for validation:"));
        assertTrue(result.output().contains("phase-9-validation"));
        assertTrue(result.output().contains("harness/reports/runs/PHASE_9_VALIDATION_REPORT.md"));
        assertTrue(result.output().contains("prevents stacking phase executions"));
        assertFalse(Files.exists(repo.resolve("harness/bin/build/prompts/phase-9-next.codex-prompt.txt")));
    }

    @Test
    void inProgressStatusBlocksNextPhaseExecution(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-running");
        writeBlueprint(repo, "phase-9-next");
        writePhaseIndex(repo,
                "1\tphase-9-running\tin_progress\tTBD\n"
                        + "2\tphase-9-next\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Cannot run next phase."));
        assertTrue(result.output().contains("Phase already in progress:"));
        assertTrue(result.output().contains("phase-9-running"));
        assertTrue(result.output().contains("move it to validation before starting another phase"));
        assertFalse(Files.exists(repo.resolve("harness/bin/build/prompts/phase-9-next.codex-prompt.txt")));
    }

    @Test
    void newBlueprintIsAddedAsPlannedWithoutChangingExistingStatuses(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-implemented");
        writeBlueprint(repo, "phase-9-approved");
        writeBlueprint(repo, "phase-9-next");
        writeBlueprint(repo, "phase-9-new");
        writePhaseIndex(repo,
                "1\tphase-9-implemented\timplemented\tabc123\n"
                        + "2\tphase-9-approved\tapproved\tdef456\n"
                        + "3\tphase-9-next\tnext\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        String phaseIndex = Files.readString(repo.resolve("harness/phases/phase-index.tsv"));
        assertTrue(phaseIndex.contains("1\tphase-9-implemented\timplemented\tabc123\n"));
        assertTrue(phaseIndex.contains("2\tphase-9-approved\tapproved\tdef456\n"));
        assertTrue(phaseIndex.contains("3\tphase-9-next\tnext\tTBD\n"));
        assertTrue(phaseIndex.contains("4\tphase-9-new\tplanned\tTBD\n"));
    }

    @Test
    void firstPlannedPhaseIsPromotedWhenNoNextOrValidationExists(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-planned-a");
        writeBlueprint(repo, "phase-9-planned-b");
        writePhaseIndex(repo,
                "1\tphase-9-planned-a\tplanned\tTBD\n"
                        + "2\tphase-9-planned-b\tplanned\tTBD\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Promoted first planned phase to next: phase-9-planned-a"));
        assertTrue(result.output().contains("next phase id: phase-9-planned-a"));

        String phaseIndex = Files.readString(repo.resolve("harness/phases/phase-index.tsv"));
        assertTrue(phaseIndex.contains("1\tphase-9-planned-a\tnext\tTBD\n"));
        assertTrue(phaseIndex.contains("2\tphase-9-planned-b\tplanned\tTBD\n"));
    }

    @Test
    void syncsNewBlueprintsUsingNaturalPhaseOrder(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo,
                "1\tphase-4-2-java-decision-unresolved-early-return\timplemented\t2b1b756\n"
                        + "2\tphase-4-2-1-java-block-throw-with-pre-statements\timplemented\t79635bd\n"
                        + "3\tphase-4-2-2-java-single-line-if-throw\timplemented\t07f6398\n"
                        + "4\tphase-4-3-java-if-else-decision-shape\timplemented\tcb487f1\n"
                        + "5\tharness-0-4-1-runner-prompt-output-path\timplemented\t5c19a2e\n"
                        + "6\tphase-4-3-1-java-mixed-throw-return-if-else\timplemented\tef4bb6f\n"
                        + "7\tphase-4-4-java-method-local-decision-calls\timplemented\t01d5ee1\n");
        writeBlueprint(repo, "phase-4-5-java-else-if-nested-decision-shape");
        writeBlueprint(repo, "phase-4-6-java-boolean-condition-expression-trace");
        writeBlueprint(repo, "phase-4-7-java-ternary-decision-trace");
        writeBlueprint(repo, "phase-4-8-java-switch-statement-expression-decision-trace");
        writeBlueprint(repo, "phase-4-9-java-optional-decision-trace");
        writeBlueprint(repo, "phase-4-10-java-stream-filter-match-decision-trace");
        writeBlueprint(repo, "phase-4-11-decision-trace-closeout-contract-and-examples");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains(
                "Promoted first planned phase to next: phase-4-5-java-else-if-nested-decision-shape"));
        assertTrue(result.output().contains(
                "prompt path: harness/bin/build/prompts/phase-4-5-java-else-if-nested-decision-shape.codex-prompt.txt"));
        assertFalse(result.output().contains("next phase id: phase-4-10-java-stream-filter-match-decision-trace"));

        List<String> phaseIndexLines = Files.readAllLines(repo.resolve("harness/phases/phase-index.tsv"));
        assertEquals("8\tphase-4-5-java-else-if-nested-decision-shape\tnext\tTBD", phaseIndexLines.get(8));
        assertEquals("9\tphase-4-6-java-boolean-condition-expression-trace\tplanned\tTBD", phaseIndexLines.get(9));
        assertEquals("10\tphase-4-7-java-ternary-decision-trace\tplanned\tTBD", phaseIndexLines.get(10));
        assertEquals("11\tphase-4-8-java-switch-statement-expression-decision-trace\tplanned\tTBD", phaseIndexLines.get(11));
        assertEquals("12\tphase-4-9-java-optional-decision-trace\tplanned\tTBD", phaseIndexLines.get(12));
        assertEquals("13\tphase-4-10-java-stream-filter-match-decision-trace\tplanned\tTBD", phaseIndexLines.get(13));
        assertEquals("14\tphase-4-11-decision-trace-closeout-contract-and-examples\tplanned\tTBD", phaseIndexLines.get(14));

        assertTrue(Files.isRegularFile(repo.resolve(
                "harness/bin/build/prompts/phase-4-5-java-else-if-nested-decision-shape.codex-prompt.txt")));
        assertFalse(Files.exists(repo.resolve(
                "harness/bin/build/prompts/phase-4-10-java-stream-filter-match-decision-trace.codex-prompt.txt")));
    }

    @Test
    void reportAlreadyExistingForNextPhaseBlocksExecution(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next");
        writePhaseIndex(repo, "1\tphase-9-next\tnext\tTBD\n");
        Path report = repo.resolve("harness/reports/runs/PHASE_9_NEXT_REPORT.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "# Existing report\n", StandardCharsets.UTF_8);

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Report already exists for next phase:"));
        assertTrue(result.output().contains("harness/reports/runs/PHASE_9_NEXT_REPORT.md"));
        assertTrue(result.output().contains("This phase may already be waiting for validation."));
        assertFalse(Files.exists(repo.resolve("harness/bin/build/prompts/phase-9-next.codex-prompt.txt")));
    }

    @Test
    void legacyIndexIsMigratedToMinimalIndex(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writeBlueprint(repo, "phase-9-next");
        writeLegacyPhaseIndex(repo,
                "1\tphase-9-next\tnext\tharness/blueprints/wrong.blueprint.md\tharness/reports/runs/WRONG_REPORT.md\tabc123\n");

        CommandResult result = runRunner(repo, "--dry-run");

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("order\tid\tstatus\tcommit\n"
                        + "1\tphase-9-next\tnext\tabc123\n",
                Files.readString(repo.resolve("harness/phases/phase-index.tsv")));
        assertTrue(result.output().contains("harness/reports/runs/PHASE_9_NEXT_REPORT.md"));
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
        Files.writeString(phaseIndex, "order\tid\tstatus\tcommit\n" + rows, StandardCharsets.UTF_8);
    }

    private static void writeLegacyPhaseIndex(Path repo, String rows) throws IOException {
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
