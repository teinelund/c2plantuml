package org.teinelund.tools.c2plantuml;

import java.nio.file.Path;
import java.util.Map;

public class CSourceFile {

    private String headerFileName;
    private Path headerFilePath;
    private Path sourceFilePath;
    private Map<String, CMethodImplementation> methods;

}
