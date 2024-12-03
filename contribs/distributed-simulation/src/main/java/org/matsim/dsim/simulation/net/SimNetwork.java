package org.matsim.dsim.simulation.net;

import com.google.common.collect.Streams;
import com.google.inject.Inject;
import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class SimNetwork {

	private final Map<Id<Link>, SimLink> links;
	private final Map<Id<Node>, SimNode> nodes;
	private final int part;

	@Inject
	SimNetwork(Network network, Config config, NetworkPartition networkPartition,
			   ActiveLinks activeLinks, ActiveNodes activeNodes) {

		// collect node ids, which belong to this partition
		var localNodes = network.getNodes().values().stream()
			.filter(n -> networkPartition.containsNode(n.getId()))
			.collect(Collectors.toMap(Node::getId, n -> n));

		// collect linkids, which we need to include into this partition of the network. This means local links, split in and split out links.
		// this is why we are using the in and out links of the network nodes instead of using the network partition
		var linkIds = localNodes.values().stream()
			.flatMap(n -> Streams.concat(n.getInLinks().values().stream(), n.getOutLinks().values().stream()))
			.map(Link::getId)
			.collect(Collectors.toSet());

		// convert local node ids into SimNodes
		nodes = localNodes.values().stream()
			.map(node -> new SimNode(node.getId()))
			.collect(Collectors.toMap(SimNode::getId, n -> n));

		// convert local link ids into SimLinks and set the SimLinks as in and out links on the sim nodes.
		links = new HashMap<>();
		for (var linkId : linkIds) {
			var link = network.getLinks().get(linkId);
			var fromNode = nodes.get(link.getFromNode().getId());
			var toNode = nodes.get(link.getToNode().getId());
			var simLink = SimLink.create(
				link, toNode,
				config.qsim(), network.getEffectiveCellSize(),
				networkPartition.getIndex(), activeLinks::activate, activeNodes::activate
			);
			if (fromNode != null) {
				fromNode.addOutLink(simLink);
			}
			if (toNode != null) {
				toNode.addInLink(simLink);
			}
			links.put(linkId, simLink);
		}
		this.part = networkPartition.getIndex();
	}
}
