package org.teinelund.tools.c2plantuml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

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
    void parseSourceFileWhereFileContainOneIncludeStatementWithMultiLineComment() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "#include \"nasm.h\"/* This is an important comment",
                " * more comment",
                " * .. and yet more",
                "end of comment */#include \"common.h\"");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isFalse();
        assertThat(result.getIncludeHeaderFiles().size()).isEqualTo(2);
        assertThat(result.getIncludeHeaderFiles().get(0)).isEqualTo("nasm.h");
        assertThat(result.getIncludeHeaderFiles().get(1)).isEqualTo("common.h");

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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "set_default_limits", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "seg_alloc", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "list_pragma", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "begintemp", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "process_pragma", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "utf8_to_16be", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "perm_alloc", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "tok_smac_param", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "error_where", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "tok_check_len", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "pp_concat_match", null))).isTrue();
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
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "list_error", null))).isTrue();
    }

    @Test
    void parseSourceFileWhereFileContainsethodDefinitionsOnThreeLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "expr *evaluate(scanner sc, void *scprivate, struct tokenval *tv,",
                "               int *fwref, bool crit, struct eval_hints *hints)",
                "{",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();

        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();

        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        assertThat(result.getMethodDefinitions().size()).isEqualTo(1);
        assertThat(result.getMethodDefinitions().contains(new CMethodImplementation( "evaluate", null))).isTrue();
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
    void parseSourceFileWhereFileContainsOneSimpleMethodInvokationReturningVoid() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_function(void)",
                "{",
                "   nasm_free(tempexprs[--ntempexprs]);",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(1);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("nasm_free");
    }

    @Test
    void parseSourceFileWhereFileContainsOneSimpleMethodInvokationInReturnStatement() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int some_function(void)",
                "{",
                "               return finishtemp();",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(1);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("finishtemp");
    }

    @Test
    void parseSourceFileWhereFileContainsOneSimpleMethodInvokationInReturnStatementWithAssignment() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "int some_function(void)",
                "{",
                "       return tt = scanfunc(scpriv, tokval);",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(1);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("scanfunc");
    }

    @Test
    void parseSourceFileWhereFileContainsThreeSimpleMethodInvokations() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "static expr *scalarvect(int64_t scalar)",
                "{",
                "    begintemp();",
                "    addtotemp(EXPR_SIMPLE, scalar);",
                "    return finishtemp();",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(3);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("begintemp");
        assertThat(methodImplementation.getMethodInvokationNames().get(1)).isEqualTo("addtotemp");
        assertThat(methodImplementation.getMethodInvokationNames().get(2)).isEqualTo("finishtemp");
    }

    @Test
    void parseSourceFileWhereFileContainsMethodInvokationsOnTwoLines() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_method(void)",
                "{",
                "    addtotemp((base == NO_SEG ? EXPR_UNKNOWN : EXPR_SEGBASE + base),",
                "                  1L);",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(1);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("addtotemp");
    }

    @Test
    void parseSourceFileWhereFileContainsMethodInvokationsOnTwoLinesWithVariableAssignment() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_method(void)",
                "{",
                "     while (ntempexpr >= tempexpr_size) {",
                "        tempexpr = nasm_realloc(tempexpr,",
                "                                tempexpr_size * 4);",
                "     }",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(1);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("nasm_realloc");
    }

    @Test
    void parseSourceFileWhereFileContainsMethodInvokationsWithMethodInvokationAmongParameters() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_method(void)",
                "{",
                "    tempexprs = nasm_realloc(tempexprs,",
                "                         tempexprs_size * sizeof(*tempexprs));",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(2);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("nasm_realloc");
        assertThat(methodImplementation.getMethodInvokationNames().get(1)).isEqualTo("sizeof");
    }

    @Test
    void parseSourceFileWhereFileContainsTwoMethodInvokationsInTheSameLine() {
        // Initialize
        List<String> cHeaderFilecontent = List.of(
                "void some_method(void)",
                "{",
                "    preserve = is_really_simple(p) || is_really_simple(q);",
                "}");
        // Test
        CSourceFile result = this.sut.parseSourceFile(cHeaderFilecontent, "");
        // Verify
        assertThat(result.getIncludeHeaderFiles().isEmpty()).isTrue();
        assertThat(result.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(result.getMethodDefinitions().isEmpty()).isFalse();
        CMethodImplementation methodImplementation = result.getMethodDefinitions().get(0);
        assertThat(methodImplementation.getMethodInvokationNames().isEmpty()).isFalse();
        assertThat(methodImplementation.getMethodInvokationNames().size()).isEqualTo(2);
        assertThat(methodImplementation.getMethodInvokationNames().get(0)).isEqualTo("is_really_simple");
        assertThat(methodImplementation.getMethodInvokationNames().get(1)).isEqualTo("is_really_simple");
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

    @Test
    void weaveCodeTogherWhereAllCollectionsAreEmpty() {
        // Initialize
        Collection<CSourceFile> cHeaderFiles = new ArrayList<>();
        Collection<CSourceFile> cSourceFiles = new ArrayList<>();
        Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
        String startingMethodName = "";
        String implementingSourceFileName = "";
        // Test
        this.sut.weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName, implementingSourceFileName);
        // Verify
        assertThat(cHeaderFiles.isEmpty()).isTrue();
        assertThat(cSourceFiles.isEmpty()).isTrue();
        assertThat(cSourceFileMap.isEmpty()).isTrue();
    }

    @Test
    void weaveCodeTogherWithTwoHeaderAndSourceFiles() {
        // Initialize
        List<CSourceFile> cHeaderFiles = createHeaderFiles(SourceFileState.TWO_SOURCE_FILES);
        List<CSourceFile> cSourceFiles = createImplementationFiles(SourceFileState.TWO_SOURCE_FILES);
        Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
        String startingMethodName = "processOrders";
        String implementingSourceFileName = "";
        // Test
        this.sut.weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName,
                implementingSourceFileName);
        // Verify
        assertThat(cHeaderFiles.size()).isEqualTo(2);
        assertThat(cSourceFiles.size()).isEqualTo(2);

        CSourceFile resultOrderC = cSourceFiles.get(0);
        assertThat(resultOrderC.getFileName()).isEqualTo("order.c");
        assertThat(resultOrderC.getHeaderFiles().size()).isEqualTo(1);
        CSourceFile resultOrderH = resultOrderC.getHeaderFiles().get(0);
        assertThat(resultOrderH.getFileName()).isEqualTo("order.h");
        assertThat(resultOrderC.getcSourceFile()).isNull();
        assertThat(resultOrderC.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(resultOrderC.getMethodDefinitions().size()).isEqualTo(2);
        CMethodImplementation resultCreaterOrderMethodImpl = resultOrderC.getMethodDefinitions().get(0);
        assertThat(resultCreaterOrderMethodImpl.getName()).isEqualTo("createOrder");
        assertThat(resultCreaterOrderMethodImpl.getMethodInvokationNames().isEmpty()).isTrue();
        assertThat(resultCreaterOrderMethodImpl.getSourceFile()).isSameAs(resultOrderC);

        CMethodImplementation resultInitializeOrderMethodImpl = resultOrderC.getMethodDefinitions().get(1);
        assertThat(resultInitializeOrderMethodImpl.getName()).isEqualTo("initializeOrder");
        assertThat(resultInitializeOrderMethodImpl.getMethodInvokationNames().isEmpty()).isTrue();
        assertThat(resultInitializeOrderMethodImpl.getSourceFile()).isSameAs(resultOrderC);

        CSourceFile resultOrderEngineC = cSourceFiles.get(1);
        assertThat(resultOrderEngineC.getFileName()).isEqualTo("orderengine.c");
        assertThat(resultOrderEngineC.getHeaderFiles().size()).isEqualTo(2);
        CSourceFile resultOrderH2 = resultOrderEngineC.getHeaderFiles().get(0);
        assertThat(resultOrderH2.getFileName()).isEqualTo("order.h");
        assertThat(resultOrderH2).isSameAs(resultOrderH);
        CSourceFile resultOrderEngineH = resultOrderEngineC.getHeaderFiles().get(1);
        assertThat(resultOrderEngineH.getFileName()).isEqualTo("orderengine.h");

        assertThat(resultOrderEngineC.getcSourceFile()).isNull();
        assertThat(resultOrderEngineC.getMethodDeclarations().isEmpty()).isTrue();
        assertThat(resultOrderEngineC.getMethodDefinitions().size()).isEqualTo(1);
        CMethodImplementation resultProcessOrdersMethodImpl = resultOrderEngineC.getMethodDefinitions().get(0);
        assertThat(resultProcessOrdersMethodImpl.getName()).isEqualTo("processOrders");
        assertThat(resultProcessOrdersMethodImpl.getSourceFile()).isSameAs(resultOrderEngineC);
        assertThat(resultProcessOrdersMethodImpl.getMethodInvokations().size()).isEqualTo(2);
        assertThat(resultProcessOrdersMethodImpl.getMethodInvokations().get(0)).isSameAs(resultCreaterOrderMethodImpl);
        assertThat(resultProcessOrdersMethodImpl.getMethodInvokations().get(1)).isSameAs(resultInitializeOrderMethodImpl);
    }

    @Test
    void weaveCodeTogherWithTwoHeaderAndSourceFilesWhereStartingMethodNameExistAndIsUnique() {
        // Initialize
        List<CSourceFile> cHeaderFiles = createHeaderFiles(SourceFileState.TWO_SOURCE_FILES);
        List<CSourceFile> cSourceFiles = createImplementationFiles(SourceFileState.TWO_SOURCE_FILES);
        Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
        String startingMethodName = "processOrders";
        String implementingSourceFileName = "";
        // Test
        this.sut.weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName,
                implementingSourceFileName);
        // Verify
        assertThat(this.sut.getStartingMethod()).isNotNull();
        assertThat(this.sut.getStartingMethod().getName()).isEqualTo("processOrders");
    }

    @Test
    void weaveCodeTogherWithTwoHeaderAndSourceFilesWhereStartingMethodNameExistButIsNotUnique() {
        // Initialize
        List<CSourceFile> cHeaderFiles = createHeaderFiles(SourceFileState.THREE_SOURCE_FILES);
        List<CSourceFile> cSourceFiles = createImplementationFiles(SourceFileState.THREE_SOURCE_FILES);
        Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
        String startingMethodName = "processOrders";
        String implementingSourceFileName = "";
        // Test and verify
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            this.sut.weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName,
                    implementingSourceFileName);
            fail("IllegalStateException was expected to be thrown.");
        });
    }

    @Test
    void weaveCodeTogherWithTwoHeaderAndSourceFilesWhereStartingMethodNameExistAndIsUnique_2() {
        // Initialize
        List<CSourceFile> cHeaderFiles = createHeaderFiles(SourceFileState.THREE_SOURCE_FILES);
        List<CSourceFile> cSourceFiles = createImplementationFiles(SourceFileState.THREE_SOURCE_FILES);
        Map<String, CSourceFile> cSourceFileMap = new HashMap<>();
        String startingMethodName = "processOrders";
        String implementingSourceFileName = "orderengine.c";
        // Test
        this.sut.weaveCodeTogether(cHeaderFiles, cSourceFiles, cSourceFileMap, startingMethodName,
                implementingSourceFileName);
        // Verify
        assertThat(this.sut.getStartingMethod()).isNotNull();
        assertThat(this.sut.getStartingMethod().getName()).isEqualTo("processOrders");
    }

    enum SourceFileState {TWO_SOURCE_FILES, THREE_SOURCE_FILES};

    private List<CSourceFile> createHeaderFiles(SourceFileState sourceFileState) {
        List<CSourceFile> cHeaderFiles = new ArrayList<>();
        CSourceFile headerFile = new CSourceFile("order.h");
        headerFile.addMethodDeclaration("createOrder");
        headerFile.addMethodDeclaration("initializeOrder");
        cHeaderFiles.add(headerFile);
        headerFile = new CSourceFile("orderengine.h");
        headerFile.addMethodDeclaration("processOrders");
        cHeaderFiles.add(headerFile);
        if (sourceFileState == SourceFileState.THREE_SOURCE_FILES) {
            headerFile = new CSourceFile("orderstatistics.h");
            headerFile.addMethodDeclaration("processOrders");
            cHeaderFiles.add(headerFile);
        }
        return cHeaderFiles;
    }

    private List<CSourceFile> createImplementationFiles(SourceFileState sourceFileState) {
        List<CSourceFile> cSourceFiles = new ArrayList<>();
        CSourceFile cSourceFile = new CSourceFile("order.c");
        cSourceFile.addIncludeHeaderFile("order.h");
        cSourceFile.addMethodImplementation("createOrder");
        cSourceFile.addMethodImplementation("initializeOrder");
        cSourceFiles.add(cSourceFile);
        cSourceFile = new CSourceFile("orderengine.c");
        cSourceFile.addIncludeHeaderFile("order.h");
        cSourceFile.addIncludeHeaderFile("orderengine.h");
        cSourceFile.addMethodImplementation("processOrders");
        cSourceFile.addMethodInvokation("createOrder");
        cSourceFile.addMethodInvokation("initializeOrder");
        cSourceFiles.add(cSourceFile);
        if (sourceFileState == SourceFileState.THREE_SOURCE_FILES) {
            cSourceFile = new CSourceFile("orderstatistics.c");
            cSourceFile.addIncludeHeaderFile("order.h");
            cSourceFile.addIncludeHeaderFile("orderstatistics.h");
            cSourceFile.addMethodImplementation("processOrders");
            cSourceFile.addMethodInvokation("createOrder");
            cSourceFile.addMethodInvokation("initializeOrder");
            cSourceFiles.add(cSourceFile);
        }
        return cSourceFiles;
    }
}
