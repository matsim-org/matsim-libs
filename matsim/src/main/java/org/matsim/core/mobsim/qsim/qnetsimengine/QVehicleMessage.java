package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

/**
 * Class to represent a vehicle as message object.
 */
public record QVehicleMessage(
	Id<Vehicle> id,
	Message driver,
	Collection<Message> passengers,
	double earliestExitTime
) implements Message {
}
