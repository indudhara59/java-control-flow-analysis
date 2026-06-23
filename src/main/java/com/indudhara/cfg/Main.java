package com.indudhara.cfg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.indudhara.cfg.builder.CFGBuilder;
import com.indudhara.cfg.exporter.DotExporter;
import com.indudhara.cfg.model.CFG;
import com.indudhara.cfg.parser.JavaSourceParser;

import java.nio.file.Path;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Path sourcePath = args.length > 0 ? Path.of(args[0]) : Path.of("examples", "TestInput.java");
        Path outputDirectory = args.length > 1 ? Path.of(args[1]) : null;

        JavaSourceParser parser = new JavaSourceParser();
        CFGBuilder builder = new CFGBuilder();
        DotExporter exporter = new DotExporter();

        List<MethodDeclaration> methods = parser.parseMethods(sourcePath);
        if (methods.isEmpty()) {
            System.out.println("No methods found in " + sourcePath);
            return;
        }

        for (MethodDeclaration method : methods) {
            CFG cfg = builder.build(method);
            String dot = exporter.toDot(cfg);
            System.out.println(dot);

            if (outputDirectory != null) {
                writeDotFile(outputDirectory, cfg, dot);
            }
        }
    }

    private static void writeDotFile(Path outputDirectory, CFG cfg, String dot) {
        try {
            Files.createDirectories(outputDirectory);
            Path outputFile = outputDirectory.resolve(cfg.getMethodName() + ".dot");
            Files.writeString(outputFile, dot);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write DOT output to " + outputDirectory, exception);
        }
    }
}
