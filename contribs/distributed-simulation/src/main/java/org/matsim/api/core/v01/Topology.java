package org.matsim.api.core.v01;

import lombok.Builder;
import lombok.Getter;
import org.matsim.api.core.v01.messages.Node;

import java.util.List;

@Getter
@Builder
public class Topology implements Message {

    private final int totalPartitions;

    private final List<Node> nodes;

    public int getNodesCount() {
        return nodes.size();
    }

    public Node getNode(int index) {
        return nodes.get(index);
    }

}
