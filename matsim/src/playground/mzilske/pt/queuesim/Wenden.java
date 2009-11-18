package playground.mzilske.pt.queuesim;

import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class Wenden implements UmlaufStueckI {

	private NetworkRouteWRefs route;
	
	public Wenden(NetworkRouteWRefs route) {
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

	public NetworkRouteWRefs getCarRoute() {
		return route;
	}

	public boolean isFahrt() {
		return false;
	}

}
