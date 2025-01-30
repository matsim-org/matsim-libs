package org.matsim.dsim;

import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;

import java.util.List;

public final class LocalContext implements SimulationContext {

	/**
	 * Local context with a single partition.
	 */
	public final static LocalContext INSTANCE = new LocalContext(
		Topology.builder()
			.totalPartitions(1)
			.nodes(List.of(SimulationNode.SINGLE_INSTANCE))
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
	public SimulationNode getNode() {
		return topology.getNode(0);
	}
}
