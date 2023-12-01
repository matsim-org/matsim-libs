package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.function.Function;

/**
 * Provides a method to identify the passenger group id of an agent.
 * @author nkuehnel / MOIA
 */
public interface PassengerGroupIdentifier extends Function<MobsimPassengerAgent, Id<PassengerGroupIdentifier.PassengerGroup>> {

	class PassengerGroup {
		private PassengerGroup(){}
	}

	@Override
	Id<PassengerGroupIdentifier.PassengerGroup> apply(MobsimPassengerAgent agent);

}
