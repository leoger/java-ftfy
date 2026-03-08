package org.ftfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParitySnapshotTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path PARITY_FIXTURE_DIR = Path.of("src/test/resources/fixtures/parity");

    @ParameterizedTest(name = "{0}::{1}")
    @MethodSource("parityCases")
    void fixTextMatchesCommittedPythonSnapshots(
            String snapshotId, String caseId, String input, String expectedPythonFixText) {
        assertEquals(
                expectedPythonFixText,
                Ftfy.fixText(input),
                () ->
                        "Parity mismatch for snapshot '"
                                + snapshotId
                                + "', case '"
                                + caseId
                                + "'");
    }

    static Stream<Arguments> parityCases() throws IOException {
        assertTrue(
                Files.isDirectory(PARITY_FIXTURE_DIR),
                () -> "Missing parity fixture directory: " + PARITY_FIXTURE_DIR);

        List<Path> snapshotFiles;
        try (Stream<Path> files = Files.list(PARITY_FIXTURE_DIR)) {
            snapshotFiles =
                    files.filter(path -> path.getFileName().toString().endsWith(".json"))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .toList();
        }

        assertFalse(snapshotFiles.isEmpty(), "No parity snapshots found.");

        return snapshotFiles.stream()
                .map(ParitySnapshotTest::readSnapshot)
                .flatMap(
                        snapshot ->
                                snapshot.cases().stream()
                                        .map(
                                                testCase ->
                                                        Arguments.of(
                                                                snapshot.snapshotId(),
                                                                testCase.id(),
                                                                testCase.input(),
                                                                testCase.pythonFixText())));
    }

    private static SnapshotFile readSnapshot(Path snapshotPath) {
        try {
            SnapshotFile snapshot = OBJECT_MAPPER.readValue(snapshotPath.toFile(), SnapshotFile.class);
            assertEquals(
                    1,
                    snapshot.formatVersion(),
                    () ->
                            "Unsupported format_version in "
                                    + snapshotPath
                                    + ": "
                                    + snapshot.formatVersion());
            assertTrue(
                    snapshot.snapshotId() != null && !snapshot.snapshotId().isBlank(),
                    () -> "snapshot_id is required in " + snapshotPath);
            assertTrue(
                    snapshot.cases() != null && !snapshot.cases().isEmpty(),
                    () -> "cases must be non-empty in " + snapshotPath);
            snapshot.cases().forEach(
                    testCase -> {
                        assertTrue(
                                testCase.id() != null && !testCase.id().isBlank(),
                                () -> "Case id is required in " + snapshotPath);
                        assertTrue(testCase.input() != null, () -> "input is required in " + snapshotPath);
                        assertTrue(
                                testCase.pythonFixText() != null,
                                () -> "python_fix_text is required in " + snapshotPath);
                    });
            return snapshot;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read parity snapshot: " + snapshotPath, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SnapshotFile(
            @JsonProperty("format_version") int formatVersion,
            @JsonProperty("snapshot_id") String snapshotId,
            @JsonProperty("cases") List<ParityCase> cases) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ParityCase(
            @JsonProperty("id") String id,
            @JsonProperty("input") String input,
            @JsonProperty("python_fix_text") String pythonFixText) {}
}
