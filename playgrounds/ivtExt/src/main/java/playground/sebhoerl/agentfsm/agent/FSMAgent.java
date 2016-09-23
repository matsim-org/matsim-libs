package playground.sebhoerl.agentfsm.agent;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;

public interface FSMAgent extends MobsimAgent {
	void startLeg(Leg leg);
}
