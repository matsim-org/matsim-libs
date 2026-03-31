package org.matsim.contrib.dynagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.vehicles.Vehicle;

/**
 * Class to serialize the internal state of a DynAgent.
 */
public record DynAgentMessage(
	Id<Person> id,
	Id<Link> currentLinkId,
	Id<Vehicle> vehicleId,
	MobsimAgent.State state,
	DynLeg dynLeg,
	DynActivity dynActivity
) implements Message {

}
