package org.matsim.contrib.ev.temperature;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface TemperatureService {
	double getCurrentTemperature(Id<Link> linkId);
}
