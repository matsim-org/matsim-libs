package org.matsim.core.network.turnRestrictions;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A network cleaner that ensures strongly connected components in the presence of turn restrictions.
 * Uses the routing graph expansion of {@link TurnRestrictionsContext} to identify disconnected elements using
 * the original {@link org.matsim.core.network.algorithms.NetworkCleaner}.
 * Only keeps turn restrictions that do not disconnect the network.
 *
 * @author nkuehnel / MOIA
 */
public class TurnRestrictionsNetworkCleaner implements NetworkRunnable {


    @Override
    public void run(Network network) {
        TurnRestrictionsContext turnRestrictions = TurnRestrictionsContext.build(network);
        colorNetwork(network, turnRestrictions);
        new NetworkCleaner().run(network);
        collapseNetwork(network, turnRestrictions);
        reapplyRestrictions(network, turnRestrictions);
    }


    public void colorNetwork(Network network, TurnRestrictionsContext turnRestrictions) {

        for (Link link : network.getLinks().values()) {
            if (turnRestrictions.replacedLinks.containsKey(link.getId())) {
                network.removeLink(link.getId());
            }
        }

        for (TurnRestrictionsContext.ColoredNode coloredNode : turnRestrictions.coloredNodes) {
            Node coloredCopy = NetworkUtils.createNode(getColoredNodeId(coloredNode), coloredNode.node().getCoord());
            network.addNode(coloredCopy);
            coloredCopy.getAttributes().putAttribute("colored", coloredNode.node().getId());
        }

        for (TurnRestrictionsContext.ColoredLink coloredLink : turnRestrictions.coloredLinks) {
            Node fromNode;
            if (coloredLink.fromColoredNode != null) {
                fromNode = network.getNodes().get(getColoredNodeId(coloredLink.fromColoredNode));
            } else {
                fromNode = network.getNodes().get(coloredLink.fromNode.getId());
            }

            Node toNode;
            if (coloredLink.toColoredNode != null) {
                toNode = network.getNodes().get(getColoredNodeId(coloredLink.toColoredNode));
            } else {
                toNode = network.getNodes().get(coloredLink.toNode.getId());
            }

            Verify.verifyNotNull(fromNode);
            Verify.verifyNotNull(toNode);

            Id<Link> linkId = getColoredLinkId(coloredLink);
            Link link = NetworkUtils.createLink(
                    linkId,
                    fromNode,
                    toNode,
                    network,
                    coloredLink.link.getLength(),
                    coloredLink.link.getFreespeed(),
                    coloredLink.link.getCapacity(),
                    coloredLink.link.getNumberOfLanes()
            );
            link.getAttributes().putAttribute("colored", coloredLink.link.getId());
            network.addLink(link);
        }
    }


    private void collapseNetwork(Network network, TurnRestrictionsContext turnRestrictions) {

        for (TurnRestrictionsContext.ColoredNode coloredNode : turnRestrictions.coloredNodes) {
            Id<Node> nodeId = getColoredNodeId(coloredNode);
            // colored node copy still present, but not the original
            if (network.getNodes().containsKey(nodeId) && !network.getNodes().containsKey(coloredNode.node().getId())) {
                Node nodeCopy = NetworkUtils.createNode(coloredNode.node().getId(), coloredNode.node().getCoord());
                network.addNode(nodeCopy);
                for (TurnRestrictionsContext.ColoredLink coloredLink : coloredNode.inLinks()) {
                    if (network.getLinks().containsKey(coloredLink.link.getId())) {
                        nodeCopy.addInLink(network.getLinks().get(coloredLink.link.getId()));
                    }
                }
                for (TurnRestrictionsContext.ColoredLink coloredLink : coloredNode.outLinks()) {
                    if (network.getLinks().containsKey(coloredLink.link.getId())) {
                        nodeCopy.addOutLink(network.getLinks().get(coloredLink.link.getId()));
                    }
                }
            }
        }

        for (Map.Entry<Id<Link>, TurnRestrictionsContext.ColoredLink> idColoredLinkEntry : turnRestrictions.replacedLinks.entrySet()) {
            TurnRestrictionsContext.ColoredLink coloredLink = idColoredLinkEntry.getValue();
            checkRealLinkExistence(network, coloredLink);
        }

        for (Map.Entry<Id<Link>, List<TurnRestrictionsContext.ColoredLink>> idColoredLinkEntry : turnRestrictions.coloredLinksPerLinkMap.entrySet()) {
            for (TurnRestrictionsContext.ColoredLink coloredLink : idColoredLinkEntry.getValue()) {
                checkRealLinkExistence(network, coloredLink);
            }
        }

        //remove colored nodes / links
        for (TurnRestrictionsContext.ColoredNode coloredNode : turnRestrictions.coloredNodes) {
            Id<Node> nodeId = Id.createNodeId(coloredNode.node().getId() + "_" + coloredNode.index());
            network.removeNode(nodeId);
        }

        for (TurnRestrictionsContext.ColoredLink coloredLink : turnRestrictions.coloredLinks) {
            Id<Link> linkId = Id.createLinkId(coloredLink.link.getId() + "_" + coloredLink.index);
            turnRestrictions.network.removeLink(linkId);
        }
    }

