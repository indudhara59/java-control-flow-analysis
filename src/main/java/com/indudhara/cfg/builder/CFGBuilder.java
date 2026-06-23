package com.indudhara.cfg.builder;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.indudhara.cfg.model.CFG;
import com.indudhara.cfg.model.CFGNode;

import java.util.ArrayList;
import java.util.List;

public class CFGBuilder {
    private CFG cfg;
    private CFGNode exitNode;

    public CFG build(MethodDeclaration method) {
        cfg = new CFG(method.getNameAsString());
        CFGNode entryNode = cfg.addNode("ENTRY " + method.getNameAsString());
        exitNode = cfg.addNode("EXIT " + method.getNameAsString());

        List<Flow> openFlows = method.getBody()
                .map(body -> buildBlock(body, List.of(new Flow(entryNode, null))))
                .orElseGet(() -> List.of(new Flow(entryNode, null)));

        for (Flow flow : openFlows) {
            addEdge(flow, exitNode);
        }

        return cfg;
    }

    private List<Flow> buildBlock(BlockStmt block, List<Flow> incoming) {
        List<Flow> current = incoming;
        for (Statement statement : block.getStatements()) {
            current = buildStatement(statement, current);
        }
        return current;
    }

    private List<Flow> buildStatement(Statement statement, List<Flow> incoming) {
        if (statement.isBlockStmt()) {
            return buildBlock(statement.asBlockStmt(), incoming);
        }
        if (statement.isExpressionStmt()) {
            return buildExpression(statement.asExpressionStmt(), incoming);
        }
        if (statement.isReturnStmt()) {
            return buildReturn(statement.asReturnStmt(), incoming);
        }
        if (statement.isIfStmt()) {
            return buildIf(statement.asIfStmt(), incoming);
        }
        if (statement.isWhileStmt()) {
            return buildWhile(statement.asWhileStmt(), incoming);
        }

        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        return List.of(new Flow(node, null));
    }

    private List<Flow> buildExpression(ExpressionStmt statement, List<Flow> incoming) {
        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        return List.of(new Flow(node, null));
    }

    private List<Flow> buildReturn(ReturnStmt statement, List<Flow> incoming) {
        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        cfg.addEdge(node, exitNode);
        return List.of();
    }

    private List<Flow> buildIf(IfStmt statement, List<Flow> incoming) {
        CFGNode condition = cfg.addNode("if (" + statement.getCondition() + ")");
        connectAll(incoming, condition);

        List<Flow> thenFlows = buildStatement(statement.getThenStmt(), List.of(new Flow(condition, "true")));
        List<Flow> elseFlows = statement.getElseStmt()
                .map(elseStatement -> buildStatement(elseStatement, List.of(new Flow(condition, "false"))))
                .orElseGet(() -> List.of(new Flow(condition, "false")));

        List<Flow> merged = new ArrayList<>();
        merged.addAll(thenFlows);
        merged.addAll(elseFlows);
        return merged;
    }

    private List<Flow> buildWhile(WhileStmt statement, List<Flow> incoming) {
        CFGNode condition = cfg.addNode("while (" + statement.getCondition() + ")");
        connectAll(incoming, condition);

        List<Flow> bodyFlows = buildStatement(statement.getBody(), List.of(new Flow(condition, "true")));
        for (Flow flow : bodyFlows) {
            addEdge(flow, condition);
        }

        return List.of(new Flow(condition, "false"));
    }

    private void connectAll(List<Flow> incoming, CFGNode target) {
        for (Flow flow : incoming) {
            addEdge(flow, target);
        }
    }

    private void addEdge(Flow flow, CFGNode target) {
        cfg.addEdge(flow.node(), target, flow.label());
    }

    private String clean(String text) {
        return text.replace(System.lineSeparator(), " ").trim();
    }

    private record Flow(CFGNode node, String label) {
    }
}
