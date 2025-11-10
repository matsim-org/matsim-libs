package org.matsim.api.core.v01;

import org.matsim.api.core.v01.messages.ComputeNode;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * Describes the computing topology of the simulation. The topology is defined by the following attributes:
 * - The number of total partitions into which the simulation domain is divided
 * - A list of {@link ComputeNode}s which are responsible for the execution of one or more parts of the simulation
 */
public class Topology implements Message, Iterable<ComputeNode> {

	private final int totalPartitions;

	@Nonnull
	private final List<ComputeNode> computeNodes;

	Topology(int totalPartitions, @Nonnull List<ComputeNode> computeNodes) {
		this.totalPartitions = totalPartitions;
		this.computeNodes = computeNodes;
	}

	public static TopologyBuilder builder() {
		return new TopologyBuilder();
	}

	public int getNodesCount() {
		return computeNodes.size();
	}

	public ComputeNode getNode(int index) {
		return computeNodes.get(index);
	}

	/**
	 * Return whether the simulation runs in distributed setup.
	 */
	public boolean isDistributed() {
		return computeNodes.size() > 1;
	}

	@Override
	@Nonnull
	public Iterator<ComputeNode> iterator() {
		return computeNodes.iterator();
	}

	public int getTotalPartitions() {
		return totalPartitions;
	}

	public static class TopologyBuilder {
		private int totalPartitions;
		private List<ComputeNode> computeNodes;

		TopologyBuilder() {
		}

		public TopologyBuilder totalPartitions(int totalPartitions) {
			this.totalPartitions = totalPartitions;
			return this;
		}

		public TopologyBuilder computeNodes(List<ComputeNode> computeNodes) {
			this.computeNodes = computeNodes;
			return this;
		}

		public Topology build() {
			return new Topology(this.totalPartitions, this.computeNodes);
		}

		public String toString() {
			return "Topology.TopologyBuilder(totalPartitions=" + this.totalPartitions + ", nodes=" + this.computeNodes + ")";
		}
	}
}
