package org.matsim.core.router.turnRestrictions;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public final class ColoredLink {
    private final int index;
    private final Link link;
    private final TurnRestrictionsContext.ColoredNode fromColoredNode;
    private final Node fromNode;
    private TurnRestrictionsContext.ColoredNode toColoredNode;
    private Node toNode;

    public ColoredLink(
            int index,
            Link link,
            TurnRestrictionsContext.ColoredNode fromColoredNode,
            Node fromNode,
            TurnRestrictionsContext.ColoredNode toColoredNode,
            Node toNode
    ) {
        this.index = index;
        this.link = link;
        this.fromColoredNode = fromColoredNode;
        this.fromNode = fromNode;
        this.toColoredNode = toColoredNode;
        this.toNode = toNode;
    }

    public int getIndex() {
        return index;
    }

    public Link getLink() {
        return link;
    }

    public TurnRestrictionsContext.ColoredNode getFromColoredNode() {
        return fromColoredNode;
    }

    public Node getFromNode() {
        return fromNode;
    }

    public TurnRestrictionsContext.ColoredNode getToColoredNode() {
        return toColoredNode;
    }

    public Node getToNode() {
        return toNode;
    }

    //package private only
    void setToColoredNode(TurnRestrictionsContext.ColoredNode toColoredNode) {
        this.toColoredNode = toColoredNode;
    }

    //package private only
    void setToNode(Node node) {
        this.toNode = node;
    }
}
