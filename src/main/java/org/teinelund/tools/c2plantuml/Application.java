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
    Collection<CSourceFile> cHeaderFiles = new ArrayList<>();
    Collection<CSourceFile> cSourceFiles = new ArrayList<>();

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
        String fileNameName = fileName.toString();
        List<String> sourceLines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        if (fileNameName.endsWith(".h")) {
            cHeaderFiles.add(parseSourceFile(sourceLines, fileNameName));
        }
        else {
            cSourceFiles.add(parseSourceFile(sourceLines, fileNameName));
        }
    }

    private Pattern includePattern = Pattern.compile("^\\s*#include \"(.+)\"\\s*$");

    private Pattern methodDeclarationPattern = Pattern.compile(
            "^\\s*(?:extern\\s+)?(?:const\\s+)?(?:static\\s+)?(?:(?:enum|struct|unsigned)\\s+)?[a-zA-Z0-9_]+\\s+\\*{0,2}\\s*" +
                    "(?:(?:const_func|pure_func|safe_alloc|safe_malloc\\(\\d+\\)|safe_malloc2\\(\\d+,\\s*\\d+\\))\\s+)?" +
                    "([a-zA-Z0-9_]+)\\(.*\\);\\s*$");

    private Pattern methodDefinitionPattern = Pattern.compile(
            "^\\s*(?:static\\s+)?(?:inline\\s+)?(?:(?:enum|struct|unsigned)\\s+)?[a-zA-Z0-9_]+\\s+\\*{0,2}\\s*" +
                    "(?:(?:const_func|pure_func|safe_alloc|safe_malloc\\(\\d+\\)|safe_malloc2\\(\\d+,\\s*\\d+\\)|printf_func\\(\\d+,\\s*\\d+\\))\\s+)?" +
                    "([a-zA-Z0-9_]+)\\(.*\\)\\s*\\{\\s*$");

    private Pattern methodCurlyBracesClose = Pattern.compile("^\\s*}.*$");

    private Pattern methodCurlyBracesOpen = Pattern.compile("^.*\\{\\s*$");

    private Pattern singleLineComment = Pattern.compile("/\\*.*\\*/");

    // TODO:
    // int32_t base = ofmt->segbase(seg + 1);
    private Pattern methodInvokation = Pattern.compile("^\\s*(?:return\\s+)?(?:[a-zA-Z0-9_]+\\s*=\\s*)?([a-zA-Z0-9_]+)\\((.*)\\)\\s*\\;\\s*$");

    private Pattern methodInvokationInsideExpression = Pattern.compile("^.*\\s+([a-zA-Z0-9_]+)\\(.*\\).*$");



    CSourceFile parseSourceFile(List<String> sourceLines, String fileNameName) {
        String[] lineMemory = clearMemory();
        CSourceFile cSourceFile = new CSourceFile();
        String methodName = "";
        STATE state = STATE.OUTSIDE_METHOD_DEFINITION;
        int nrOfOpenCurlyBraces = 0;
        boolean isMultilineComment = false;
        for (String line : sourceLines) {

            // Replace singe line comments
            Matcher matcher = singleLineComment.matcher(line);
            if (matcher.find()) {
                line = matcher.replaceAll("");
            }
            // Remove multi line comments
            if (isMultilineComment) {
                int index = line.indexOf("*/");
                if (index >= 0) {
                    line = line.substring(index + 2);
                    isMultilineComment = false;
                }
                else {
                    continue;
                }
            }
            int index = line.indexOf("/*");
            if (index >= 0) {
                line = line.substring(0, index);
                isMultilineComment = true;
            }

            addNewLineToMemory(line, lineMemory);

            boolean foundMatch = false;

            switch (state) {
                case OUTSIDE_METHOD_DEFINITION:
                    // Check one line
                    matcher = includePattern.matcher(line);
                    if (matcher.matches()) {
                        String includeHeaderFile = matcher.group(1);
                        cSourceFile.addIncludeHeaderFile(includeHeaderFile);
                        lineMemory = clearMemory();
                        foundMatch = true;
                    }
                    matcher = methodDeclarationPattern.matcher(line);
                    if (!foundMatch && matcher.matches()) {
                        methodName = matcher.group(1);
                        cSourceFile.addMethodDeclaration(methodName);
                        lineMemory = clearMemory();
                        foundMatch = true;
                    }

                    // Find Method Definition
                    matcher = methodDefinitionPattern.matcher(line);
                    if (!foundMatch && matcher.matches()) {
                        methodName = matcher.group(1);
                        cSourceFile.addMethodImplementation(methodName);
                        lineMemory = clearMemory();
                        foundMatch = true;
                        state = STATE.INSIDE_METHOD_DEFINITION;
                        nrOfOpenCurlyBraces = 1;
                    }

                    // Consider two lines
                    if (!foundMatch && !lineMemory[1].isBlank()) {
                        String twoLines = lineMemory[1] + " " + lineMemory[0];
                        matcher = methodDeclarationPattern.matcher(twoLines);
                        if (!foundMatch && matcher.matches()) {
                            methodName = matcher.group(1);
                            cSourceFile.addMethodDeclaration(methodName);
                            lineMemory = clearMemory();
                            foundMatch = true;
                        }

                        // Find Method Definition
                        matcher = methodDefinitionPattern.matcher(twoLines);
                        if (!foundMatch && matcher.matches()) {
                            methodName = matcher.group(1);
                            cSourceFile.addMethodImplementation(methodName);
                            lineMemory = clearMemory();
                            foundMatch = true;
                            state = STATE.INSIDE_METHOD_DEFINITION;
                            nrOfOpenCurlyBraces = 1;
                        }
                    }
                    if (!foundMatch && !lineMemory[1].isBlank() && !lineMemory[2].isBlank()) {
                        String threeLines = lineMemory[2] + " " + lineMemory[1] + " " + lineMemory[0];

                        // Find Method Definition
                        matcher = methodDefinitionPattern.matcher(threeLines);
                        if (!foundMatch && matcher.matches()) {
                            methodName = matcher.group(1);
                            cSourceFile.addMethodImplementation(methodName);
                            lineMemory = clearMemory();
                            foundMatch = true;
                            state = STATE.INSIDE_METHOD_DEFINITION;
                            nrOfOpenCurlyBraces = 1;
                        }
                    }

                    // Try finding dangling braces
                    matcher = methodCurlyBracesClose.matcher(line);
                    if (!foundMatch && matcher.matches()) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces--;
                    }
                    matcher = methodCurlyBracesOpen.matcher(line);
                    if (!foundMatch && matcher.matches()) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces++;
                    }
                    break;
                case INSIDE_METHOD_DEFINITION:
                    // one line
                    boolean foundCurlyrace = false;
                    matcher = methodCurlyBracesClose.matcher(line);
                    if (matcher.matches()) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces--;
                        foundCurlyrace = true;
                    }
                    matcher = methodCurlyBracesOpen.matcher(line);
                    if (matcher.matches()) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces++;
                        foundCurlyrace = true;
                    }
                    if (foundCurlyrace) {
                        foundMatch = true;
                    }

                    matcher = methodInvokation.matcher(line);
                    if (!foundMatch && matcher.matches()) {
                        methodName = matcher.group(1);
                        cSourceFile.addMethodInvokation(methodName);
                        lineMemory = clearMemory();
                        foundMatch = true;

                        String expression = matcher.group(2);
                        matcher = methodInvokationInsideExpression.matcher(expression);
                        if (matcher.matches()) {
                            methodName = matcher.group(1);
                            cSourceFile.addMethodInvokation(methodName);
                        }
                    }

                    // two lines
                    if (!foundMatch && !lineMemory[1].isBlank()) {
                        String twoLines = lineMemory[1] + " " + lineMemory[0];
                        matcher = methodInvokation.matcher(twoLines);
                        if (matcher.matches()) {
                            methodName = matcher.group(1);
                            cSourceFile.addMethodInvokation(methodName);
                            lineMemory = clearMemory();
                            foundMatch = true;

                            String expression = matcher.group(2);
                            matcher = methodInvokationInsideExpression.matcher(expression);
                            if (matcher.matches()) {
                                methodName = matcher.group(1);
                                cSourceFile.addMethodInvokation(methodName);
                            }
                        }
                    }

                    if (nrOfOpenCurlyBraces == 0) {
                        state = STATE.OUTSIDE_METHOD_DEFINITION;
                    }
                    break;
            }
        }

        if (state == STATE.INSIDE_METHOD_DEFINITION) {
            throw new RuntimeException("No closing curly brace found in method '" + methodName + "' in file name '" + fileNameName + "'.");
        }
        if (nrOfOpenCurlyBraces != 0) {
            throw new RuntimeException("Dangling curly brace after or near method '" + methodName + "' in file name '" + fileNameName + "'.");
        }
        return cSourceFile;
    }

    String[] clearMemory() {
        String[] array = {"", "", ""};
        return array;
    }

    void addNewLineToMemory(String line, String[] lineMemory) {
        lineMemory[2] = lineMemory[1];
        lineMemory[1] = lineMemory[0];
        lineMemory[0] = line.trim();
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

enum STATE {OUTSIDE_METHOD_DEFINITION, INSIDE_METHOD_DEFINITION};
