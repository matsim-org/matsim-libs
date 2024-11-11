package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * Class to represent a basic plan agent as message object.
 */
public record BasicPlanAgentMessage(
	Plan plan,
	int currentPlanElementIndex,
	double activityEndTime,
	MobsimAgent.State state,
	Id<Link> currentLinkId,
	int currentLinkIndex
) implements Message {

}
