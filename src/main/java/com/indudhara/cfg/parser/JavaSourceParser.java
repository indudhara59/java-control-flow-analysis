package com.indudhara.cfg.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public class JavaSourceParser {
    public List<MethodDeclaration> parseMethods(Path sourcePath) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(sourcePath);
            return compilationUnit.findAll(MethodDeclaration.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read Java source file: " + sourcePath, exception);
        }
    }
}
