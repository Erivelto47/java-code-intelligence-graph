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
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseIndexStatusScriptTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void startMovesNextToInProgressAndKeepsCommitTbd(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo,
                "1\tphase-9-current\tnext\tTBD\n"
                        + "2\tphase-9-later\tplanned\tTBD\n");

        CommandResult result = runScript(repo, "start", "phase-9-current");

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("order\tid\tstatus\tcommit\n"
                        + "1\tphase-9-current\tin_progress\tTBD\n"
                        + "2\tphase-9-later\tplanned\tTBD\n",
                Files.readString(repo.resolve("harness/phases/phase-index.tsv")));
        assertTrue(result.output().contains("phase-9-current next -> in_progress, commit TBD"));
    }

    @Test
    void validationMovesInProgressToValidationAndKeepsCommitTbd(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo, "1\tphase-9-current\tin_progress\tTBD\n");
        writeReport(repo, "phase-9-current");

        CommandResult result = runScript(repo, "validation", "phase-9-current");

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("order\tid\tstatus\tcommit\n"
                        + "1\tphase-9-current\tvalidation\tTBD\n",
                Files.readString(repo.resolve("harness/phases/phase-index.tsv")));
        assertTrue(result.output().contains("phase-9-current in_progress -> validation, commit TBD"));
    }

    @Test
    void validationRequiresGeneratedReport(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo, "1\tphase-9-current\tin_progress\tTBD\n");

        CommandResult result = runScript(repo, "validation", "phase-9-current");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Cannot move phase-9-current to validation before report exists"));
        assertEquals("order\tid\tstatus\tcommit\n"
                        + "1\tphase-9-current\tin_progress\tTBD\n",
                Files.readString(repo.resolve("harness/phases/phase-index.tsv")));
    }

    @Test
    void wrongCurrentStatusFailsWithoutChangingIndex(@TempDir Path tempDir) throws Exception {
        Path repo = createHarnessRepo(tempDir);
        writePhaseIndex(repo, "1\tphase-9-current\tplanned\tTBD\n");

        CommandResult result = runScript(repo, "start", "phase-9-current");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("Cannot update phase-9-current from status planned. Expected next."));
        assertEquals("order\tid\tstatus\tcommit\n"
                        + "1\tphase-9-current\tplanned\tTBD\n",
                Files.readString(repo.resolve("harness/phases/phase-index.tsv")));
    }

    private static Path createHarnessRepo(Path tempDir) throws Exception {
        Path repo = Files.createDirectory(tempDir.resolve("repo"));
        copyHarnessFile(repo, "harness/bin/update-phase-index-status.sh");
        repo.resolve("harness/bin/update-phase-index-status.sh").toFile().setExecutable(true);
        return repo;
    }

    private static void writePhaseIndex(Path repo, String rows) throws IOException {
        Path phaseIndex = repo.resolve("harness/phases/phase-index.tsv");
        Files.createDirectories(phaseIndex.getParent());
        Files.writeString(phaseIndex, "order\tid\tstatus\tcommit\n" + rows, StandardCharsets.UTF_8);
    }

    private static void writeReport(Path repo, String phaseId) throws IOException {
        String reportSlug = phaseId.toUpperCase().replace('-', '_');
        Path report = repo.resolve("harness/reports/runs/" + reportSlug + "_REPORT.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "# Report\n", StandardCharsets.UTF_8);
    }

    private static void copyHarnessFile(Path repo, String relativePath) throws IOException {
        Path target = repo.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(PROJECT_ROOT.resolve(relativePath), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static CommandResult runScript(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("./harness/bin/update-phase-index-status.sh");
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

    private record CommandResult(int exitCode, String output) {
    }
}
