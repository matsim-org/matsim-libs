package org.matsim.api.core.v01.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;

import java.util.Set;

/**
 * Data class describing a single network partition.
 */
public final class NetworkPartition {

	/**
	 * A partition that contains all links and nodes.
	 */
	public static final NetworkPartition SINGLE_INSTANCE = new NetworkPartition();

	final Set<Id<Link>> links;
	final Set<Id<Node>> nodes;

	private final int partition;

	NetworkPartition() {
		links = null;
		nodes = null;
		partition = 0;
	}

	NetworkPartition(int partition) {
		this.partition = partition;
		this.links = new IdSet<>(Link.class);
		this.nodes = new IdSet<>(Node.class);
	}

	/**
	 * The partition index, starting at 0.
	 */
	public int getIndex() {
		return partition;
	}

	/**
	 * Check whether the partition contains the given link.
	 */
	public boolean containsLink(Id<Link> linkId) {
		if (links == null) {
			return true;
		}

		// The first partition is technically responsible for null links
		if (linkId == null) {
			return partition == 0;
		}

		return links.contains(linkId);
	}

	/**
	 * Check whether the partition contains the given node.
	 */
	public boolean containsNode(Id<Node> nodeId) {
		if (nodes == null) {
			return true;
		}

		if (nodeId == null) {
			return partition == 0;
		}

		return nodes.contains(nodeId);
	}

}
