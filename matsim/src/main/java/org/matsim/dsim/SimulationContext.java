package org.matsim.dsim;

import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;

/**
 * Class to hold information and communication needed for distributed simulation.
 * This class is separated so that a normal simulation can run without importing the distributed context.
 * Thus, it does not to enable java preview features.
 * Once preview features are mainstream, the context class might be merged backed into one.
 */
public sealed interface SimulationContext permits DistributedContext, LocalContext {

	Topology getTopology();

	SimulationNode getNode();

	/**
	 * Whether the simulation is distributed across multiple nodes.
	 */
	default boolean isDistributed() {
		return getTopology().isDistributed();
	}
}
