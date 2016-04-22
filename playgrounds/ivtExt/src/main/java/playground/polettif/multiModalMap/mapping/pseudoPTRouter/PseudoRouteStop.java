package playground.polettif.multiModalMap.mapping.pseudoPTRouter;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * A RouteStop used in the pseudoGraph.
 *
 * Link Candidates are made for each stop facility. Since one
 * stop facility might be accessed twice in the same transitRoute,
 * unique Link Candidates for each transitRouteStop are needed. This
 * is achieved via this class.
 *
 * @author polettif
 */
public class PseudoRouteStop {


	public final String id;
	private final String name;
	private final LinkCandidate linkCandidate;

	private double departureOffset;
	private double arrivalOffset;
	private boolean awaitDepartureTime;

	public PseudoRouteStop(int order, TransitRouteStop routeStop, LinkCandidate linkCandidate) {
		this.id = Integer.toString(order) + routeStop.getStopFacility().getId() + linkCandidate.getLink().getId();
		this.name = routeStop.getStopFacility().getName() + " (" + linkCandidate.getLink().getId() + ")";
		this.linkCandidate = linkCandidate;

		this.departureOffset = routeStop.getDepartureOffset();
		this.arrivalOffset = routeStop.getArrivalOffset();
		this.awaitDepartureTime = routeStop.isAwaitDepartureTime();
	}

	public PseudoRouteStop(String id) {
		if(id.equals("SOURCE")) {
			this.id = "SOURCE";
		} else {
			this.id = "DESTINATION";
		}
		this.name = id;
		this.linkCandidate = null;

		this.departureOffset = 0.0;
		this.arrivalOffset = 0.0;
		this.awaitDepartureTime = false;
	}

	public LinkCandidate getLinkCandidate() {
		return linkCandidate;
	}

	public TransitStopFacility getParentStopFacility() {
		return linkCandidate.getParentStop();
	}

	public double getDepartureOffset() {
		return departureOffset;
	}

	public double getArrivalOffset() {
		return arrivalOffset;
	}

	public boolean isAwaitDepartureTime() {
		return awaitDepartureTime;
	}

	@Override
	public String toString() {
		return name;
	}

	public TransitStopFacility getChildStopFacility() {
		return linkCandidate.getChildStop();
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		PseudoRouteStop other = (PseudoRouteStop) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
