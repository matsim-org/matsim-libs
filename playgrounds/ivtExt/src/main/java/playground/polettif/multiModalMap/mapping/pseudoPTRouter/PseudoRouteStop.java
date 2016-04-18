package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Link candidates are made for each stop facility. For routing unique
 * link candidates for each transitRouteStop are needed
 */
public class PseudoRouteStop {

	private final TransitRouteStop routeStop;

	private final LinkCandidate linkCandidate;
	private final String id;

	public PseudoRouteStop(TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		this.routeStop = routeStop;
		this.linkCandidate = linkCandidate;
		this.id = routeStop + ":" + linkCandidate;
	}

	public PseudoRouteStop(String id) {
		this.routeStop = null;
		this.linkCandidate = null;
		this.id = id;
	}

	public LinkCandidate getLinkCandidate() {
		return linkCandidate;
	}

	public TransitStopFacility getParentStopFacility() {
		return routeStop.getStopFacility();
	}

	public double getDepartureOffset() {
		return routeStop.getDepartureOffset();
	}

	public double getArrivalOffset() {
		return routeStop.getArrivalOffset();
	}

	public boolean isAwaitDepartureTime() {
		return routeStop.isAwaitDepartureTime();
	}

	@Override
	public String toString() {
		return id;
	}

	public boolean isDestination() {
		return id.equals("destination");
	}

	public TransitStopFacility getChildStopFacility() {
		return linkCandidate.getChildStop();
	}

	public String getId() {
		return id;
	}
}
