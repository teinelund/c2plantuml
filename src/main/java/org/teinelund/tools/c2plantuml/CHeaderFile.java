package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CHeaderFile {

    List<String> includeHeaderFiles = new ArrayList<>();
    List<CMethodDeclaration> methodDeclarations = new ArrayList<>();

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
}
