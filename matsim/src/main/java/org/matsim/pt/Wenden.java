package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class Wenden implements UmlaufStueckI {

	private NetworkRoute route;
	
	public Wenden(NetworkRoute route) {
		this.route = route;
	}

	public Departure getDeparture() {
		return null;
	}

	public TransitLine getLine() {
		return null;
	}

	public TransitRoute getRoute() {
		return null;
	}

	public NetworkRoute getCarRoute() {
		return route;
	}

	public boolean isFahrt() {
		return false;
	}

}
