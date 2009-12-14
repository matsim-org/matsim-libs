package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;

public interface DepartureHandler {
	
	public void handleDeparture(double now, DriverAgent agent, Link link, Leg leg);

}
