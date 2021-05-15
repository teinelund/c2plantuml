package org.teinelund.tools.c2plantuml;

import java.util.List;
import java.util.Objects;

public class CMethodImplementation {

    private String name;

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
}
