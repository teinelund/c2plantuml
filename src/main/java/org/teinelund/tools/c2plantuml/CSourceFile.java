package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CSourceFile {

    private List<String> includeHeaderFiles = new ArrayList<>();
    private List<CMethodDeclaration> methodDeclarations = new ArrayList<>();
    private List<CMethodImplementation> methodImplementations = new ArrayList<>();
    private CMethodImplementation currentMethodImplementation = null;

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
}
