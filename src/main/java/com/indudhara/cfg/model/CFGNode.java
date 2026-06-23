package com.indudhara.cfg.model;

public class CFGNode {
    private final String id;
    private final String label;

    public CFGNode(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
