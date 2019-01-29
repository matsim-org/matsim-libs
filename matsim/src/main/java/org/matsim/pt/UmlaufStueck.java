package org.matsim.pt;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

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

	@Override
	public TransitLine getLine() {
		return line;
	}

	@Override
	public TransitRoute getRoute() {
		return route;
	}

	@Override
	public Departure getDeparture() {
		return departure;
	}

	@Override
	public NetworkRoute getCarRoute() {
		return route.getRoute();
	}

	@Override
	public boolean isFahrt() {
		return true;
	}
	

}
