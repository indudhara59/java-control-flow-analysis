package com.indudhara.cfg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.indudhara.cfg.builder.CFGBuilder;
import com.indudhara.cfg.exporter.DotExporter;
import com.indudhara.cfg.model.CFG;
import com.indudhara.cfg.parser.JavaSourceParser;

import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Path sourcePath = args.length > 0 ? Path.of(args[0]) : Path.of("examples", "TestInput.java");

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
            System.out.println(exporter.toDot(cfg));
        }
    }
}
