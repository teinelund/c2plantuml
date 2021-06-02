package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CSourceFile {

    private String sourceFileName;

    private List<String> includeHeaderFiles = new ArrayList<>();
    private List<CMethodDeclaration> methodDeclarations = new ArrayList<>();
    private List<CMethodImplementation> methodImplementations = new ArrayList<>();
    private CMethodImplementation currentMethodImplementation = null;

    // If this is a header file, bellow contains the method implementations.
    private CSourceFile cSourceFile;

    private List<CSourceFile> headerFiles = new ArrayList<>();

    public CSourceFile(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public void addIncludeHeaderFile(String includeHeaderFile) {
        includeHeaderFiles.add(includeHeaderFile);
    }

    public List<String> getIncludeHeaderFiles() {
        return Collections.unmodifiableList(includeHeaderFiles);
    }

    public void addMethodDeclaration(String methodName) {
        CMethodDeclaration methodDeclaration = new CMethodDeclaration(methodName);
        methodDeclarations.add(methodDeclaration);
    }

    public List<CMethodDeclaration> getMethodDeclarations() {
        return Collections.unmodifiableList(methodDeclarations);
    }

    public void addMethodImplementation(String methodName) {
        CMethodImplementation cMethodImplementation = new CMethodImplementation(methodName, this);
        methodImplementations.add(cMethodImplementation);
        currentMethodImplementation = cMethodImplementation;
    }

    public List<CMethodImplementation> getMethodDefinitions() {
        return Collections.unmodifiableList(methodImplementations);
    }

    public void addMethodInvokation(String methodName) {
        if (Objects.isNull(currentMethodImplementation)) {
            throw new RuntimeException("Method invokation outside method definition. Method invokation: '" + methodName + "'.");
        }
        currentMethodImplementation.addMethodInvokation(methodName);
    }

    public String getFileName() {
        return sourceFileName;
    }

    // If this is a header file, bellow contains the method implementations.
    public void addSourceFile(CSourceFile cSourceFile) {
        this.cSourceFile = cSourceFile;
    }

    // If this is a header file, bellow contains the method implementations.
    public CSourceFile getcSourceFile() {
        return cSourceFile;
    }

    // Both source file and header files may contain included header files.
    public void addHeaderFile(CSourceFile cHeaderFile) {
        headerFiles.add(cHeaderFile);
    }

    public List<CSourceFile> getHeaderFiles() {
        return headerFiles;
    }
}
