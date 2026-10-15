package org.matsim.core.mobsim.dsim;

/**
 * Callback for components that need to know about every agent inserted into the simulation, including agents which never produce any events.
 * Agents arriving from another partition are reported through {@link NotifyAgentPartitionTransfer} instead.
 */
public interface NotifyAgentInsertion {

	/**
	 * Called once for each agent created by an agent source, right before the agent's initial state is arranged on its starting partition.
	 */
	void onAgentInserted(DistributedMobsimAgent agent);
}
