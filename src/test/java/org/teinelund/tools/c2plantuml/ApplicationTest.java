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
    void parseSourceFileWhereFileIsEmpty() {
        // Initialize
        List<String> cHeaderFilecontent = Collections.emptyList();
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainOneIncludeStatement() {
        // Initialize
        List<String> cHeaderFilecontent = List.of("#include \"nasm.h\"");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(1);
        assertThat(result.getIncludeHeaderFiles().get(0)).isEqualTo("nasm.h");

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainOneIncludeStatementWithSingleLineComment() {
        // Initialize
        List<String> cHeaderFilecontent = List.of("#include \"nasm.h\"  /* This is an important comment */");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(1);
        assertThat(result.getIncludeHeaderFiles().get(0)).isEqualTo("nasm.h");

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainThreeIncludeStatement() {
        // Initialize
        List<String> cHeaderFilecontent = List.of("#include \"nasm.h\"", "#include \"iflag.h\"", "#include \"perfhash.h\"");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(3);
        assertThat(result.getIncludeHeaderFiles().contains("nasm.h")).isTrue();
        assertThat(result.getIncludeHeaderFiles().contains("iflag.h")).isTrue();
        assertThat(result.getIncludeHeaderFiles().contains("perfhash.h")).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithSimpleReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int64_t insn_size(int32_t segment, int64_t offset, int bits, insn *instruction);",
                "int64_t assemble(int32_t segment, int64_t offset, int bits, insn *instruction);",
                "bool process_directives(char *);",
                "void process_pragma(char *);");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(4);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("insn_size"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("assemble"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("process_directives"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("process_pragma"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithPointerReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "char * MD5End(MD5_CTX *, char *);",
                "void **colln(Collection * c, int index);");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(2);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("MD5End"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("colln"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithConstExternReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "const char *src_set_fname(const char *newname);",
                "extern char * externMD5End(MD5_CTX *, char *);",
                "extern unsigned int ilog2_32(uint32_t v);",
                "extern void   MD5Init(MD5_CTX *context);",
                "extern const char *nasm_comment(void);"
        );
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(5);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("src_set_fname"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("externMD5End"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("ilog2_32"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("MD5Init"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_comment"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithEnumOrStructReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "enum floatize float_deffmt(int bytes);",
                "extern enum directive_result  nasm_set_limit(const char *, const char *);",
                "extern const struct use_package *nasm_find_use_package(const char *);",
                "struct rbtree *rb_insert(struct rbtree *, struct rbtree *);",
                "const struct strlist_entry *strlist_add(struct strlist *list, const char *str);");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(5);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("float_deffmt"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_set_limit"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_find_use_package"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("rb_insert"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("strlist_add"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithConstFuncOrPureFuncReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "extern unsigned int const_func ilog2_32(uint32_t v);",
                "extern int const_func alignlog2_32(uint32_t v);",
                "int pure_func nasm_strnicmp(const char *, const char *, size_t);");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(3);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("ilog2_32"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("alignlog2_32"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_strnicmp"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationsWithSafeAllocOrSafeMallocReturnType() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void * safe_alloc strlist_linearize(const struct strlist *list, char sep);",
                "void * safe_malloc(1) nasm_malloc(size_t);",
                "void * safe_malloc2(1,2) nasm_calloc(size_t, size_t);"
                );
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(3);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("strlist_linearize"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_malloc"))).isTrue();
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("nasm_calloc"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDeclarationWithDeclarationOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
            "static enum ea_type process_ea(operand *, ea *, int, int,",
            "opflags_t, insn *, const char **);"
        );
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isFalse();
        assertThat(result.getMethodDeclarations().size()).isEqualTo(1);
        assertThat(result.getMethodDeclarations().contains(new CMethodDeclaration("process_ea"))).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDefinitionsWithSimpleReturnTypeOnSingleLine() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void set_default_limits(void) {",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "set_default_limits"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDefinitionsWithSimpleReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int32_t seg_alloc(void)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "seg_alloc"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsMethodDefinitionsWithEnumReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "enum directive_result list_pragma(const struct pragma *pragma)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "list_pragma"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticMethodDefinitionsWithSimpleReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static void begintemp(void)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "begintemp"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsNonStaticMethodDefinitionsWithSimpleReturnTypeOnTwoLines_2() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void process_pragma(char *str)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "process_pragma"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticMethodDefinitionsWithSimpleReturnTypeOnTwoLines_2() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static size_t utf8_to_16be(uint8_t *str, size_t len, char *op)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "utf8_to_16be"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticMethodDefinitionsWithPointerSafeAllocReturnTypeOnTwoLines_2() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static char * safe_alloc perm_alloc(size_t len)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "perm_alloc"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticInlineMethodDefinitionsWithEnumReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static inline enum pp_token_type tok_smac_param(int param)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "tok_smac_param"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticMethodDefinitionsWithStructReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static struct src_location error_where(errflags severity)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "error_where"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticInlineMethodDefinitionsWithUnsignedReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static inline unsigned int tok_check_len(size_t len)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "tok_check_len"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticInlineMethodDefinitionsWithBasicReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static inline bool pp_concat_match(const Token *t, unsigned int mask)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "pp_concat_match"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStaticPrintfFuncMethodDefinitionsWithBasicReturnTypeOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static void printf_func(2, 3) list_error(errflags severity, const char *fmt, ...)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "list_error"))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsStatementBlockWithLonelyBraces() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   if (x > 5)",
                "   {",
                "       y = 3;",
                "   }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
    }

    @Test
    void parseSourceFileWhereFileContainsStatementBlockWithLonelyBracesAndComments() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   if (x > 5)",
                "   {  /* *p and *q have same type */",
                "       y = 3;",
                "   }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
    }

    @Test
    void parseSourceFileWhereFileContainsStatementBlockContainingOpenBrace() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   if (p->type > q->type) {",
                "       addtotemp(q->type, q->value);",
                "       lasttype = q++->type;",
                "   }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
    }

    @Test
    void parseSourceFileWhereFileContainsStatementBlockContainingNonLonelyClosingBrace() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   if (p->type > q->type) {",
                "       addtotemp(q->type, q->value);",
                "   } else",
                "   {",
                "       lasttype = q++->type;",
                "   }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
    }

    @Test
    void parseSourceFileWhereFileContainsStatementBlockContainingBothOpenAndClosingBrace() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   if (p->type > q->type) {",
                "       addtotemp(q->type, q->value);",
                "   } else {",
                "       lasttype = q++->type;",
                "   }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
    }

    @Test
    void parseSourceFileWhereFileContainValidHeaderFileContent() {
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
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
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
