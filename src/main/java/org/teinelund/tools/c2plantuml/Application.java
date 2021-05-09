package org.teinelund.tools.c2plantuml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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

    public void execute(String[] args, JCommander jc) {
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
    }

    void verifyParameters() {
        if (Objects.isNull(input) || input.isBlank()) {
            printError("Parameter --input is mandatory.");
            System.exit(1);
        }
        if (Objects.isNull(output) || output.isBlank()) {
            printError("Parameter --output is mandatory.");
            System.exit(1);
        }

        Path inputPath = Path.of(input);
        if (Files.notExists(inputPath)) {
            printError("Input path '" + input + "' does not exist. Check spelling.");
            System.exit(1);
        }
        if (!Files.isDirectory(inputPath)) {
            printError("Input path '" + input + "' is not a directory. Check it.");
            System.exit(1);
        }

        Path outputPath = Path.of(output);
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
