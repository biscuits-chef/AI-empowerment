import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 校验 Java 声明及方法入参均具备中文 Javadoc，防止后续编码遗漏注释。
 */
class ChineseDocumentationArchitectureTest {

    /**
     * 匹配中文字符的正则表达式。
     */
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fff]");

    /**
     * 验证全部 Java 声明和入参注释完整性。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    @Test
    void everyJavaDeclarationAndParameterHasChineseDocumentation() throws IOException {
        final JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8));
        for (final Path sourceFile : sourceFiles()) {
            final ParseResult<CompilationUnit> result = parser.parse(sourceFile);
            if (!result.isSuccessful() || !result.getResult().isPresent()) {
                fail("无法解析 Java 源文件：" + sourceFile + "，原因：" + result.getProblems());
            }
            final CompilationUnit unit = result.getResult().get();
            unit.findAll(TypeDeclaration.class).forEach(type -> assertChineseJavadoc(type, sourceFile));
            unit.findAll(FieldDeclaration.class).forEach(field -> assertChineseJavadoc(field, sourceFile));
            unit.findAll(EnumConstantDeclaration.class).forEach(value -> assertChineseJavadoc(value, sourceFile));
            unit.findAll(CallableDeclaration.class).forEach(callable -> {
                assertChineseJavadoc(callable, sourceFile);
                assertParameterTags(callable, sourceFile);
                assertReturnTag(callable, sourceFile);
                assertThrowsTags(callable, sourceFile);
            });
        }
    }

    /**
     * 收集生产与测试 Java 源文件。
     *
     * @return 待检查的 Java 源文件列表。
     *
     * @throws IOException 当输入、状态或依赖调用不满足执行条件时抛出。
     */
    private static List<Path> sourceFiles() throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(Paths.get("src"))) {
            return stream.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * 检查声明是否具有中文 Javadoc。
     *
     * @param declaration 待检查的 Java 声明。
     *
     * @param sourceFile Java 源文件路径。
     */
    private static void assertChineseJavadoc(final NodeWithJavadoc<?> declaration, final Path sourceFile) {
        final JavadocComment comment = declaration.getJavadocComment()
                .orElseGet(() -> failWithMissingJavadoc(declaration, sourceFile));
        assertTrue(CHINESE.matcher(comment.getContent()).find(),
                () -> "Javadoc 必须包含中文：" + location((Node) declaration, sourceFile));
    }

    /**
     * 报告缺失 Javadoc 的测试失败。
     *
     * @param declaration 待检查的 Java 声明。
     *
     * @param sourceFile Java 源文件路径。
     *
     * @return 报告缺失 Javadoc 的测试失败。
     */
    private static JavadocComment failWithMissingJavadoc(
            final NodeWithJavadoc<?> declaration,
            final Path sourceFile) {
        fail("缺少 Javadoc：" + location((Node) declaration, sourceFile));
        throw new IllegalStateException("unreachable");
    }

    /**
     * 检查每个入参是否具有中文说明。
     *
     * @param callable 待检查的方法或构造器声明。
     *
     * @param sourceFile Java 源文件路径。
     */
    private static void assertParameterTags(final CallableDeclaration<?> callable, final Path sourceFile) {
        final String content = callable.getJavadocComment().get().getContent();
        for (final Parameter parameter : callable.getParameters()) {
            final Pattern tag = Pattern.compile("@param\\s+" + Pattern.quote(parameter.getNameAsString())
                    + "\\s+[^\\r\\n]*[\\u4e00-\\u9fff]");
            assertTrue(tag.matcher(content).find(), () -> "方法入参缺少中文 @param："
                    + parameter.getNameAsString() + "，位置：" + location(callable, sourceFile));
        }
    }

    /**
     * 检查非 void 方法是否具有中文返回值说明。
     *
     * @param callable 待检查的方法或构造器声明。
     *
     * @param sourceFile Java 源文件路径。
     */
    private static void assertReturnTag(final CallableDeclaration<?> callable, final Path sourceFile) {
        if (!(callable instanceof MethodDeclaration)
                || ((MethodDeclaration) callable).getType().isVoidType()) {
            return;
        }
        final String content = callable.getJavadocComment().get().getContent();
        final Pattern returnTag = Pattern.compile("@return\\s+[^\\r\\n]*[\\u4e00-\\u9fff]");
        assertTrue(returnTag.matcher(content).find(),
                () -> "非 void 方法缺少中文 @return：" + location(callable, sourceFile));
    }

    /**
     * 生成源文件路径与行号定位信息。
     *
     * @param node 待读取的 JSON 或语法树节点。
     *
     * @param sourceFile Java 源文件路径。
     *
     * @return 生成源文件路径与行号定位信息。
     */
    private static String location(final Node node, final Path sourceFile) {
        return sourceFile + ":" + node.getBegin().map(position -> position.line).orElse(1);
    }

    /**
     * 检查显式声明的每一种异常都有中文异常说明。
     *
     * @param callable 待检查的方法或构造器声明。
     * @param sourceFile Java 源文件路径。
     */
    private static void assertThrowsTags(final CallableDeclaration<?> callable, final Path sourceFile) {
        final String content = callable.getJavadocComment().get().getContent();
        callable.getThrownExceptions().forEach(exception -> {
            final Pattern tag = Pattern.compile("@throws\\s+" + Pattern.quote(exception.asString())
                    + "\\s+[^\\r\\n]*[\\u4e00-\\u9fff]");
            assertTrue(tag.matcher(content).find(), () -> "显式异常缺少中文 @throws："
                    + exception.asString() + "，位置：" + location(callable, sourceFile));
        });
    }

    /**
     * 验证门禁会拒绝缺失声明、入参、返回值和异常注释的源码，而不是仅检查形式。
     */
    @Test
    void rejectsMissingDeclarationAndCallableTags() {
        final CompilationUnit unit = new JavaParser().parse(
                "class Sample { int value; int method(String input) throws Exception { return 0; } }")
                .getResult().orElseThrow(AssertionError::new);
        final Path sourceFile = Paths.get("Sample.java");
        final MethodDeclaration method = unit.findFirst(MethodDeclaration.class)
                .orElseThrow(AssertionError::new);
        assertThrows(AssertionError.class, () -> assertChineseJavadoc(method, sourceFile));
        method.setJavadocComment("中文方法说明。");
        assertThrows(AssertionError.class, () -> assertParameterTags(method, sourceFile));
        assertThrows(AssertionError.class, () -> assertReturnTag(method, sourceFile));
        assertThrows(AssertionError.class, () -> assertThrowsTags(method, sourceFile));
        method.setJavadocComment("中文方法说明。\n@param input 输入文本。\n@return 计算结果。"
                + "\n@throws Exception 输入无法处理时抛出。");
        assertChineseJavadoc(method, sourceFile);
        assertParameterTags(method, sourceFile);
        assertReturnTag(method, sourceFile);
        assertThrowsTags(method, sourceFile);
    }
}
