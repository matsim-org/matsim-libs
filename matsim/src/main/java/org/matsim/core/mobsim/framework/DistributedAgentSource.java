package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;

/**
 * Interface for creating agents and vehicles for the mobsim. This interface
 */
public interface DistributedAgentSource {

	/**
	 * Create agents and vehicles for the given network partition.
	 *
	 * @param partition for which to create agents and vehicles
	 * @param mobsim to insert agents and vehicles into the simulation
	 */
	void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim);

	/**
	 * Return the message class of the agents produced by this source.
	 */
	Class<? extends Message> getMessageClass();

	/**
	 * Construct an agent from a message.
	 */
	DistributedMobsimAgent fromMessage(Message message);

}
