package com.indudhara.cfg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CFG {
    private final String methodName;
    private final List<CFGNode> nodes = new ArrayList<>();
    private final List<CFGEdge> edges = new ArrayList<>();

    public CFG(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public CFGNode addNode(String label) {
        CFGNode node = new CFGNode("n" + (nodes.size() + 1), label);
        nodes.add(node);
        return node;
    }

    public void addEdge(CFGNode from, CFGNode to) {
        addEdge(from, to, null);
    }

    public void addEdge(CFGNode from, CFGNode to, String label) {
        edges.add(new CFGEdge(from, to, label));
    }

    public List<CFGNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<CFGEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
}
