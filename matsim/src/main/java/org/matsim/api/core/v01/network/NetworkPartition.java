package org.matsim.api.core.v01.network;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;

import java.util.Set;

import static org.matsim.api.core.v01.network.NetworkPartitioning.ATTRIBUTE;

/**
 * Data class describing a single network partition.
 */
public final class NetworkPartition {

	/**
	 * A partition that contains all links and nodes.
	 */
	public static final NetworkPartition SINGLE_INSTANCE = new NetworkPartition();

	private final Set<Id<Link>> links;
	private final IntSet neighbors;
	private final Set<Id<Node>> nodes;

	private final int partition;

	NetworkPartition() {
		links = null;
		nodes = null;
		neighbors = IntSet.of();
		partition = 0;
	}

	NetworkPartition(int partition) {
		this.partition = partition;
		this.links = new IdSet<>(Link.class);
		this.nodes = new IdSet<>(Node.class);
		this.neighbors = new IntOpenHashSet();
	}

	void addLink(Link link) {
		links.add(link.getId());
	}

	void addNode(Node node) {
		nodes.add(node.getId());

		node.getInLinks().values().stream()
			.mapToInt(l -> (int) l.getFromNode().getAttributes().getAttribute(ATTRIBUTE))
			.filter(p -> p != partition)
			.forEach(neighbors::add);

		node.getOutLinks().values().stream()
			.mapToInt(l -> (int) l.getToNode().getAttributes().getAttribute(ATTRIBUTE))
			.filter(p -> p != partition)
			.forEach(neighbors::add);
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
	 * Return whether any of the nodes of a link is contained on this partition.
	 */
	public boolean containsNodesOfLink(Link link) {
		return containsNode(link.getFromNode().getId()) || containsNode(link.getToNode().getId());
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

	/**
	 * Return neighbors of this partition.
	 */
	public IntSet getNeighbors() {
		return neighbors;
	}

	static boolean isSplit(Link link) {
		var fromRank = (int) link.getFromNode().getAttributes().getAttribute(ATTRIBUTE);
		var toRank = (int) link.getToNode().getAttributes().getAttribute(ATTRIBUTE);
		return fromRank != toRank;
	}


}
