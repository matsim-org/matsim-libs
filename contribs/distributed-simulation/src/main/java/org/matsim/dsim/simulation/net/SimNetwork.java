package org.matsim.dsim.simulation.net;

import com.google.common.collect.Streams;
import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Map;
import java.util.stream.Collectors;

import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

@Getter
public class SimNetwork {

    private final Map<Id<Link>, SimLink> links;
    private final Map<Id<Node>, SimNode> nodes;

    SimNetwork(Network network, Config config, SimLink.OnLeaveQueue vehicleEndsRouteHandler, int part) {

        var localNodes = network.getNodes().values().stream()
                .filter(n -> isOnPartition(n, part))
                .collect(Collectors.toMap(Node::getId, n -> n));

        var linkIds = localNodes.values().stream()
                .flatMap(n -> Streams.concat(n.getInLinks().values().stream(), n.getOutLinks().values().stream()))
                .map(Link::getId)
                .collect(Collectors.toSet());

        links = linkIds.stream()
                .map(id -> network.getLinks().get(id))
                .map(link ->
					SimLink.create(link, vehicleEndsRouteHandler, config.qsim(), network.getEffectiveCellSize(), part))
                .collect(Collectors.toMap(SimLink::getId, simLink -> simLink));
        nodes = localNodes.values().stream()
                .map(node -> Tuple.of(node.getId(), SimNode.create(node, links)))
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
    }

    private static boolean isOnPartition(Attributable element, int part) {
        return (int) element.getAttributes().getAttribute(PARTITION_ATTR_KEY) == part;
    }
}
