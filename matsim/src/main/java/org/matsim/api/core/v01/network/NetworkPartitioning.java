package org.matsim.api.core.v01.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.messages.ComputeNode;

/**
 * Data class describing how the network is partitioned.
 */
public final class NetworkPartitioning {

	public final static NetworkPartitioning SINGLE_INSTANCE = new NetworkPartitioning();
	public final static String ATTRIBUTE = "partition";


	private final IdSet<Link> linksOnNode;
	private final Object2IntMap<Id<Link>> link2partition;

	private final Int2ObjectMap<NetworkPartition> partitions = new Int2ObjectOpenHashMap<>();


	private final ComputeNode computeNode;

	/**
	 * Empty partition.
	 */
	public NetworkPartitioning() {
		computeNode = null;
		linksOnNode = null;
		link2partition = null;
	}

	/**
	 * Constructor to read partition information from a {@link ComputeNode}.
	 */
	public NetworkPartitioning(ComputeNode computeNode, Network network) {
		this.computeNode = computeNode;
		this.linksOnNode = new IdSet<>(Link.class);
		this.link2partition = new Object2IntOpenHashMap<>();
		for (Link link : network.getLinks().values()) {
			Integer partition = (Integer) link.getAttributes().getAttribute(ATTRIBUTE);

			if (partition != null) {
				partitions.computeIfAbsent(partition, k -> new NetworkPartition(partition)).addLink(link);
				link2partition.put(link.getId(), (int) partition);
				if (computeNode.getParts().contains((int) partition))
					linksOnNode.add(link.getId());
			}
		}

		for (Node n : network.getNodes().values()) {
			Integer partition = (Integer) n.getAttributes().getAttribute(ATTRIBUTE);
			if (partition != null) {
				partitions.computeIfAbsent(partition, k -> new NetworkPartition(partition)).addNode(n);
			}
		}
	}

	/**
	 * Retrieve the network information for a single partition.
	 */
	public NetworkPartition getPartition(int partition) {
		if (partitions.isEmpty())
			return NetworkPartition.SINGLE_INSTANCE;

		// Will return an empty partition if not found
		return partitions.computeIfAbsent(partition, k -> new NetworkPartition(partition));
	}

	/**
	 * Return the partition index of a link.
	 */
	public int getPartition(Id<Link> linkId) {
		if (link2partition == null) {
			return 0;
		}

		return link2partition.getInt(linkId);
	}

	/**
	 * Check whether link id is on the current node.
	 */
	public boolean isLinkOnCurrentNode(Id<Link> linkId) {
		if (computeNode == null || linksOnNode == null) {
			return true;
		}

		return linksOnNode.contains(linkId);
	}
}
