package playground.vsp.cadyts.marginals;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

class TripEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, TransitDriverStartsEventHandler {

	@Inject
	private Network network;

	@Inject
	private StageActivityTypes stageActivityTypes;

	@Inject
	private MainModeIdentifier mainModeIdentifier;

	@Inject(optional = true)
	private AgentFilter agentFilter = id -> true; // by default include all agents

	private final Set<Id<Person>> drivers = new HashSet<>();
	private final Map<Id<Person>, List<BeelineTrip>> tripToPerson = new HashMap<>();

	Map<Id<Person>, List<BeelineTrip>> getTrips() {
		return tripToPerson;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {

		drivers.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (stageActivityTypes.isStageActivity(event.getActType()) || drivers.contains(event.getPersonId()) || !agentFilter.includeAgent(event.getPersonId()))
			return;

		// maybe handle drt? Drt drivers have their own activities

		if (!tripToPerson.containsKey(event.getPersonId())) {
			List<BeelineTrip> trips = new ArrayList<>();
			tripToPerson.put(event.getPersonId(), trips);
		}

		// we have to put in the trip here, since the activity end lets us know whether we have a main activity or a
		// staging acitivity
		BeelineTrip trip = new BeelineTrip();
		trip.departureTime = event.getTime();
		trip.departureLink = event.getLinkId();
		trip.departureFacility = event.getFacilityId();

		tripToPerson.get(event.getPersonId()).add(trip);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		if (!tripToPerson.containsKey(event.getPersonId())) return;

		BeelineTrip trip = getCurrentTrip(event.getPersonId());
		trip.arrivalLink = event.getLinkId();
		trip.arrivalTime = event.getTime();
		trip.arrivalFacility = event.getFacilityId();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (!tripToPerson.containsKey(event.getPersonId())) return;

		BeelineTrip trip = getCurrentTrip(event.getPersonId());
		Leg leg = trip.legs.get(trip.legs.size() - 1);
		leg.setTravelTime(event.getTime() - leg.getDepartureTime());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (!tripToPerson.containsKey(event.getPersonId())) return;

		// a new leg is started
		Leg leg = PopulationUtils.createLeg(event.getLegMode());
		leg.setDepartureTime(event.getTime());
		BeelineTrip trip = getCurrentTrip(event.getPersonId());
		trip.legs.add(leg);

		// only investigate a mode if it is not the access/egress leg. If this is used in a multi-modal scenario,
		// the last leg, will define the mainMode of the whole trip, if the current default main mode identifier is used
		if (!leg.getMode().equals(TransportMode.non_network_walk)) {
			trip.mainMode = mainModeIdentifier.identifyMainMode(trip.legs);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {

		tripToPerson.remove(event.getPersonId());
	}

	@Override
	public void reset(final int iteration) {
		tripToPerson.clear();
	}

	private BeelineTrip getCurrentTrip(Id<Person> personId) {

		List<BeelineTrip> trips = tripToPerson.get(personId);
		return trips.get(trips.size() - 1);
	}

	static class BeelineTrip {
		private Id<Link> departureLink;
		private Id<Link> arrivalLink;
		private double departureTime;
		private double arrivalTime;
		private Id<ActivityFacility> departureFacility;
		private Id<ActivityFacility> arrivalFacility;
		private String mainMode = TransportMode.other;

		private List<Leg> legs = new ArrayList<>();

		Id<Link> getDepartureLink() {
			return departureLink;
		}

		Id<Link> getArrivalLink() {
			return arrivalLink;
		}

		public double getDepartureTime() {
			return departureTime;
		}

		public double getArrivalTime() {
			return arrivalTime;
		}

		Id<ActivityFacility> getDepartureFacility() {
			return departureFacility;
		}

		Id<ActivityFacility> getArrivalFacility() {
			return arrivalFacility;
		}

		public String getMainMode() {
			return mainMode;
		}
	}
}
