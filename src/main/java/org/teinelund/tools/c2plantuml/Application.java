package org.teinelund.tools.c2plantuml;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * Main class
 */
public class Application {

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

        System.out.println("Welcome to c2plantuml!");
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
