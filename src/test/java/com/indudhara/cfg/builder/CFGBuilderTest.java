package com.indudhara.cfg.builder;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.indudhara.cfg.exporter.DotExporter;
import com.indudhara.cfg.model.CFG;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CFGBuilderTest {
    private final CFGBuilder builder = new CFGBuilder();
    private final DotExporter exporter = new DotExporter();

    @Test
    void buildsIfElseReturnGraph() {
        String dot = buildDot("""
                int compute(int value) {
                    int result = value * 2;
                    if (result > 10) {
                        result = result - 1;
                    } else {
                        result = result + 1;
                    }
                    return result;
                }
                """);

        assertTrue(dot.contains("if (result > 10)"));
        assertTrue(dot.contains("[label=\"true\"]"));
        assertTrue(dot.contains("[label=\"false\"]"));
        assertTrue(dot.contains("return result;"));
    }

    @Test
    void modelsBreakAndContinueInLoop() {
        String dot = buildDot("""
                int scan(int[] values) {
                    int total = 0;
                    for (int value : values) {
                        if (value < 0) {
                            continue;
                        }
                        if (value == 10) {
                            break;
                        }
                        total = total + value;
                    }
                    return total;
                }
                """);

        assertTrue(dot.contains("for each (int value : values)"));
        assertTrue(dot.contains("continue;"));
        assertTrue(dot.contains("break;"));
        assertTrue(dot.contains("return total;"));
    }

    @Test
    void modelsSwitchAndTryCatchFinally() {
        String switchDot = buildDot("""
                int choose(int code) {
                    switch (code) {
                        case 1:
                            return 10;
                        default:
                            return 0;
                    }
                }
                """);
        String tryDot = buildDot("""
                int divide(int left, int right) {
                    try {
                        return left / right;
                    } catch (ArithmeticException exception) {
                        return 0;
                    } finally {
                        System.out.println("done");
                    }
                }
                """);

        assertTrue(switchDot.contains("switch (code)"));
        assertTrue(switchDot.contains("case 1"));
        assertTrue(switchDot.contains("default"));
        assertTrue(tryDot.contains("try"));
        assertTrue(tryDot.contains("catch (ArithmeticException exception)"));
        assertTrue(tryDot.contains("finally"));
    }

    private String buildDot(String methodSource) {
        MethodDeclaration method = StaticJavaParser.parseBodyDeclaration(methodSource).asMethodDeclaration();
        CFG cfg = builder.build(method);
        return exporter.toDot(cfg);
    }
}
