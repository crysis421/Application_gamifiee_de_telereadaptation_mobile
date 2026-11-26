package com.uphf.saes5;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Règles de dépendance entre couches (voir ARCHITECTURE.md), vérifiées sur les sources : une
 * dépendance interdite ajoutée par mégarde fait échouer la construction.
 */
public class ArchitectureRulesTest {

    private static final Path SOURCES = Paths.get("src/main/java/com/uphf/saes5");

    /** Les fichiers d'un package dont un import enfreint la règle. */
    private static List<String> violations(String layer, Pattern forbiddenImport,
                                           Pattern fileName) throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES.resolve(layer))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")
                    && fileName.matcher(p.getFileName().toString()).matches())
                    .collect(Collectors.toList())) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.startsWith("import ") && forbiddenImport.matcher(line).find()) {
                        found.add(SOURCES.relativize(file) + " : " + line);
                    }
                }
            }
        }
        return found;
    }

    private static final Pattern ANY = Pattern.compile(".*");

    @Test
    public void theDomain_dependsOnNothingElse_andNotOnAndroid() throws IOException {
        List<String> found = violations("domain", Pattern.compile(
                "import android\\.|import com\\.uphf\\.saes5\\.(R;|data\\.|ui\\.|di\\.)"), ANY);
        assertTrue(String.join("\n", found), found.isEmpty());
    }

    @Test
    public void theData_neverDependsOnTheInterface() throws IOException {
        List<String> found = violations("data", Pattern.compile("import com\\.uphf\\.saes5\\.(ui|di)\\."), ANY);
        assertTrue(String.join("\n", found), found.isEmpty());
    }

    @Test
    public void viewModels_andWhatTheyPublish_doNotDependOnAndroid() throws IOException {
        List<String> found = violations("ui", Pattern.compile("import android\\.|import com\\.uphf\\.saes5\\.R;"),
                Pattern.compile(".*(ViewModel|UiState|ScreenState|Event|Args)\\.java"));
        assertTrue(String.join("\n", found), found.isEmpty());
    }
}
