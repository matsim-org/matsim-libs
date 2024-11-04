package org.matsim.api.core.v01.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.messages.SimulationNode;

/**
 * Data class describing how the network is partitioned.
 */
public final class NetworkPartitioning {

	public final static NetworkPartitioning SINGLE_INSTANCE = new NetworkPartitioning();
	public final static String ATTRIBUTE = "partition";

	private final Int2ObjectMap<IdSet<Link>> byNode = new Int2ObjectOpenHashMap<>();
	private final SimulationNode node;

	/**
	 * Empty partition.
	 */
	public NetworkPartitioning() {
		node = null;
	}

	/**
	 * Constructor to read partition information from a {@link SimulationNode}.
	 */
	public NetworkPartitioning(SimulationNode node, Network network) {
		this.node = node;
		for (Link link : network.getLinks().values()) {
			Integer partition = (Integer) link.getAttributes().getAttribute(ATTRIBUTE);
			if (partition != null) {
				byNode.computeIfAbsent(partition, k -> new IdSet<>(Link.class)).add(link.getId());
			}
		}
	}

	/**
	 * Check whether link id is on the current node.
	 */
	public boolean isLinkOnCurrentNode(Id<Link> linkId) {
		if (node == null || byNode.isEmpty()) {
			return true;
		}

		return byNode.get(node.getRank()).contains(linkId);
	}

}
