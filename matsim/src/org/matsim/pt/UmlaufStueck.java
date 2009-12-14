package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class UmlaufStueck implements UmlaufStueckI {

	private TransitLine line;
	private TransitRoute route;
	private Departure departure;
	
	public UmlaufStueck(TransitLine line, TransitRoute route,
			Departure departure) {
		this.line = line;
		this.route = route;
		this.departure = departure;
	}

	public TransitLine getLine() {
		return line;
	}

	public TransitRoute getRoute() {
		return route;
	}

	public Departure getDeparture() {
		return departure;
	}

	public NetworkRouteWRefs getCarRoute() {
		return route.getRoute();
	}

	public boolean isFahrt() {
		return true;
	}
	

}
