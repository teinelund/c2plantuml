package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CSourceFile {

    List<String> includeHeaderFiles = new ArrayList<>();
    List<CMethodDeclaration> methodDeclarations = new ArrayList<>();
    List<CMethodImplementation> methodImplementations = new ArrayList<>();

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
        CMethodImplementation cMethodImplementation = new CMethodImplementation(methodName);
        methodImplementations.add(cMethodImplementation);
    }

    public List<CMethodImplementation> getMethodDefinitions() {
        return Collections.unmodifiableList(methodImplementations);
    }
}
