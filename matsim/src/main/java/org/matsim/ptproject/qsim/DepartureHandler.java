package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PersonDriverAgent;

public interface DepartureHandler {
	
	public void handleDeparture(double now, PersonDriverAgent agent, Id linkId, Leg leg);

}
