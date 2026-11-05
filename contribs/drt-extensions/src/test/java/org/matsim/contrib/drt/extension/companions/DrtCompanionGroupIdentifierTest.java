package org.matsim.contrib.drt.extension.companions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.passenger.PassengerGroupIdentifier;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.population.PopulationUtils;

/**
 * Prebooked legs are submitted while the agent is still in an earlier activity, so the group has to be read from the
 * booked leg and not from the agent's current plan element.
 */
public class DrtCompanionGroupIdentifierTest {

	@Test
	void groupIsTakenFromTheBookedLeg() {
		var leg = PopulationUtils.createLeg("drt");
		var group = Id.create("group", PassengerGroupIdentifier.PassengerGroup.class);
		DrtCompanionUtils.setPassengerGroupIdentifier(leg, group);

		// the agent is not on this leg (e.g. still in its first activity)
		var agent = mock(MobsimPassengerAgent.class);

		assertThat(new DrtCompanionGroupIdentifier().getGroupId(agent, leg)).contains(group);
	}

	@Test
	void legWithoutGroup() {
		var leg = PopulationUtils.createLeg("drt");
		assertThat(new DrtCompanionGroupIdentifier().getGroupId(mock(MobsimPassengerAgent.class), leg)).isEmpty();
	}
}
