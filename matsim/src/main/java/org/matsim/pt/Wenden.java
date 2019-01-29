package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class Wenden implements UmlaufStueckI {

	private NetworkRoute route;
	
	public Wenden(NetworkRoute route) {
		this.route = route;
	}

	@Override
	public Departure getDeparture() {
		return null;
	}

	@Override
	public TransitLine getLine() {
		return null;
	}

	@Override
	public TransitRoute getRoute() {
		return null;
	}

	@Override
	public NetworkRoute getCarRoute() {
		return route;
	}

	@Override
	public boolean isFahrt() {
		return false;
	}

}
