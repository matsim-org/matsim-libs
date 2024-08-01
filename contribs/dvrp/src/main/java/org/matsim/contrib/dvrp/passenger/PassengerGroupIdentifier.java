package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
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

	Optional<Id<PassengerGroup>> getGroupId(MobsimPassengerAgent agent);

}
