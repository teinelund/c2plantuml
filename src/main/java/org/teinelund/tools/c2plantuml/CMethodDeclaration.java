package org.teinelund.tools.c2plantuml;

import java.util.Objects;

public class CMethodDeclaration {

    private String name;

    public CMethodDeclaration(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        Objects.equals(obj, name);
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CMethodDeclaration)) {
            return false;
        }
        CMethodDeclaration other = (CMethodDeclaration) obj;
        return name == null ? other.name == null : name.equals(other.name);
    }
}
