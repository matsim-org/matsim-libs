package org.matsim.api.core.v01.messages;

import it.unimi.dsi.fastutil.ints.IntList;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;

import java.util.Objects;

/**
 * Information about one computing node within a {@link Topology}, such as the node's rank, the number of compute cores available, the number of
 * simulation partitions managed by the node, as well as its hostname.
 */
public class ComputeNode implements Message {

	/**
	 * A instance of a node that is used for single instance simulations.
	 */
	public static final ComputeNode SINGLE_INSTANCE = ComputeNode.builder().parts(IntList.of(0)).rank(0).cores(1).hostname("localhost").build();

	private final int rank;
	private final int cores;
	private final boolean distributed;
	private final IntList parts;
	private final String hostname;

	ComputeNode(NodeBuilder builder) {
		this.rank = builder.rank;
		this.cores = builder.cores;
		this.parts = builder.parts;
		this.hostname = builder.hostname;
		this.distributed = builder.distributed;
	}

	public static NodeBuilder builder() {
		return new NodeBuilder();
	}

	/**
	 * Check if this node is the head node of the topology.
	 */
	public boolean isHeadNode() {
		return this.rank == 0;
	}

	/**
	 * Return whether a certain part is the first within the node.
	 */
	public boolean isFirstPartition(int part) {
		return this.parts.isEmpty() || this.parts.getInt(0) == part;
	}

	/**
	 * Whether there is a distributed simulation running.
	 */
	public boolean isDistributed() {
		return this.distributed;
	}

	/**
	 * The rank of this node. The terminology is borrowed from MPI. The rank starts at 0.
	 */
	public int getRank() {
		return this.rank;
	}

	/**
	 * Number of cores this node should use.
	 */
	public int getCores() {
		return this.cores;
	}

	/**
	 * The partitions this node is responsible for.
	 */
	public IntList getParts() {
		return this.parts;
	}

	public String getHostname() {
		return this.hostname;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof ComputeNode other)) return false;
		if (!other.canEqual(this)) return false;
		if (this.getRank() != other.getRank()) return false;
		if (this.getCores() != other.getCores()) return false;
		final Object this$parts = this.getParts();
		final Object other$parts = other.getParts();
		if (!Objects.equals(this$parts, other$parts)) return false;
		final Object this$hostname = this.getHostname();
		final Object other$hostname = other.getHostname();
		return Objects.equals(this$hostname, other$hostname);
	}

	protected boolean canEqual(final Object other) {
		return other instanceof ComputeNode;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getRank();
		result = result * PRIME + this.getCores();
		final Object $parts = this.getParts();
		result = result * PRIME + ($parts == null ? 43 : $parts.hashCode());
		final Object $hostname = this.getHostname();
		result = result * PRIME + ($hostname == null ? 43 : $hostname.hashCode());
		return result;
	}

	public String toString() {
		return "Node(rank=" + this.getRank() + ", cores=" + this.getCores() + ", parts=" + this.getParts() + ", hostname=" + this.getHostname() + ")";
	}

	public NodeBuilder toBuilder() {
		return new NodeBuilder().rank(this.rank).cores(this.cores).parts(this.parts).hostname(this.hostname);
	}

	public static class NodeBuilder {
		private int rank;
		private int cores;
		private boolean distributed;
		private IntList parts;
		private String hostname;

		NodeBuilder() {
		}

		public NodeBuilder rank(int rank) {
			this.rank = rank;
			return this;
		}

		public NodeBuilder cores(int cores) {
			this.cores = cores;
			return this;
		}

		public NodeBuilder parts(IntList parts) {
			this.parts = parts;
			return this;
		}

		public NodeBuilder hostname(String hostname) {
			this.hostname = hostname;
			return this;
		}

		public NodeBuilder distributed(boolean distributed) {
			this.distributed = distributed;
			return this;
		}

		public ComputeNode build() {
			return new ComputeNode(this);
		}

		public String toString() {
			return "ComputeNode.NodeBuilder(rank=" + this.rank + ", cores=" + this.cores + ", parts=" + this.parts + ", hostname=" + this.hostname + ")";
		}
	}
}
