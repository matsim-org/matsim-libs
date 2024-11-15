package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

/**
 * Class to represent a vehicle as message object.
 */
public record QVehicleMessage(
	double linkEnterTime,
	double earliestLinkExitTime,
	Id<Link> currentLinkId,
	Vehicle vehicle,
	int passengerCapacity
) implements Message {
}
