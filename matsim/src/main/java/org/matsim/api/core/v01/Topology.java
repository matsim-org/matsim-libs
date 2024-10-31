package org.matsim.api.core.v01;

import org.matsim.api.core.v01.messages.Node;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * Describes the computing topology of the simulation.
 */
public class Topology implements Message, Iterable<Node> {

	private final int totalPartitions;

	@Nonnull
	private final List<Node> nodes;

	Topology(int totalPartitions, @Nonnull List<Node> nodes) {
		this.totalPartitions = totalPartitions;
		this.nodes = nodes;
	}

	public static TopologyBuilder builder() {
		return new TopologyBuilder();
	}

	public int getNodesCount() {
		return nodes.size();
	}

	public Node getNode(int index) {
		return nodes.get(index);
	}

	/**
	 * Return whether the simulation runs in distributed setup.
	 */
	public boolean isDistributed() {
		return nodes.size() > 1;
	}

	public int getTotalPartitions() {
		return this.totalPartitions;
	}

	@Override
	public Iterator<Node> iterator() {
		return nodes.iterator();
	}

	public static class TopologyBuilder {
		private int totalPartitions;
		private List<Node> nodes;

		TopologyBuilder() {
		}

		public TopologyBuilder totalPartitions(int totalPartitions) {
			this.totalPartitions = totalPartitions;
			return this;
		}

		public TopologyBuilder nodes(List<Node> nodes) {
			this.nodes = nodes;
			return this;
		}

		public Topology build() {
			return new Topology(this.totalPartitions, this.nodes);
		}

		public String toString() {
			return "Topology.TopologyBuilder(totalPartitions=" + this.totalPartitions + ", nodes=" + this.nodes + ")";
		}
	}
}
