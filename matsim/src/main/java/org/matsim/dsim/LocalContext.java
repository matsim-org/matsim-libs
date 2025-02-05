package org.matsim.dsim;

import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;

import java.util.List;

public final class LocalContext implements SimulationContext {

	/**
	 * Local context with a single partition.
	 */
	public final static LocalContext INSTANCE = new LocalContext(
		Topology.builder()
			.totalPartitions(1)
			.computeNodes(List.of(ComputeNode.SINGLE_INSTANCE))
			.build()
	);

	private final Topology topology;

	private LocalContext(Topology topology) {
		this.topology = topology;
	}

	@Override
	public Topology getTopology() {
		return topology;
	}

	@Override
	public ComputeNode getComputeNode() {
		return topology.getNode(0);
	}
}
