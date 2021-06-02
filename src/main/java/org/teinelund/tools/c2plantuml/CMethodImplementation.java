package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CMethodImplementation {

    private String name;
    private CSourceFile cSourceFile;
    private List<String> methodInvokationNames = new ArrayList<>();
    private List<CMethodImplementation> methodInvokations = new ArrayList<>();

    public CMethodImplementation(String methodName, CSourceFile cSourceFile) {
        this.name = methodName;
        this.cSourceFile = cSourceFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CMethodImplementation)) {
            return false;
        }
        CMethodImplementation other = (CMethodImplementation) obj;
        return name == null ? other.name == null : name.equals(other.name);
    }

    public CSourceFile getSourceFile() {
        return this.cSourceFile;
    }

    public String getName() {
        return name;
    }

    public void addMethodInvokation(String methodName) {
        methodInvokationNames.add(methodName);
    }

    public List<String> getMethodInvokationNames() {
        return Collections.unmodifiableList(methodInvokationNames);
    }

    public void addMethodInvokation(CMethodImplementation cMethodImplementation) {
        methodInvokations.add(cMethodImplementation);
    }

    public List<CMethodImplementation> getMethodInvokations() {
        return Collections.unmodifiableList(methodInvokations);
    }
}
