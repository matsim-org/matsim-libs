package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * Interface for vehicles in the distributed mobsim. This vehicle can be sent to other nodes in the simulation.
 */
public interface DistributedMobsimVehicle extends QVehicle {

	/**
	 * Convert the vehicle to a message that can be sent to other nodes. The message can be any java object, but should be as lightweight as possible.
	 */
	Message toMessage();

}
