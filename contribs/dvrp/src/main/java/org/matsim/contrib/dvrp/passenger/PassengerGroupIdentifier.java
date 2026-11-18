package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.Optional;

/**
 * Provides a method to identify the passenger group id of an agent.
 * @author nkuehnel / MOIA
 */
public interface PassengerGroupIdentifier {

	class PassengerGroup {
		private PassengerGroup(){}
	}

	/**
	 * Group of the agent for its current leg, used when the agent departs.
	 */
	Optional<Id<PassengerGroup>> getGroupId(MobsimPassengerAgent agent);

	/**
	 * Group of the agent for the given leg, which need not be the current plan element: prebooked legs are submitted
	 * while the agent is still in an earlier activity. Implementations that store the group on the leg (e.g. as a leg
	 * attribute) should override this method; the default delegates to {@link #getGroupId(MobsimPassengerAgent)},
	 * which is correct for implementations that do not depend on the leg (e.g. a person attribute).
	 */
	default Optional<Id<PassengerGroup>> getGroupId(MobsimPassengerAgent agent, Leg leg) {
		return getGroupId(agent);
	}

}
