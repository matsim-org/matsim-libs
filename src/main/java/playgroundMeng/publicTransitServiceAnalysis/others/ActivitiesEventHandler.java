package playgroundMeng.publicTransitServiceAnalysis.others;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.ActivityImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;

public class ActivitiesEventHandler
		implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler {

	private Network network;
	private List<Trip> trips = new LinkedList<Trip>();
	private Map<Id<Person>, Trip> personId2Trip = new HashedMap();
	private Map<Id<Person>, List<Trip>> personId2Trips = new HashedMap();
	private LinkedList<ActivityImp> activities = new LinkedList<ActivityImp>();

	@Inject
	public ActivitiesEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!event.getActType().endsWith("interaction")) {
			Trip trip = new Trip();
			trip.setPersonId(event.getPersonId());
			Link link = network.getLinks().get(event.getLinkId());

			ActivityImp activityImp = new ActivityImp(event.getActType(), link.getCoord(), event.getTime());
			activityImp.setLink(link);
			trip.setActivityEndImp(activityImp);
			personId2Trip.put(event.getPersonId(), trip);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().endsWith("interaction")) {
			Link link = network.getLinks().get(event.getLinkId());
			ActivityImp activityImp = new ActivityImp(event.getActType(), link.getCoord(), event.getTime());
			activityImp.setLink(link);
			personId2Trip.get(event.getPersonId()).setActivityStartImp(activityImp);
			if (this.personId2Trips.containsKey(event.getPersonId())) {
				this.personId2Trips.get(event.getPersonId()).add(personId2Trip.get(event.getPersonId()));
			} else {
				this.personId2Trips.put(event.getPersonId(), new LinkedList<Trip>());
				this.personId2Trips.get(event.getPersonId()).add(personId2Trip.get(event.getPersonId()));
			}
			this.trips.add(personId2Trip.get(event.getPersonId()));
			this.personId2Trip.put(event.getPersonId(), null);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personId2Trip.containsKey(event.getPersonId())) {
			this.personId2Trip.get(event.getPersonId()).getModes().add(event.getLegMode());
		}

	}

	public LinkedList<ActivityImp> getActivities() {
		return activities;
	}

	public Map<Id<Person>, List<Trip>> getPersonId2Trips() {
		return personId2Trips;
	}

	public List<Trip> getTrips() {
//		return getSubTripsByRadom(trips, 10000);
		return this.trips;
	}

	public List<Trip> getSubTripsByRadom(List<Trip> list, int count) {
		List backList = null;
		backList = new LinkedList<Trip>();
		Random random = new Random();
		int backSum = 0;
		if (list.size() >= count) {
			backSum = count;
		} else {
			backSum = list.size();
		}
		for (int i = 0; i < backSum; i++) {
//			随机数的范围为0-list.size()-1
			int target = random.nextInt(list.size());
			backList.add(list.get(target));
			list.remove(target);
		}
		return backList;
	}

}
