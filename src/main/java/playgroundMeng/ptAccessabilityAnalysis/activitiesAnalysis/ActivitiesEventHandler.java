package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;


public class ActivitiesEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler{

	@Inject
	private Network network;
	private List<Trip> trips = new LinkedList<Trip>();
	private Map<Id<Person>, Trip> personId2Trip = new HashedMap();
	private Map<Id<Person>, List<Trip>> personId2Trips = new HashedMap();
	private LinkedList<ActivityImp> activities = new LinkedList<ActivityImp>();
	
	
	public ActivitiesEventHandler (Network network) {
		this.network = network;
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getActType().endsWith("interaction")) {
			Trip trip = new Trip();
			trip.setPersonId(event.getPersonId());
			Link link = network.getLinks().get(event.getLinkId());
			ActivityImp activityImp = new ActivityImp(event.getActType(),link.getCoord(), event.getTime());
			trip.setActivityEndImp(activityImp);
			personId2Trip.put(event.getPersonId(), trip);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().endsWith("interaction")) {
			Link link = network.getLinks().get(event.getLinkId());
			personId2Trip.get(event.getPersonId()).setActivityStartImp(new ActivityImp(event.getActType(),link.getCoord(), event.getTime()));
			if(this.personId2Trips.containsKey(event.getPersonId())) {
				this.personId2Trips.get(event.getPersonId()).add(personId2Trip.get(event.getPersonId()));
			} else {
				this.personId2Trips.put(event.getPersonId(), new LinkedList<Trip>());
				this.personId2Trips.get(event.getPersonId()).add(personId2Trip.get(event.getPersonId()));
				this.personId2Trip.put(event.getPersonId(), null);
			}
	
		}
		
	}
	
	public LinkedList<ActivityImp> getActivities() {
		return activities;
	}
	
	public Map<Id<Person>, List<Trip>> getPersonId2Trips() {
		return personId2Trips;
	}

}
