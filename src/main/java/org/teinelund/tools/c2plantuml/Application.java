package org.teinelund.tools.c2plantuml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Main class
 */
public class Application {

    @Parameter(names = { "-i", "--input" }, description = "Directory containing c source code to be analyzed. Mandatory.", order = 1)
    private String input = "";

    @Parameter(names = { "-o", "--output" }, description = "Output file path to store plant UML content. Mandatory.", order = 2)
    private String output = "";

    @Parameter(names = { "-v", "--verbose" }, description = "Verbose output.", order = 50)
    private boolean verbose = false;

    @Parameter(names = { "-V", "--version" }, description = "Version of application.", order = 51)
    private boolean version = false;

    @Parameter(names = { "-h", "--help" }, help = true, order = 52)
    private boolean help = false;

    Path inputPath;
    Path outputPath;
    Collection<Path> paths;
    Collection<CHeaderFile> cHeaderFiles = new ArrayList<>();

    public static void main(String[] args) {
        Application application = new Application();

        JCommander jc = JCommander.newBuilder()
                .addObject(application)
                .programName("c2plantuml")
                .build();
        jc.parse(args);

        try {
            application.execute(args, jc);
        }
        catch(Exception e) {
            printError(e.getMessage());
            e.printStackTrace();
        }
    }

    public void execute(String[] args, JCommander jc) throws IOException {
        if (help || version) {
            if (help) {
                jc.usage();
            }
            else {
                String versionString = this.getClass().getPackage().getImplementationVersion();
                System.out.println("C2plantuml. Version: " + versionString);
                System.out.println("Copyright (c) 2021 Henrik Teinelund.");
            }
            System.exit(0);
        }

        printVerbose("Verbose mode on.");

        verifyParameters();

        paths = fetchCFiles();

        parsePaths();

    }

    void parsePaths() throws IOException {
        Map<String, CSourceFile> cFileEntityMap = new HashMap<>();
        for (Path path : paths) {
            parsePath(path);
        }
    }

    void parsePath(Path path) throws IOException {
        Path fileName = path.getFileName();
        String FileNameName = fileName.toString();
        List<String> sourceLines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        if (FileNameName.endsWith(".h")) {
            cHeaderFiles.add(parseHeaderFile(sourceLines));
        }
        else {
            parseSourceFile(sourceLines, FileNameName);
        }
    }

    void parseSourceFile(List<String> sourceLines, String fileName) {
    }

    private Pattern includePattern = Pattern.compile("^\\s*#include \"(.+)\"\\s*$");
    private Pattern methodDeclarationPattern = Pattern.compile("^\\s*[a-zA-Z0-9_]+\\s+\\*?([a-zA-Z0-9_]+)\\(.*\\);\\s*$");

    CHeaderFile parseHeaderFile(List<String> sourceLines) {
        CHeaderFile cHeaderFile = new CHeaderFile();
        for (String line : sourceLines) {
            Matcher matcher = includePattern.matcher(line);
            if ( matcher.matches() ) {
                String includeHeaderFile = matcher.group(1);
                cHeaderFile.addIncludeHeaderFile(includeHeaderFile);
            }
            matcher = methodDeclarationPattern.matcher(line);
            if ( matcher.matches() ) {
                String methodName = matcher.group(1);
                cHeaderFile.addMethodDeclaration(methodName);
            }
        }
        return cHeaderFile;
    }

    Collection<Path> fetchCFiles() throws IOException {
        printVerbose("Fetch C Files.");
        Collection<Path> paths = new LinkedList<>();
        try (Stream<Path> entries = Files.walk(inputPath)) {
            entries.forEach( p -> {
                if (Files.isRegularFile(p)) {
                    if (p.toString().endsWith(".h") || p.toString().endsWith(".c")) {
                        paths.add(p);
                    }
                }
            });
        }

        // Verbose output
        printVerbose("Fetched " + paths.size() + " of paths.");
        if (verbose) {
            for (Path path : paths) {
                System.out.println("> " + path.toString());
            }
        }

        return paths;
    }

    void verifyParameters() {
        printVerbose("Verify Parameters.");
        if (Objects.isNull(input) || input.isBlank()) {
            printError("Parameter --input is mandatory.");
            System.exit(1);
        }
        if (Objects.isNull(output) || output.isBlank()) {
            printError("Parameter --output is mandatory.");
            System.exit(1);
        }

        inputPath = Path.of(input);
        if (Files.notExists(inputPath)) {
            printError("Input path '" + input + "' does not exist. Check spelling.");
            System.exit(1);
        }
        if (!Files.isDirectory(inputPath)) {
            printError("Input path '" + input + "' is not a directory. Check it.");
            System.exit(1);
        }

        outputPath = Path.of(output);
        if (Files.exists(outputPath)) {
            printError("Output path '" + output + "' does exist. Check it.");
            System.exit(1);
        }
    }

    static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    void printVerbose(String message) {
        if (verbose) {
            System.out.println("[VERBOSE] " + message);
        }
    }
}
