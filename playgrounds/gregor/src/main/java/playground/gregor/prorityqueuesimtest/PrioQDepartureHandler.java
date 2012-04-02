package playground.gregor.prorityqueuesimtest;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

public class PrioQDepartureHandler implements DepartureHandler {

	private final PrioQEngine engine;

	/**
	 * @param sim2d
	 */
	public PrioQDepartureHandler(PrioQEngine engine) {
		this.engine = engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.ptproject.qsim.interfaces.DepartureHandler#handleDeparture
	 * (double, org.matsim.core.mobsim.framework.PersonAgent,
	 * org.matsim.api.core.v01.Id, org.matsim.api.core.v01.population.Leg)
	 */
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent instanceof MobsimDriverAgent && agent.getMode().equals("walkprioq")) {
			//TODO agents can not depart directly since their actual departure time might be later than now (because of the
			//sub-second time res. Instead we trap the agents in "limbo" until their time is up. 
			handleAgent2DDeparture((MobsimDriverAgent) agent);
			return true;
		}
		return false;
	}


	private void handleAgent2DDeparture(MobsimDriverAgent agent) {
		this.engine.agentDepart(agent);
	}
}
