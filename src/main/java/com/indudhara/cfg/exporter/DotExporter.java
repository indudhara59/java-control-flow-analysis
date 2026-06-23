package com.indudhara.cfg.exporter;

import com.indudhara.cfg.model.CFG;
import com.indudhara.cfg.model.CFGEdge;
import com.indudhara.cfg.model.CFGNode;

public class DotExporter {
    public String toDot(CFG cfg) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph \"").append(escape(cfg.getMethodName())).append("\" {\n");

        for (CFGNode node : cfg.getNodes()) {
            dot.append("  ")
                    .append(node.getId())
                    .append(" [label=\"")
                    .append(escape(node.getLabel()))
                    .append("\"];\n");
        }

        for (CFGEdge edge : cfg.getEdges()) {
            dot.append("  ")
                    .append(edge.getFrom().getId())
                    .append(" -> ")
                    .append(edge.getTo().getId());

            if (edge.getLabel() != null && !edge.getLabel().isBlank()) {
                dot.append(" [label=\"").append(escape(edge.getLabel())).append("\"]");
            }

            dot.append(";\n");
        }

        dot.append("}\n");
        return dot.toString();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
