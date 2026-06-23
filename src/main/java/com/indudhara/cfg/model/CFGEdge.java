package com.indudhara.cfg.model;

public class CFGEdge {
    private final CFGNode from;
    private final CFGNode to;
    private final String label;

    public CFGEdge(CFGNode from, CFGNode to, String label) {
        this.from = from;
        this.to = to;
        this.label = label;
    }

    public CFGNode getFrom() {
        return from;
    }

    public CFGNode getTo() {
        return to;
    }

    public String getLabel() {
        return label;
    }
}
