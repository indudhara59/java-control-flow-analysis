package com.indudhara.cfg.builder;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.indudhara.cfg.model.CFG;
import com.indudhara.cfg.model.CFGNode;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class CFGBuilder {
    private CFG cfg;
    private CFGNode exitNode;
    private final Deque<LoopContext> loops = new ArrayDeque<>();
    private final Deque<BreakContext> breakContexts = new ArrayDeque<>();
    private final Deque<CFGNode> finalizers = new ArrayDeque<>();

    public CFG build(MethodDeclaration method) {
        cfg = new CFG(method.getNameAsString());
        loops.clear();
        breakContexts.clear();
        finalizers.clear();
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
        List<Statement> statements = block.getStatements();

        for (int index = 0; index < statements.size(); index++) {
            Statement statement = statements.get(index);
            if (isBasicBlockStatement(statement)) {
                List<String> labels = new ArrayList<>();
                int cursor = index;
                while (cursor < statements.size() && isBasicBlockStatement(statements.get(cursor))) {
                    labels.add(clean(statements.get(cursor).toString()));
                    cursor++;
                }
                current = buildBasicBlock(labels, current);
                index = cursor - 1;
            } else {
                current = buildStatement(statement, current);
            }
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
        if (statement.isForStmt()) {
            return buildFor(statement.asForStmt(), incoming);
        }
        if (statement.isForEachStmt()) {
            return buildForEach(statement.asForEachStmt(), incoming);
        }
        if (statement.isDoStmt()) {
            return buildDoWhile(statement.asDoStmt(), incoming);
        }
        if (statement.isBreakStmt()) {
            return buildBreak(statement.asBreakStmt(), incoming);
        }
        if (statement.isContinueStmt()) {
            return buildContinue(statement.asContinueStmt(), incoming);
        }
        if (statement.isSwitchStmt()) {
            return buildSwitch(statement.asSwitchStmt(), incoming);
        }
        if (statement.isTryStmt()) {
            return buildTry(statement.asTryStmt(), incoming);
        }

        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        return List.of(new Flow(node, null));
    }

    private boolean isBasicBlockStatement(Statement statement) {
        return statement.isExpressionStmt();
    }

    private List<Flow> buildBasicBlock(List<String> labels, List<Flow> incoming) {
        CFGNode node = cfg.addNode("BLOCK\n" + String.join("\n", labels));
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
        cfg.addEdge(node, finalizers.isEmpty() ? exitNode : finalizers.peek());
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

        BreakContext breakContext = new BreakContext();
        LoopContext loop = new LoopContext(condition);
        breakContexts.push(breakContext);
        loops.push(loop);
        List<Flow> bodyFlows = buildStatement(statement.getBody(), List.of(new Flow(condition, "true")));
        loops.pop();
        breakContexts.pop();

        for (Flow flow : bodyFlows) {
            addEdge(flow, condition);
        }

        List<Flow> exits = new ArrayList<>();
        exits.add(new Flow(condition, "false"));
        exits.addAll(breakContext.breakFlows());
        return exits;
    }

    private List<Flow> buildFor(ForStmt statement, List<Flow> incoming) {
        List<Flow> current = incoming;
        if (!statement.getInitialization().isEmpty()) {
            CFGNode init = cfg.addNode("for init: " + joinExpressions(statement.getInitialization()));
            connectAll(current, init);
            current = List.of(new Flow(init, null));
        }

        CFGNode condition = cfg.addNode(statement.getCompare()
                .map(compare -> "for (" + compare + ")")
                .orElse("for (true)"));
        connectAll(current, condition);

        CFGNode update = null;
        if (!statement.getUpdate().isEmpty()) {
            update = cfg.addNode("for update: " + joinExpressions(statement.getUpdate()));
        }

        CFGNode continueTarget = update != null ? update : condition;
        BreakContext breakContext = new BreakContext();
        LoopContext loop = new LoopContext(continueTarget);
        breakContexts.push(breakContext);
        loops.push(loop);
        List<Flow> bodyFlows = buildStatement(statement.getBody(), List.of(new Flow(condition, "true")));
        loops.pop();
        breakContexts.pop();

        if (update != null) {
            connectAll(bodyFlows, update);
            cfg.addEdge(update, condition);
        } else {
            for (Flow flow : bodyFlows) {
                addEdge(flow, condition);
            }
        }

        List<Flow> exits = new ArrayList<>();
        exits.add(new Flow(condition, "false"));
        exits.addAll(breakContext.breakFlows());
        return exits;
    }

    private List<Flow> buildForEach(ForEachStmt statement, List<Flow> incoming) {
        CFGNode condition = cfg.addNode("for each (" + statement.getVariable() + " : " + statement.getIterable() + ")");
        connectAll(incoming, condition);

        BreakContext breakContext = new BreakContext();
        LoopContext loop = new LoopContext(condition);
        breakContexts.push(breakContext);
        loops.push(loop);
        List<Flow> bodyFlows = buildStatement(statement.getBody(), List.of(new Flow(condition, "true")));
        loops.pop();
        breakContexts.pop();

        for (Flow flow : bodyFlows) {
            addEdge(flow, condition);
        }

        List<Flow> exits = new ArrayList<>();
        exits.add(new Flow(condition, "false"));
        exits.addAll(breakContext.breakFlows());
        return exits;
    }

    private List<Flow> buildDoWhile(DoStmt statement, List<Flow> incoming) {
        CFGNode bodyEntry = cfg.addNode("do");
        connectAll(incoming, bodyEntry);
        CFGNode condition = cfg.addNode("while (" + statement.getCondition() + ")");

        BreakContext breakContext = new BreakContext();
        LoopContext loop = new LoopContext(condition);
        breakContexts.push(breakContext);
        loops.push(loop);
        List<Flow> bodyFlows = buildStatement(statement.getBody(), List.of(new Flow(bodyEntry, null)));
        loops.pop();
        breakContexts.pop();

        connectAll(bodyFlows, condition);
        cfg.addEdge(condition, bodyEntry, "true");

        List<Flow> exits = new ArrayList<>();
        exits.add(new Flow(condition, "false"));
        exits.addAll(breakContext.breakFlows());
        return exits;
    }

    private List<Flow> buildBreak(BreakStmt statement, List<Flow> incoming) {
        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        if (!breakContexts.isEmpty()) {
            breakContexts.peek().breakFlows().add(new Flow(node, null));
        }
        return List.of();
    }

    private List<Flow> buildContinue(ContinueStmt statement, List<Flow> incoming) {
        CFGNode node = cfg.addNode(clean(statement.toString()));
        connectAll(incoming, node);
        if (!loops.isEmpty()) {
            cfg.addEdge(node, loops.peek().continueTarget());
        }
        return List.of();
    }

    private List<Flow> buildSwitch(SwitchStmt statement, List<Flow> incoming) {
        CFGNode selector = cfg.addNode("switch (" + statement.getSelector() + ")");
        connectAll(incoming, selector);

        BreakContext breakContext = new BreakContext();
        breakContexts.push(breakContext);
        List<Flow> exits = new ArrayList<>();
        for (SwitchEntry entry : statement.getEntries()) {
            String label = entry.getLabels().isEmpty()
                    ? "default"
                    : "case " + joinExpressions(entry.getLabels());
            CFGNode caseNode = cfg.addNode(label);
            cfg.addEdge(selector, caseNode, label);

            List<Flow> caseFlows = new ArrayList<>();
            caseFlows.add(new Flow(caseNode, null));
            for (Statement caseStatement : entry.getStatements()) {
                caseFlows = buildStatement(caseStatement, caseFlows);
            }
            exits.addAll(caseFlows);
        }
        breakContexts.pop();

        if (statement.getEntries().stream().noneMatch(entry -> entry.getLabels().isEmpty())) {
            exits.add(new Flow(selector, "no match"));
        }
        exits.addAll(breakContext.breakFlows());
        return exits;
    }

    private List<Flow> buildTry(TryStmt statement, List<Flow> incoming) {
        CFGNode tryNode = cfg.addNode("try");
        connectAll(incoming, tryNode);

        Optional<BlockStmt> finallyBlock = statement.getFinallyBlock();
        CFGNode finallyNode = finallyBlock.map(block -> cfg.addNode("finally")).orElse(null);
        if (finallyNode != null) {
            finalizers.push(finallyNode);
        }

        List<Flow> exits = new ArrayList<>(buildBlock(statement.getTryBlock(), List.of(new Flow(tryNode, null))));

        for (CatchClause catchClause : statement.getCatchClauses()) {
            CFGNode catchNode = cfg.addNode("catch (" + catchClause.getParameter() + ")");
            cfg.addEdge(tryNode, catchNode, "exception");
            exits.addAll(buildBlock(catchClause.getBody(), List.of(new Flow(catchNode, null))));
        }

        if (finallyNode != null) {
            finalizers.pop();
            connectAll(exits, finallyNode);
            return buildBlock(finallyBlock.orElseThrow(), List.of(new Flow(finallyNode, null)));
        }

        return exits;
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

    private String joinExpressions(Iterable<?> expressions) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Object expression : expressions) {
            joiner.add(expression.toString());
        }
        return joiner.toString();
    }

    private record Flow(CFGNode node, String label) {
    }

    private record LoopContext(CFGNode continueTarget) {
    }

    private record BreakContext(List<Flow> breakFlows) {
        private BreakContext() {
            this(new ArrayList<>());
        }
    }
}
