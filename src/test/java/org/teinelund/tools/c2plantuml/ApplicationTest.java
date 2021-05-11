package org.teinelund.tools.c2plantuml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApplicationTest {

    private Application sut = null;

    @BeforeEach
    void init(TestInfo testInfo) {
        this.sut = new Application();
    }

    @Test
    void parseHeaderFileWhereFileIsEmpty() {
        // Initialize
        List<String> cHeaderFilecontent = Collections.emptyList();
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
    }

    @Test
    void parseHeaderFileWhereFileContainOneIncludeStatement() {
        // Initialize
        List<String> cHeaderFilecontent = List.of("#include \"nasm.h\"");
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(1);
        assertThat(result.getIncludeHeaderFiles().get(0)).isEqualTo("nasm.h");

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
    }

    @Test
    void parseHeaderFileWhereFileContainThreeIncludeStatement() {
        // Initialize
        List<String> cHeaderFilecontent = List.of("#include \"nasm.h\"", "#include \"iflag.h\"", "#include \"perfhash.h\"");
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(3);
        assertThat(result.getIncludeHeaderFiles().contains("nasm.h")).isTrue();
        assertThat(result.getIncludeHeaderFiles().contains("iflag.h")).isTrue();
        assertThat(result.getIncludeHeaderFiles().contains("perfhash.h")).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
    }

    @Test
    void parseHeaderFileWhereFileContainOneMethodDeclaration() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int64_t insn_size(int32_t segment, int64_t offset, int bits, insn *instruction);");
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(1);
        assertThat(result.getMethodDeclarations().get(0).getName()).isEqualTo("insn_size");
    }

    @Test
    void parseHeaderFileWhereFileContainThreeMethodDeclaration() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int64_t assemble(int32_t segment, int64_t offset, int bits, insn *instruction);",
                "bool process_directives(char *);",
                "void process_pragma(char *);");
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(3);
        assertThat(result.getMethodDeclarations().get(0).getName()).isEqualTo("assemble");
        assertThat(result.getMethodDeclarations().get(1).getName()).isEqualTo("process_directives");
        assertThat(result.getMethodDeclarations().get(2).getName()).isEqualTo("process_pragma");
    }

    @Test
    void parseHeaderFileWhereFileContainValidContent() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "/* ----------------------------------------------------------------------- *",
                " *   ",
                " *   Copyright 1996-2009 The NASM Authors - All Rights Reserved",
                " * ----------------------------------------------------------------------- */",
                "",
                "#ifndef NASM_QUOTE_H",
                "#define NASM_QUOTE_H",
                "",
                "#include \"compiler.h\"",
                "",
                "char *nasm_quote(const char *str, size_t *len);",
                "size_t nasm_unquote(char *str, char **endptr);",
                "",
                "#endif /* NASM_QUOTE_H */",
                "");
        // Test
        CHeaderFile result = this.sut.parseHeaderFile(cHeaderFilecontent);
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(1);
        assertThat(result.getIncludeHeaderFiles().contains("compiler.h")).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(2);
        assertThat(result.getMethodDeclarations().get(0).getName()).isEqualTo("nasm_quote");
        assertThat(result.getMethodDeclarations().get(1).getName()).isEqualTo("nasm_unquote");
    }
}
