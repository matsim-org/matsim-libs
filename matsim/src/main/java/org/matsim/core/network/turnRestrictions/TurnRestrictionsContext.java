package org.matsim.core.network.turnRestrictions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.*;

/**
 * Turn restriction context that expands networks with turn restrictions with colored subgraphs.
 *
 *  * @author mrieser / Simunto
 */
public final class TurnRestrictionsContext {

    private static final Logger LOG = LogManager.getLogger(TurnRestrictionsContext.class);

    private int nodeCount;
    private int linkCount;

    public final Network network;

    public Map<Id<Link>, ColoredLink> replacedLinks = new HashMap<>();
    public List<ColoredNode> coloredNodes = new ArrayList<>();
    public List<ColoredLink> coloredLinks = new ArrayList<>();
    public Map<Id<Link>, List<ColoredLink>> coloredLinksPerLinkMap = new HashMap<>();

    private TurnRestrictionsContext(Network network) {
        this.network = network;
        this.nodeCount = Id.getNumberOfIds(Node.class);
        this.linkCount = Id.getNumberOfIds(Link.class);
    }

    public static final class ColoredLink {
        public final int index;
        public final Link link;
        public final ColoredNode fromColoredNode;
        public final Node fromNode;
        public ColoredNode toColoredNode;
        public Node toNode;

        private ColoredLink(
                int index,
                Link link,
                ColoredNode fromColoredNode,
                Node fromNode,
                ColoredNode toColoredNode,
                Node toNode
        ) {
            this.index = index;
            this.link = link;
            this.fromColoredNode = fromColoredNode;
            this.fromNode = fromNode;
            this.toColoredNode = toColoredNode;
            this.toNode = toNode;
        }
    }

