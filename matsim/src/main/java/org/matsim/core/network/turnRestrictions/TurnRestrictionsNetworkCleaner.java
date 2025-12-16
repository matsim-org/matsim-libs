package org.matsim.core.network.turnRestrictions;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A network cleaner that ensures strongly connected components in the presence
 * of {@link DisallowedNextLinks} aka turn restrictions.
 * Uses the routing graph expansion of {@link TurnRestrictionsContext} to
 * identify disconnected elements using the original
 * {@link MultimodalNetworkCleaner}.
 * Only keeps turn restrictions that do not disconnect the network.
 *
 * @author nkuehnel / MOIA
 */
public class TurnRestrictionsNetworkCleaner {

    @SuppressWarnings("deprecation")
    public void run(Network network, String mode) {
        TurnRestrictionsContext turnRestrictions = TurnRestrictionsContext.build(network, mode);
        colorNetwork(network, turnRestrictions, mode);
        new MultimodalNetworkCleaner(network).run(Set.of(mode));
        collapseNetwork(network, turnRestrictions, mode);
        reapplyRestrictions(network, turnRestrictions, mode);
    }

    public void colorNetwork(Network network, TurnRestrictionsContext turnRestrictions, String mode) {

        for (Link link : network.getLinks().values()) {
            if (turnRestrictions.replacedLinks.containsKey(link.getId())) {
                if(link.getAllowedModes().contains(mode)) {
                    NetworkUtils.removeAllowedMode(link, mode);
                    DisallowedNextLinks disallowedNextLinks = NetworkUtils.getDisallowedNextLinks(link);
                    Verify.verifyNotNull(disallowedNextLinks);
                    disallowedNextLinks.removeDisallowedLinkSequences(mode);
                    if(disallowedNextLinks.isEmpty()) {
                        NetworkUtils.removeDisallowedNextLinks(link);
                    }
                }
                if(link.getAllowedModes().isEmpty()) {
                    network.removeLink(link.getId());
                }
            }
        }

        for (TurnRestrictionsContext.ColoredNode coloredNode : turnRestrictions.coloredNodes) {
            Node coloredCopy = NetworkUtils.createNode(getColoredNodeId(coloredNode), coloredNode.node().getCoord());
            network.addNode(coloredCopy);
            coloredCopy.getAttributes().putAttribute("colored", coloredNode.node().getId());
        }

        Set<String> modeSingletonSet = Set.of(mode);
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
            link.setAllowedModes(modeSingletonSet);
            link.getAttributes().putAttribute("colored", coloredLink.link.getId());
            network.addLink(link);
        }
    }


    private void collapseNetwork(Network network, TurnRestrictionsContext turnRestrictions, String mode) {

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
            checkRealLinkExistence(network, coloredLink, mode);
        }

        for (Map.Entry<Id<Link>, List<TurnRestrictionsContext.ColoredLink>> idColoredLinkEntry : turnRestrictions.coloredLinksPerLinkMap.entrySet()) {
            for (TurnRestrictionsContext.ColoredLink coloredLink : idColoredLinkEntry.getValue()) {
                checkRealLinkExistence(network, coloredLink, mode);
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

    private void checkRealLinkExistence(Network network, TurnRestrictionsContext.ColoredLink coloredLink, String mode) {
        Id<Link> copyId = getColoredLinkId(coloredLink);
        if (network.getLinks().containsKey(copyId)) {
            if (!network.getLinks().containsKey(coloredLink.link.getId())) {
                Link link = coloredLink.link;
                Link linkCopy = NetworkUtils.createLink(
                        link.getId(),
                        network.getNodes().get(link.getFromNode().getId()),
                        network.getNodes().get(link.getToNode().getId()),
                        network,
                        link.getLength(),
                        link.getFreespeed(),
                        link.getCapacity(),
                        link.getNumberOfLanes()
                );
                // copy all attributes except initial turn restrictions
                NetworkUtils.copyAttributesExceptDisallowedNextLinks(coloredLink.link, linkCopy);

                // copy all other modes
                linkCopy.setAllowedModes(coloredLink.link.getAllowedModes());

                // copy all turn restrictions of all other modes
                DisallowedNextLinks originalDisallowedNextLinks = NetworkUtils.getDisallowedNextLinks(coloredLink.link);
                if (originalDisallowedNextLinks != null) {
                    DisallowedNextLinks disallowedNextLinks = NetworkUtils.getOrCreateDisallowedNextLinks(linkCopy);
                    for (Map.Entry<String, List<List<Id<Link>>>> restriction : originalDisallowedNextLinks.getAsMap().entrySet()) {
                        if (!mode.equals(restriction.getKey())) {
                            for (List<Id<Link>> sequence : restriction.getValue()) {
                                disallowedNextLinks.addDisallowedLinkSequence(restriction.getKey(), sequence);
                            }
                        }
                    }
                    if (disallowedNextLinks.isEmpty()) {
                        NetworkUtils.removeDisallowedNextLinks(linkCopy);
                    }
                }
                network.addLink(linkCopy);
            }

            // make sure that current mode is allowed on the link
            NetworkUtils.addAllowedMode(network.getLinks().get(coloredLink.link.getId()), mode);
        }
    }

    private void reapplyRestrictions(Network network, TurnRestrictionsContext turnRestrictions, String mode) {
        for (Map.Entry<Id<Link>, TurnRestrictionsContext.ColoredLink> idColoredLinkEntry : turnRestrictions.replacedLinks.entrySet()) {
            Link link = network.getLinks().get(idColoredLinkEntry.getValue().link.getId());
            List<Id<Link>> currentPath = new ArrayList<>();
            Set<Id<Link>> visitedLinkIds = new HashSet<>();
            if (idColoredLinkEntry.getValue().toNode != null) {
                throw new RuntimeException("Shouldn't happen");
            } else {
                advance(idColoredLinkEntry.getValue(), currentPath, link, network, turnRestrictions, mode, visitedLinkIds);
            }
        }
    }

    private void advance(TurnRestrictionsContext.ColoredLink coloredLink, List<Id<Link>> currentPath,
            Link replacedStartLink, Network network, TurnRestrictionsContext turnRestrictions, String mode,
            Set<Id<Link>> visitedLinkIds) {

        if (!visitedLinkIds.add(coloredLink.link.getId())) {
            return; // already visited
        }

        if (!network.getLinks().containsKey(coloredLink.link.getId())) {
            // link sequence is not part of the network anymore and doesn't need to be explored.
            return;
        }

        if (coloredLink.toColoredNode != null) {

            // initial node may have been cleaned but re-inserted when colored versions persist.
            // -> retrieve by original id
            Node node = network.getNodes().get(coloredLink.toColoredNode.node().getId());

            // set of reachable links from the original node
            // use id, as the link object may have been deleted and re-inserted as a copy during the process
            Set<Id<Link>> unrestrictedReachableLinks = node.getOutLinks().values()
                    .stream()
                    .map(Identifiable::getId)
                    .filter(link -> network.getLinks().containsKey(link))
                    //only consider links that are reachable by the current mode.
                    .filter(link -> network.getLinks().get(link).getAllowedModes().contains(mode))
                    .collect(Collectors.toSet());


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
                    disallowedNextLinks.addDisallowedLinkSequence(mode, path);
                }
            }

            for (TurnRestrictionsContext.ColoredLink link : toAdvance) {
                List<Id<Link>> nextPath = new ArrayList<>(currentPath);
                nextPath.add(link.link.getId());
                if (turnRestrictions.replacedLinks.containsKey(link.link.getId())) {
                    continue;
                }
                advance(link, nextPath, replacedStartLink, network, turnRestrictions, mode, visitedLinkIds);
            }
        }

        visitedLinkIds.remove(coloredLink.link.getId());
    }

    private Id<Node> getColoredNodeId(TurnRestrictionsContext.ColoredNode coloredNode) {
        return Id.createNodeId(coloredNode.node().getId() + "_" + coloredNode.index());
    }

    private Id<Link> getColoredLinkId(TurnRestrictionsContext.ColoredLink coloredLink) {
        return Id.createLinkId(coloredLink.link.getId() + "_" + coloredLink.index);
    }
}
