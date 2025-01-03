package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Interface for creating agents and vehicles for the mobsim. This interface
 */
public interface DistributedAgentSource {

	/**
	 * Create agents and vehicles for the given network partition.
	 *
	 * @param partition for which to create agents and vehicles
	 * @param mobsim    to insert agents and vehicles into the simulation
	 */
	void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim);

	/**
	 * Return classes of agent that this source will create.
	 */
	Set<Class<? extends DistributedMobsimAgent>> getAgentClasses();

	/**
	 * Construct an agent from a message.
	 *
	 * @return null if this source is not responsible for the given message.
	 */
	@Nullable
	DistributedMobsimAgent agentFromMessage(Class<? extends DistributedMobsimAgent> type, Message message);

	/**
	 * The vehicle classes this provider will support.
	 */
	default Set<Class<? extends DistributedMobsimVehicle>> getVehicleClasses() {
		return Set.of();
	}

	/**
	 * Construct a vehicle from a message.
	 *
	 * @return null if this source is not responsible for the given message.
	 */
	@Nullable
	default DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {
		return null;
	}

	/**
	 * Called when a vehicle has been created from a message and populated with its occupants.
	 */
	default void onDriverCreated(DriverAgent driver, DistributedMobsimVehicle vehicle) {
	}

}
