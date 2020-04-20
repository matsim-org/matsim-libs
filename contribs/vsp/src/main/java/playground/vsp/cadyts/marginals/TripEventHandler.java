package playground.vsp.cadyts.marginals;

import com.google.inject.Inject;

import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

class TripEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, TransitDriverStartsEventHandler {

	private MainModeIdentifier mainModeIdentifier = new TransportPlanningMainModeIdentifier();

	@Inject(optional = true)
	private AgentFilter agentFilter = id -> true; // by default include all agents

	private final Set<Id<Person>> drivers = new HashSet<>();
	private final Map<Id<Person>, List<Trip>> tripToPerson = new HashMap<>();

	Map<Id<Person>, List<Trip>> getTrips() {
		return new HashMap<>(tripToPerson);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {

		drivers.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (StageActivityTypeIdentifier.isStageActivity(event.getActType()) || drivers.contains(event.getPersonId()) || !agentFilter.includeAgent(event.getPersonId()))
			return;

		// maybe handle drt? Drt drivers have their own activities

		if (!tripToPerson.containsKey(event.getPersonId())) {
			List<Trip> trips = new ArrayList<>();
			tripToPerson.put(event.getPersonId(), trips);
		}

		// we have to put in the trip here, since the activity end lets us know whether we have a main activity or a
		// staging acitivity
		Trip trip = new Trip();
		trip.departureTime = event.getTime();
		trip.departureLink = event.getLinkId();
		trip.departureFacility = event.getFacilityId();

		tripToPerson.get(event.getPersonId()).add(trip);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

		// Don't end the trip until we have a real activity
		if (StageActivityTypeIdentifier.isStageActivity(event.getActType()) || !tripToPerson.containsKey(event.getPersonId())) return;

		Trip trip = getCurrentTrip(event.getPersonId());
		trip.arrivalLink = event.getLinkId();
		trip.arrivalTime = event.getTime();
		trip.arrivalFacility = event.getFacilityId();

		try {
			trip.mainMode = mainModeIdentifier.identifyMainMode(trip.legs);
		} catch (Exception e) {
			// the default main mode identifier can't handle non-network-walk only
			trip.mainMode = TransportMode.non_network_walk;
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (!tripToPerson.containsKey(event.getPersonId())) return;

		Trip trip = getCurrentTrip(event.getPersonId());
		Leg leg = trip.legs.get(trip.legs.size() - 1);
		leg.setTravelTime(event.getTime() - leg.getDepartureTime().seconds());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (!tripToPerson.containsKey(event.getPersonId())) return;

		// a new leg is started
		Leg leg = PopulationUtils.createLeg(event.getLegMode());
		leg.setDepartureTime(event.getTime());
		leg.setMode(event.getLegMode());
		Trip trip = getCurrentTrip(event.getPersonId());
		trip.legs.add(leg);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {

		tripToPerson.remove(event.getPersonId());
	}

	@Override
	public void reset(final int iteration) {
		tripToPerson.clear();
	}

	private Trip getCurrentTrip(Id<Person> personId) {

		List<Trip> trips = tripToPerson.get(personId);
		return trips.get(trips.size() - 1);
	}

	static class Trip {
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

		public List<Leg> getLegs() {
			return new ArrayList<>(legs);
		}
	}
}
