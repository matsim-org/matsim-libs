package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record SimNode(Id<Node> id, List<SimLink> inLinks, Map<Id<Link>, SimLink> outLinks) {

    double calculateAvailableCapacity() {
        return inLinks.stream()
                .filter(SimLink::isOffering)
                .mapToDouble(SimLink::getMaxFlowCapacity)
                .sum();
    }

    boolean[] createExhaustedLinks() {
        var result = new boolean[inLinks.size()];
        for (var i = 0; i < inLinks.size(); i++) {
            if (!inLinks.get(i).isOffering()) {
                result[i] = true;
            }
        }
        return result;
    }

    boolean isActiveInNextTimestep(double now) {
        return inLinks.stream().anyMatch(SimLink::isOffering);
    }

    static SimNode create(Node node, Map<Id<Link>, SimLink> links) {

        var inLinks = node.getInLinks().keySet().stream()
                .map(links::get)
                .toList();
        var outLinks = node.getOutLinks().keySet().stream()
                .map(links::get)
                .collect(Collectors.toMap(SimLink::getId, link -> link));
        return new SimNode(node.getId(), inLinks, outLinks);
    }
}
