package org.matsim.api.core.v01;

import org.matsim.api.core.v01.messages.SimulationNode;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * Describes the computing topology of the simulation.
 */
public class Topology implements Message, Iterable<SimulationNode> {

	private final int totalPartitions;

	@Nonnull
	private final List<SimulationNode> nodes;

	Topology(int totalPartitions, @Nonnull List<SimulationNode> nodes) {
		this.totalPartitions = totalPartitions;
		this.nodes = nodes;
	}

	public static TopologyBuilder builder() {
		return new TopologyBuilder();
	}

	public int getNodesCount() {
		return nodes.size();
	}

	public SimulationNode getNode(int index) {
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
	public Iterator<SimulationNode> iterator() {
		return nodes.iterator();
	}

	public static class TopologyBuilder {
		private int totalPartitions;
		private List<SimulationNode> nodes;

		TopologyBuilder() {
		}

		public TopologyBuilder totalPartitions(int totalPartitions) {
			this.totalPartitions = totalPartitions;
			return this;
		}

		public TopologyBuilder nodes(List<SimulationNode> nodes) {
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
