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

    @Parameter(names = { "-i", "--input" }, description = "Directory containing c source code to be analyzed. Mandatory.",
            order = 1)
    private String input = "";

    @Parameter(names = { "-o", "--output" }, description = "Output file path to store plant UML content. Mandatory.",
            order = 2)
    private String output = "";

    @Parameter(names = { "-m", "--method" }, description = "Method name to start the UML sequence diagram. Default " +
            "method name is 'main' and aims at the method 'void main(char* args)'. Optional.", order = 3)
    private String startingMethodName = "main";

    @Parameter(names = { "-s", "--source"}, description = "Source file name, where the method (given by the option " +
            "--method) reside. If method name is unique, source is optional. If method name is not unique, source is " +
            "mandatory. Example \"--source order.c\".", order = 4)
    private String implementingSourceFileName = "";

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
    Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
    CMethodImplementation startMethod = null;
    String plantUmlContent = "";

    public static void main(String[] args) {
        Application application = new Application();

        JCommander jc = JCommander.newBuilder()
                .addObject(application)
                .programName("c2plantuml")
                .build();
        jc.parse(args);

        try {
            application.execute(jc);
        }
        catch(Exception e) {
            printError(e.getMessage());
            e.printStackTrace();
        }
    }

    public void execute(JCommander jc) throws IOException {
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

        weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName, implementingSourceFileName);

        //printAST();

        createPlantUmlContent(this.startMethod);

        savePlantUmlContent();
    }

    // DEBUG
    void printAST() {
        for (CSourceFile file : cSourceFiles) {
            System.out.println("* " + file.getFileName());
            for (CMethodImplementation impl : file.getMethodDefinitions()) {
                System.out.println("  method impl: " + impl.getName());
                for (CMethodImplementation invok : impl.getMethodInvokations()) {
                    if (invok.getSourceFile().getFileName().equals(file.getFileName())) {
                        System.out.println("     -> " + invok.getName());
                    }
                    else {
                        System.out.println("     -> (" + invok.getSourceFile().getFileName() + ") " + invok.getName());
                    }
                }
                System.out.println("");
            }
            System.out.println("");
        }
    }

    void savePlantUmlContent() throws IOException {
        printVerbose("Save PlantUML Content.");
        Files.writeString(this.outputPath, this.plantUmlContent, StandardCharsets.UTF_8);
    }

    void createPlantUmlContent(CMethodImplementation startMethod) {
        printVerbose("Create PlantUML Content.");
        StringBuilder plantUmlContent = new StringBuilder();
        plantUmlContent.append("@startuml"); plantUmlContent.append(System.lineSeparator());
        plantUmlContent.append("autoactivate on"); plantUmlContent.append(System.lineSeparator());
        plantUmlContent.append("actor Invoker"); plantUmlContent.append(System.lineSeparator());
        createPlantUmlContentMethod(plantUmlContent, "Invoker", startMethod, 0);
        plantUmlContent.append("@enduml"); plantUmlContent.append(System.lineSeparator());
        this.plantUmlContent = plantUmlContent.toString();
    }

    String getPlantUmlContent() {
        return this.plantUmlContent;
    }

    void createPlantUmlContentMethod(StringBuilder plantUmlContent, String source,
                                     CMethodImplementation cMethodImplementation, int nrOfInvokationsInSameSourcefile) {
        String invokeUml = source + " -> " + cMethodImplementation.getSourceFile().getFileName() + " ++ : " +
                cMethodImplementation.getName();
        printVerbose(invokeUml);
        cMethodImplementation.incTouch();
        if (source.equals(cMethodImplementation.getSourceFile().getFileName())) {
            nrOfInvokationsInSameSourcefile++;
        }
        plantUmlContent.append(invokeUml); plantUmlContent.append(System.lineSeparator());
        if (cMethodImplementation.getTouch() < 2 && nrOfInvokationsInSameSourcefile < 2) {
            for (CMethodImplementation invokedMethod : cMethodImplementation.getMethodInvokations()) {
                createPlantUmlContentMethod(plantUmlContent, cMethodImplementation.getSourceFile().getFileName(),
                        invokedMethod, nrOfInvokationsInSameSourcefile);
            }
        }
        plantUmlContent.append(cMethodImplementation.getSourceFile().getFileName() + " --> " + source); plantUmlContent.append(System.lineSeparator());
        cMethodImplementation.decTouch();
        printVerbose(cMethodImplementation.getSourceFile().getFileName() + " --> " + source);
    }

    void weaveCodeTogether(Collection<CSourceFile> cHeaderFiles, Collection<CSourceFile> cSourceFiles,
                           Map<String, CSourceFile> cSourceFileMap, String startingMethodName,
                           String implementingSourceFileName) {
        printVerbose("Wave Code Together.");

        if (cSourceFiles.isEmpty()) {
            return;
        }

        // Put all files in the map
        for (CSourceFile cSourceFile : cHeaderFiles) {
            cSourceFileMap.put(cSourceFile.getFileName(), cSourceFile);
        }
        for (CSourceFile cSourceFile : cSourceFiles) {
            cSourceFileMap.put(cSourceFile.getFileName(), cSourceFile);
        }

        // Find corresponding source file for given header file
        for (CSourceFile cHeaderFile : cHeaderFiles) {
            String fileName = cHeaderFile.getFileName().replace(".h", ".c");
            if (cSourceFileMap.containsKey(fileName)) {
                CSourceFile cSourceFile = cSourceFileMap.get(fileName);
                cHeaderFile.addSourceFile(cSourceFile);
            }
        }

        // Add header files included in source file.
        for (CSourceFile cSourceFile : cSourceFiles) {
            for (String headerFile : cSourceFile.getIncludeHeaderFiles()) {
                if (cSourceFileMap.containsKey(headerFile)) {
                    CSourceFile cHeaderFile = cSourceFileMap.get(headerFile);
                    cSourceFile.addHeaderFile(cHeaderFile);
                }
            }
        }

        //
        // Connect method invokations in method implementations
        //

        // For each C source implementation file...
        for (CSourceFile cSourceFile : cSourceFiles) {
            // for each method implementation in a CSourceFile...
            for (CMethodImplementation cMethodImplementation : cSourceFile.getMethodDefinitions()) {

                // Found starting method?
                boolean isStoreMethodImplementation = false;
                if (cMethodImplementation.getName().equals(startingMethodName)) {
                    if (implementingSourceFileName.isBlank()) {
                        isStoreMethodImplementation = true;
                    }
                    else {
                        if (cSourceFile.getFileName().equals(implementingSourceFileName)) {
                            isStoreMethodImplementation = true;
                        }
                    }
                    if (isStoreMethodImplementation) {
                        if (Objects.isNull(this.startMethod)) {
                            this.startMethod = cMethodImplementation;
                        } else {
                            if (implementingSourceFileName.isBlank()) {
                                throw new IllegalStateException("Method '" + startingMethodName + "' is not unique.");
                            }
                            else {
                                throw new IllegalStateException("Method '" + startingMethodName + "' is not unique. Source name contains: '" + implementingSourceFileName + "'. Check spelling.");
                            }
                        }
                    }
                }

                // for each method invokation name in a method implementation...
                for (String methodInvokationName : cMethodImplementation.getMethodInvokationNames()) {
                    // try to find which implementation implements the method invokation.

                    // Try first the current source file
                    for (CMethodImplementation cMethodImplementation2 : cSourceFile.getMethodDefinitions()) {
                        if (cMethodImplementation2.getName().equals(methodInvokationName)) {
                            cMethodImplementation.addMethodInvokation(cMethodImplementation2);
                        }
                    }

                    // For each header file included in the CSourceFile...
                    for (CSourceFile headerFile : cSourceFile.getHeaderFiles()) {
                        // for each method implementation in the corresponding CSourceFile for the header file...
                        if (!Objects.isNull(headerFile.getcSourceFile())) {
                            for (CMethodImplementation invokedMethodImplementation : headerFile.getcSourceFile().getMethodDefinitions()) {

                                if (invokedMethodImplementation.getName().equals(methodInvokationName)) {
                                    cMethodImplementation.addMethodInvokation(invokedMethodImplementation);
                                }

                            }
                        }
                    }
                }
            }
        }

        if (Objects.isNull(this.startMethod)) {
            throw new IllegalStateException("Method '" + startingMethodName + "' is not found.");
        }
    }

    CMethodImplementation getStartingMethod() {
        return this.startMethod;
    }

    void parsePaths() throws IOException {
        printVerbose("Parse Paths.");
        Map<String, CSourceFile> cFileEntityMap = new HashMap<>();
        for (Path path : paths) {
            parsePath(path);
        }
    }

    void parsePath(Path path) throws IOException {
        printVerbose("Parse Path.");
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
    private Pattern methodInvokation = Pattern.compile("^\\s*(?:return\\s+)?(?:[a-zA-Z0-9_]+\\s*=\\s*)?[a-zA-Z0-9_]+\\(.*?\\).*\\;\\s*$");

    private Pattern methodInvokarionName = Pattern.compile("([a-zA-Z0-9_]+)\\(");



    CSourceFile parseSourceFile(List<String> sourceLines, String fileNameName) {
        printVerbose("Parse Source File: " + fileNameName + ".");
        String[] lineMemory = clearMemory();
        CSourceFile cSourceFile = new CSourceFile(fileNameName);
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
                    if (matchIncludeStatement(line, cSourceFile)) {
                        lineMemory = clearMemory();
                        foundMatch = true;
                    }
                    else if (matchMethodDeclaration(line, cSourceFile)) {
                        lineMemory = clearMemory();
                        foundMatch = true;
                    }
                    else if (matchMethodDefinition(line, cSourceFile)) {
                        lineMemory = clearMemory();
                        state = STATE.INSIDE_METHOD_DEFINITION;
                        nrOfOpenCurlyBraces = 1;
                        foundMatch = true;
                    }
                    else if (!lineMemory[1].isBlank()) {
                        // Consider two lines
                        String twoLines = lineMemory[1] + " " + lineMemory[0];
                        if (matchMethodDeclaration(twoLines, cSourceFile)) {
                            lineMemory = clearMemory();
                            foundMatch = true;
                        }
                        else if (matchMethodDefinition(twoLines, cSourceFile)) {
                            lineMemory = clearMemory();
                            state = STATE.INSIDE_METHOD_DEFINITION;
                            nrOfOpenCurlyBraces = 1;
                            foundMatch = true;
                        }
                        else if (!lineMemory[1].isBlank() && !lineMemory[2].isBlank()) {
                            // Consider three lines
                            String threeLines = lineMemory[2] + " " + lineMemory[1] + " " + lineMemory[0];
                            if (matchMethodDeclaration(threeLines, cSourceFile)) {
                                lineMemory = clearMemory();
                                foundMatch = true;
                            }
                            else if (matchMethodDefinition(threeLines, cSourceFile)) {
                                lineMemory = clearMemory();
                                state = STATE.INSIDE_METHOD_DEFINITION;
                                nrOfOpenCurlyBraces = 1;
                                foundMatch = true;
                            }
                        }
                    }
                    // Try finding dangling braces
                    if (!foundMatch && matchCurlyBracesClose(line)) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces--;
                    }
                    if (!foundMatch && matchCurlyBracesOpen(line)) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces++;
                    }

                    break;
                case INSIDE_METHOD_DEFINITION:
                    // one line
                    boolean foundCurlyrace = false;
                    if (!foundMatch && matchCurlyBracesClose(line)) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces--;
                        foundCurlyrace = true;
                    }
                    if (!foundMatch && matchCurlyBracesOpen(line)) {
                        lineMemory = clearMemory();
                        nrOfOpenCurlyBraces++;
                        foundCurlyrace = true;
                    }
                    if (!foundCurlyrace) {
                        if (matchMethodInvokation(line, cSourceFile)) {
                            lineMemory = clearMemory();
                        }
                        else if (!foundMatch && !lineMemory[1].isBlank()) {
                            String twoLines = lineMemory[1] + " " + lineMemory[0];
                            if (matchMethodInvokation(twoLines, cSourceFile)) {
                                lineMemory = clearMemory();
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

    boolean matchIncludeStatement(String line, CSourceFile cSourceFile) {
        Matcher matcher = includePattern.matcher(line);
        if (matcher.matches()) {
            String includeHeaderFile = matcher.group(1);
            cSourceFile.addIncludeHeaderFile(includeHeaderFile);
            return true;
        }
        return false;
    }

    boolean matchMethodDeclaration(String line, CSourceFile cSourceFile) {
        Matcher matcher = methodDeclarationPattern.matcher(line);
        if (matcher.matches()) {
            String methodName = matcher.group(1);
            cSourceFile.addMethodDeclaration(methodName);
            return true;
        }
        return false;
    }

    boolean matchMethodDefinition(String line, CSourceFile cSourceFile) {
        Matcher matcher = methodDefinitionPattern.matcher(line);
        if (matcher.matches()) {
            String methodName = matcher.group(1);
            cSourceFile.addMethodImplementation(methodName);
            return true;
        }
        return false;
    }

    boolean matchCurlyBracesClose(String line) {
        Matcher matcher = methodCurlyBracesClose.matcher(line);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }

    boolean matchCurlyBracesOpen(String line) {
        Matcher matcher = methodCurlyBracesOpen.matcher(line);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }

    boolean matchMethodInvokation(String line, CSourceFile cSourceFile) {
        Matcher matcher = methodInvokation.matcher(line);
        if (matcher.matches()) {
            matcher = methodInvokarionName.matcher(line);
            while (matcher.find()) {
                String methodName = matcher.group(1);
                cSourceFile.addMethodInvokation(methodName);
            }
            return true;
        }
        return false;
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
