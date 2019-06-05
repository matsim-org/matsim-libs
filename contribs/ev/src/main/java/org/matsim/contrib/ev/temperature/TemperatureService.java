package org.matsim.contrib.ev.temperature;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface TemperatureService {

	double getCurrentTemperature(Id<Link> linkId);

	//TODO remove this method;
	// adapt AuxDischargingHandler to be able to provide link
	// (or maybe separateAuxConsumption is only in DVRP??? then the consumption could be simulated inside DynActivities???)
	double getCurrentTemperature();
}
