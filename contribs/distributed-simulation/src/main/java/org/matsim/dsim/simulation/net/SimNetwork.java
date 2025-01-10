package org.matsim.dsim.simulation.net;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.Node;
import org.matsim.dsim.DSimConfigGroup;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class SimNetwork {

	private final Map<Id<Link>, SimLink> links;
	private final Map<Id<Node>, SimNode> nodes;
	private final int part;

	@Inject
	SimNetwork(Network network, DSimConfigGroup config, NetworkPartition networkPartition,
			   ActiveLinks activeLinks, ActiveNodes activeNodes) {

		// collect nodes which belong to this partition
		var localNodes = network.getNodes().values().stream()
			.filter(n -> networkPartition.containsNode(n.getId()))
			.collect(Collectors.toMap(Node::getId, n -> n));

		// convert local node ids into SimNodes
		nodes = localNodes.values().stream()
			.map(node -> new SimNode(node.getId()))
			.collect(Collectors.toMap(SimNode::getId, n -> n));

		// convert network links into SimLinks. Since we must include SplitOutLinks, which belong to other partitions, we iterate over the localNodes
		// and collect the in and out links from those nodes.
		links = localNodes.values().stream()
			.flatMap(n -> Streams.concat(n.getInLinks().values().stream(), n.getOutLinks().values().stream()))
			.distinct()
			.map(link -> {
				var toNode = nodes.get(link.getToNode().getId());
				return SimLink.create(link, toNode, config, network.getEffectiveCellSize(), networkPartition.getIndex(), activeLinks::activate, activeNodes::activate);
			})
			.collect(Collectors.toMap(SimLink::getId, l -> l));

		// now, wire up in and out links to nodes. Iterate over nodes and use the order of in and out links from the original
		// network to ensure consistent order regardless of the applied partitioning
		for (var simNode : nodes.values()) {
			var networkNode = network.getNodes().get(simNode.getId());
			for (var linkId : networkNode.getInLinks().keySet()) {
				var simLink = links.get(linkId);
				simNode.addInLink(simLink);
			}
			for (var linkId : networkNode.getOutLinks().keySet()) {
				var simLink = links.get(linkId);
				simNode.addOutLink(simLink);
			}
		}

		this.part = networkPartition.getIndex();
	}
}
