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

	private final IdSet<Link> byNode;
	private final Int2ObjectMap<NetworkPartition> partitions = new Int2ObjectOpenHashMap<>();

	private final SimulationNode node;

	/**
	 * Empty partition.
	 */
	public NetworkPartitioning() {
		node = null;
		byNode = null;
	}

	/**
	 * Constructor to read partition information from a {@link SimulationNode}.
	 */
	public NetworkPartitioning(SimulationNode node, Network network) {
		this.node = node;
		this.byNode = new IdSet<>(Link.class);
		for (Link link : network.getLinks().values()) {
			Integer partition = (Integer) link.getAttributes().getAttribute(ATTRIBUTE);

			if (partition != null) {
				partitions.computeIfAbsent(partition, k -> new NetworkPartition(partition)).links.add(link.getId());

				if (node.getParts().contains((int) partition))
					byNode.add(link.getId());
			}
		}

		for (Node n : network.getNodes().values()) {
			Integer partition = (Integer) n.getAttributes().getAttribute(ATTRIBUTE);
			if (partition != null) {
				partitions.computeIfAbsent(partition, k -> new NetworkPartition(partition)).nodes.add(n.getId());
			}
		}
	}

	/**
	 * Retrieve the network information for a single partition.
	 */
	public NetworkPartition getPartition(int partition) {
		if (partitions.isEmpty())
			return NetworkPartition.SINGLE_INSTANCE;

		return partitions.get(partition);
	}

	/**
	 * Check whether link id is on the current node.
	 */
	public boolean isLinkOnCurrentNode(Id<Link> linkId) {
		if (node == null || byNode == null) {
			return true;
		}

		return byNode.contains(linkId);
	}

}
