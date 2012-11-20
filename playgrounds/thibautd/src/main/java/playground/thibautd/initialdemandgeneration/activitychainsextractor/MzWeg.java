package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

class MzWeg implements Identifiable {
	/**
	 * reproduces all the possible answers of the MZ
	 */
	public enum Purpose {
		work,
		commercialActivity,
		educ,
		leisure,
		shop,
		transitTransfer,
		useService,
		servePassengerRide,
		accompany,
		unknown}; 

	// /////////////////////////////////////////////////////////////////////////
	// attributes
	private final Id personId;
	private final Id wegId;
	private final double departureTime;
	private final double arrivalTime;
	private final double distance;
	private final double duration;
	private final MzAdress startAdress;
	private final MzAdress endAdress;
	private final Purpose purpose;
	private final String mode;

	// /////////////////////////////////////////////////////////////////////////
	// other fields
	private final Map<Id, MzEtappe> etappen = new HashMap<Id, MzEtappe>();

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MzWeg(
			final Id personId,
			final Id wegId,
			final double departureTime,
			final double arrivalTime,
			final double distance,
			final double duration,
			final MzAdress startAdress,
			final MzAdress endAdress,
			final Purpose purpose,
			final String mode) {
		this.personId = personId;
		this.wegId = wegId;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
		this.distance = distance;
		this.duration = duration;
		this.startAdress = startAdress;
		this.endAdress = endAdress;
		this.purpose = purpose;
		this.mode = mode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Id getId() {
		return wegId;
	}

	public Id getPersonId() {
		return personId;
	}

	public MzAdress getDepartureAdress() {
		return startAdress;
	}

	public MzAdress getArrivalAdress() {
		return endAdress;
	}

	public Purpose getPurpose() {
		return purpose;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public double getDuration() {
		return duration;
	}

	public Leg getLeg() {
		LegImpl leg = new LegImpl( getMainMode() );

		leg.setDepartureTime( departureTime );
		// TODO: check consistency tt / arrival time
		leg.setArrivalTime( arrivalTime );
		leg.setTravelTime( duration );

		NetworkRoute route = new LinkNetworkRouteImpl(null, null);
		leg.setRoute(route);
		route.setDistance(distance);
		route.setTravelTime(leg.getTravelTime());

		return leg;
	}

	// ////////////////////// helpers for the getters //////////////////////////
	private String getMainMode() {
		if ( mode != null ) {
			return mode;
		}

		double longestDistance = Double.NEGATIVE_INFINITY;
		String mode = null;

		// main mode is the mode of the longest etap
		for (MzEtappe etappe : etappen.values()) {
			if (etappe.getDistance() > longestDistance) {
				longestDistance = etappe.getDistance();
				mode = etappe.getMode();
			}
		}

		return mode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// creation methods
	// /////////////////////////////////////////////////////////////////////////
	public void addEtappe(final MzEtappe etappe) {
		if (etappen.put( etappe.getId() , etappe ) != null) {
			throw new RuntimeException( "same etappe created twice" );
		}	
	}
}
