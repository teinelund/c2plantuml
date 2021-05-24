package org.teinelund.tools.c2plantuml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CMethodImplementation {

    private String name;
    private List<String> methodInvokations = new ArrayList<>();

    public CMethodImplementation(String methodName) {
        this.name = methodName;
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

    public void addMethodInvokation(String methodName) {
        methodInvokations.add(methodName);
    }

    public List<String> getMethodInvokations() {
        return Collections.unmodifiableList(methodInvokations);
    }
}