    private void checkRealLinkExistence(Network network, TurnRestrictionsContext.ColoredLink coloredLink) {
        Id<Link> copyId = getColoredLinkId(coloredLink);
        if (network.getLinks().containsKey(copyId) && !network.getLinks().containsKey(coloredLink.link.getId())) {
            Link link = coloredLink.link;
            Link linkCopy = NetworkUtils.createLink(
                    link.getId(),
                    link.getFromNode(),
                    link.getToNode(),
                    network,
                    link.getLength(),
                    link.getFreespeed(),
                    link.getCapacity(),
                    link.getNumberOfLanes()
            );
            // copy all attributes except initial turn restrictions
            NetworkUtils.copyAttributesExceptDisallowedNextLinks(coloredLink.link, linkCopy);
            network.addLink(linkCopy);
        }
    }

    private void reapplyRestrictions(Network network, TurnRestrictionsContext turnRestrictions) {
        for (Map.Entry<Id<Link>, TurnRestrictionsContext.ColoredLink> idColoredLinkEntry : turnRestrictions.replacedLinks.entrySet()) {
            Link link = network.getLinks().get(idColoredLinkEntry.getValue().link.getId());
            List<Id<Link>> currentPath = new ArrayList<>();
            if (idColoredLinkEntry.getValue().toNode != null) {
                throw new RuntimeException("Shouldn't happen");
            } else {
                advance(idColoredLinkEntry.getValue(), currentPath, link, network, turnRestrictions);
            }
        }
    }

    private void advance(TurnRestrictionsContext.ColoredLink coloredLink, List<Id<Link>> currentPath,
                         Link replacedStartLink, Network network, TurnRestrictionsContext turnRestrictions) {

        if (!network.getLinks().containsKey(coloredLink.link.getId())) {
            // link sequence is not part of the network anymore and doesn't need to be explored.
            return;
        }

        if (coloredLink.toColoredNode != null) {
            Node node = coloredLink.toColoredNode.node();

            // set of reachable links from the original node
            // use id, as the link object may have been deleted and re-inserted as a copy during the process
            Set<Id<Link>> unrestrictedReachableLinks = new HashSet<>(node.getOutLinks().values()
                    .stream()
                    .map(Identifiable::getId)
                    .filter(link -> network.getLinks().containsKey(link))
                    .collect(Collectors.toSet()));


            List<TurnRestrictionsContext.ColoredLink> toAdvance = new ArrayList<>();
            for (TurnRestrictionsContext.ColoredLink outLink : coloredLink.toColoredNode.outLinks()) {
                // remove from the set all links that are also reachable from within the colored subgraph
                unrestrictedReachableLinks.remove(outLink.link.getId());
                if (outLink.toColoredNode != null) {
                    toAdvance.add(outLink);
                }
            }

            // any remaining link is _not_ reachable from within the colored subgraph
            // -> there must be a turn restriction from the current start link across the path
            if (!unrestrictedReachableLinks.isEmpty()) {
                DisallowedNextLinks disallowedNextLinks = NetworkUtils.getOrCreateDisallowedNextLinks(replacedStartLink);
                for (Id<Link> unrestrictedReachableLink : unrestrictedReachableLinks) {
                    List<Id<Link>> path = new ArrayList<>(currentPath);
                    path.add(unrestrictedReachableLink);
                    disallowedNextLinks.addDisallowedLinkSequence("car", path);
                }
            }

            for (TurnRestrictionsContext.ColoredLink link : toAdvance) {
                List<Id<Link>> nextPath = new ArrayList<>(currentPath);
                nextPath.add(link.link.getId());
                if (turnRestrictions.replacedLinks.containsKey(link.link.getId())) {
                    return;
                }
                advance(link, nextPath, replacedStartLink, network, turnRestrictions);
            }
        }
    }

    private Id<Node> getColoredNodeId(TurnRestrictionsContext.ColoredNode coloredNode) {
        return Id.createNodeId(coloredNode.node().getId() + "_" + coloredNode.index());
    }

    private Id<Link> getColoredLinkId(TurnRestrictionsContext.ColoredLink coloredLink) {
        return Id.createLinkId(coloredLink.link.getId() + "_" + coloredLink.index);
    }
}