    public record ColoredNode(
            int index,
            Node node,
            List<ColoredLink> outLinks,
            List<ColoredLink> inLinks
    ) {

        @Override
        public int index() {
            return index;
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public int hashCode() {
            return index;
        }
    }

    /**
     * Builds a graph from a network considering all links without caring about
     * allowedModes but considering {@link DisallowedNextLinks} (aka turn
     * restrictions) of the given {@code mode}.
     *
     * If mode == null and network contains turn restrictions, turn
     * restrictions of all modes are merged. This could result in a network which
     * is more restrictive than necessary for routing.
     *
     * @param network
     * @param mode
     * @return
     */
    public static TurnRestrictionsContext build(Network network, @Nullable String mode) {
        /*
         * The implementation follows the algorithm developed by
         * Marcel Rieser (Simunto) and Hannes Rewald (Volkswagen Group)
         * in October 2023 during the MATSim Code Sprint week in Berlin,
         * and documented at https://github.com/matsim-org/matsim-code-examples/wiki/turn-restrictions.
         *
         * TL;DR:
         * Main idea of algorithm: for each link with turn-restrictions, create a sub-graph of the network
         * containing all links required to model all allowed paths, but exclude the last link of turn restrictions'
         * link sequence to enforce the "disallow" along that route.
         *
         *
         * Implementation details:
         * - The easiest solution would be to make a copy of the original network, then start modifying it
         *   according to the algorithm above (e.g. add and delete links and nodes), and then to convert the
         *   resulting network into a graph. This would require substantial amount of memory for duplicating
         *   the complete network, and might pose problems as we will have multiple links with the same id.
         * - Given the assumption that turn restrictions apply only to a small amount of the full network,
         *   we keep the original network intact. Instead, we keep all modifications in separate data-structures
         *   so they can be used to create the routing-graph.
         * - If the network is already filtered for a specific mode, it might be that links referenced
         *   in a turn restriction are missing. The implementation must be able to deal with such cases,
         *   prevent NullPointerExceptions.
         * - As turn restrictions are mode-specific, the algorithm needs to know for which mode the
         *   turn restriction need to be considered.
         */
        boolean didWarnAboutMergingOfDisallowedNextLinks = false;

        TurnRestrictionsContext context = new TurnRestrictionsContext(network);

        for (Link startingLink : network.getLinks().values()) {

            // get disallowed next links for statingLink
            DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(startingLink);
            if (disallowedNextLinks == null) {
                continue;
            }
            Collection<List<Id<Link>>> turnRestrictions;
            if (mode != null) {
                turnRestrictions = disallowedNextLinks.getDisallowedLinkSequences(mode);
            } else {
                turnRestrictions = disallowedNextLinks.getMergedDisallowedLinkSequences();
                if (!didWarnAboutMergingOfDisallowedNextLinks) {
                    LOG.warn("No mode was specified, merging available DisallowedNextLinks from all modes.");
                    LOG.warn("This could result in a graph that is more restrictive than meant to be for routing.");
                    didWarnAboutMergingOfDisallowedNextLinks = true;
                }
            }
            if (turnRestrictions == null || turnRestrictions.isEmpty()) {
                continue;
            }

            // steps 1 to 5:
            ColoredLink coloredStartingLink = context.applyTurnRestriction(context, turnRestrictions, startingLink);

            // step 6: turn restrictions have to be applied separately to existing colored links as well.
            // see if there are already colored link copies available for this starting link
            List<ColoredLink> coloredLinks = context.coloredLinksPerLinkMap.get(startingLink.getId());
            if (coloredLinks != null) {
                for (ColoredLink coloredLink : coloredLinks) {
                    // optimization: re-point toNode instead of re-applying full turn restrictions
                    if (coloredLink.toColoredNode == null) {
                        coloredLink.toColoredNode = coloredStartingLink.toColoredNode;
                        coloredLink.toNode = null;
                    } else {
                        context.applyTurnRestriction(context, turnRestrictions, coloredLink);
                    }
                }
            }
        }

        return context;
    }

    private ColoredLink applyTurnRestriction(TurnRestrictionsContext context, Collection<List<Id<Link>>> restrictions, Link startingLink) {
        return this.applyTurnRestriction(context, restrictions, startingLink, null);
    }

    private void applyTurnRestriction(TurnRestrictionsContext context, Collection<List<Id<Link>>> restrictions, ColoredLink startingLink) {
        this.applyTurnRestriction(context, restrictions, null, startingLink);
    }

    private ColoredLink applyTurnRestriction(TurnRestrictionsContext context, Collection<List<Id<Link>>> restrictions, Link startingLink, ColoredLink coloredStartingLink) {
        Set<Node> affectedNodes = new HashSet<>();
        Set<ColoredNode> affectedColoredNodes = new HashSet<>();
        Set<Link> affectedLinks = new HashSet<>();
        Set<ColoredLink> affectedColoredLinks = new HashSet<>();
        Set<Id<Link>> endLinkIds = new HashSet<>();

        // step 1 and 2: collect end-links, affected-links and affected-nodes
        for (List<Id<Link>> restriction : restrictions) {

            Link currentLink;
            ColoredLink currentColoredLink;
            Node currentNode = startingLink == null ? null : startingLink.getToNode();
            // due to the optimization in step 6, every colored starting link leads to a colored to-node
            ColoredNode currentColoredNode = coloredStartingLink == null ? null : coloredStartingLink.toColoredNode;

            // walk along the restricted path, collect affectedLinks, affectedNodes and endLink
            for (Id<Link> linkId : restriction) {
                if (currentNode != null) {
                    // handle regular node
                    affectedNodes.add(currentNode);
                    currentLink = null;
                    currentColoredLink = null;
                    for (Link outLink : currentNode.getOutLinks().values()) {
                        if (outLink.getId() == linkId) {
                            currentColoredLink = context.replacedLinks.get(linkId);
                            if (currentColoredLink == null) {
                                currentLink = outLink;
                            }
                            break;
                        }
                    }

                    if (currentLink != null) {
                        affectedLinks.add(currentLink);
                        currentNode = currentLink.getToNode();
                        currentColoredNode = null;
                    }
                    if (currentColoredLink != null) {
                        affectedColoredLinks.add(currentColoredLink);
                        currentNode = currentColoredLink.toNode;
                        currentColoredNode = currentColoredLink.toColoredNode;
                    }
                    if (currentLink == null && currentColoredLink == null) {
                        // link of restriction is no longer part of the network, maybe we are in a sub-graph
                        break;
                    }
                } else if (currentColoredNode != null) {
                    // handle colored node
                    affectedColoredNodes.add(currentColoredNode);
                    currentLink = null;
                    currentColoredLink = null;
                    for (ColoredLink outLink : currentColoredNode.outLinks) {
                        if (outLink.link.getId() == linkId) {
                            currentColoredLink = outLink;
                            break;
                        }
                    }
                    if (currentColoredLink != null) {
                        affectedColoredLinks.add(currentColoredLink);
                        currentNode = currentColoredLink.toNode;
                        currentColoredNode = currentColoredLink.toColoredNode;
                    }
                    if (currentColoredLink == null) {
                        // link of restriction is no longer part of the network, maybe we are in a sub-graph
                        break;
                    }
                }
            }
            endLinkIds.add(restriction.get(restriction.size() - 1));
        }

        // step 3: create colored copies of nodes
        Map<Id<Node>, ColoredNode> newlyColoredNodes = new HashMap<>();
        for (Node affectedNode : affectedNodes) {
            int nodeIndex = context.nodeCount;
            context.nodeCount++;
            ColoredNode newlyColoredNode = new ColoredNode(nodeIndex, affectedNode, new ArrayList<>(), new ArrayList<>());
            newlyColoredNodes.put(affectedNode.getId(), newlyColoredNode);
            context.coloredNodes.add(newlyColoredNode);
        }
        for (ColoredNode affectedColoredNode : affectedColoredNodes) {
            int nodeIndex = context.nodeCount;
            context.nodeCount++;
            ColoredNode newlyColoredNode = new ColoredNode(nodeIndex, affectedColoredNode.node, new ArrayList<>(), new ArrayList<>());
            newlyColoredNodes.put(affectedColoredNode.node.getId(), newlyColoredNode);
            context.coloredNodes.add(newlyColoredNode);
        }

        // step 4: create colored copies of links
        for (Node affectedNode : affectedNodes) {
            for (Link outLink : affectedNode.getOutLinks().values()) {
                if (endLinkIds.contains(outLink.getId())) {
                    continue;
                }
                ColoredLink replacedOutLink = context.replacedLinks.get(outLink.getId());
                int linkIndex = context.linkCount;
                context.linkCount++;
                ColoredLink newlyColoredLink;
                ColoredNode fromNode = newlyColoredNodes.get(outLink.getFromNode().getId());
                if (affectedLinks.contains(outLink) || (replacedOutLink != null && affectedColoredLinks.contains(replacedOutLink))) {
                    ColoredNode toNode = newlyColoredNodes.get(outLink.getToNode().getId());
                    newlyColoredLink = new ColoredLink(linkIndex, outLink, fromNode, null, toNode, null);
                } else {
                    if(replacedOutLink != null) {
                        // we are on a link exiting the current colored subgraph (to node is real) but the link is also
                        // a replaced one, which means we should directly enter into the next turn restriction by entering
                        // the respective subgraph through the existing colored node. hrewald & nkuehnel, May 2025
                        ColoredNode toNode = replacedOutLink.toColoredNode;
                        newlyColoredLink = new ColoredLink(linkIndex, outLink, fromNode, null, toNode, null);
                    } else {
                        Node toNode = outLink.getToNode();
                        newlyColoredLink = new ColoredLink(linkIndex, outLink, fromNode, null, null, toNode);
                    }
                }
                fromNode.outLinks.add(newlyColoredLink);
                context.coloredLinks.add(newlyColoredLink);
                context.coloredLinksPerLinkMap.computeIfAbsent(outLink.getId(), id -> new ArrayList<>(3)).add(newlyColoredLink);
            }
        }
        for (ColoredNode affectedNode : affectedColoredNodes) {
            for (ColoredLink outLink : affectedNode.outLinks) {
                if (endLinkIds.contains(outLink.link.getId())) {
                    continue;
                }
                int linkIndex = context.linkCount;
                context.linkCount++;
                ColoredLink newlyColoredLink;
                ColoredNode fromNode = newlyColoredNodes.get(outLink.link.getFromNode().getId());
                if (affectedColoredLinks.contains(outLink)) {
                    ColoredNode toNode = newlyColoredNodes.get(outLink.link.getToNode().getId());
                    newlyColoredLink = new ColoredLink(linkIndex, outLink.link, fromNode, null, toNode, null);
                } else {
                    newlyColoredLink = new ColoredLink(linkIndex, outLink.link, fromNode, null, outLink.toColoredNode, outLink.toNode);
                }
                fromNode.outLinks.add(newlyColoredLink);
                context.coloredLinks.add(newlyColoredLink);
                context.coloredLinksPerLinkMap.computeIfAbsent(outLink.link.getId(), id -> new ArrayList<>(3)).add(newlyColoredLink);
            }
        }

        // step 5: replace starting link
        if (startingLink != null) {
            ColoredNode toNode = newlyColoredNodes.get(startingLink.getToNode().getId());
            int linkIndex = startingLink.getId().index(); // re-use the index
            ColoredLink newlyColoredStartingLink = new ColoredLink(linkIndex, startingLink, null, startingLink.getFromNode(), toNode, null);
            context.coloredLinks.add(newlyColoredStartingLink);
            context.replacedLinks.put(startingLink.getId(), newlyColoredStartingLink);

            return newlyColoredStartingLink;
        }
        if (coloredStartingLink != null) {
            // don't really replace the colored started link, but re-point it to the newly colored node
            coloredStartingLink.toColoredNode = newlyColoredNodes.get(coloredStartingLink.link.getToNode().getId());
            return null;

        }
        throw new IllegalArgumentException("either startingLink or coloredStartingLink must be set");
    }


    public int getNodeCount() {
        return nodeCount;
    }

    public int getLinkCount() {
        return linkCount;
    }
}
