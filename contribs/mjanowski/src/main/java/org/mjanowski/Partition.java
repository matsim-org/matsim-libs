package org.mjanowski;

import org.matsim.api.core.v01.network.Node;

import java.util.LinkedList;
import java.util.List;

public class Partition {

    private final int maxSize;
    private final List<Node> nodes;

    public Partition(int maxSize) {
        this.maxSize = maxSize;
        nodes = new LinkedList<>();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
