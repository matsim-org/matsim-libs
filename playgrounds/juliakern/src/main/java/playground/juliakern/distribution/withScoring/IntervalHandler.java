package playground.juliakern.distribution.withScoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;

import playground.juliakern.distribution.EmActivity;

public class IntervalHandler implements ActivityStartEventHandler, ActivityEndEventHandler{

	HashMap<Id, ArrayList<ActivityStartEvent>> person2asevent = new HashMap<Id, ArrayList<ActivityStartEvent>>();
	HashMap<Id, ArrayList<ActivityEndEvent>> person2aeevent = new HashMap<Id, ArrayList<ActivityEndEvent>>();
	ArrayList<EmActivity> activities = new ArrayList<EmActivity>();
	
	public ArrayList<EmActivity> getActivities() {
		return this.activities;
	}

	@Override
	public void reset(int iteration) {
		this.person2asevent.clear();// = new HashMap<Id, ArrayList<ActivityStartEvent>>();
		this.person2aeevent.clear(); // = new HashMap<Id, ArrayList<ActivityEndEvent>>();
		this.activities.clear(); // = new ArrayList<EmActivity>();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		ArrayList<ActivityEndEvent> events;
		if(person2aeevent.containsKey(personId)){
			events = person2aeevent.get(personId);
		}else{
			events = new ArrayList<ActivityEndEvent>();
		}
		events.add(event);
		person2aeevent.put(personId, events);		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		ArrayList<ActivityStartEvent> events;
		if(person2asevent.containsKey(personId)){
			events = person2asevent.get(personId);
		}else{
			events = new ArrayList<ActivityStartEvent>();
		}
		events.add(event);
		person2asevent.put(personId, events);
	}

	public void addActivitiesToTimetables(Map<Id<Link>,Integer> link2xbins, Map<Id<Link>,Integer> link2ybins, Double simulationEndTime) {
		this.activities = new ArrayList<EmActivity>();
		// combine act start events with act leave events to em activities
		// without emission values - store person id, time and x,y-cell
		
		
		for(Id personId: person2asevent.keySet()){
			//TODO? sort by time?
			for(ActivityStartEvent ase: person2asevent.get(personId)){
				Double startOfActivity = ase.getTime();				
				if (link2xbins.get(ase.getLinkId())!=null && link2ybins.get(ase.getLinkId())!=null) {
					int xBin = link2xbins.get(ase.getLinkId());
					int yBin = link2ybins.get(ase.getLinkId());
					// find corresponding act end event
					ActivityEndEvent aee = findCorrespondingActivityEndEvent(
							ase, person2aeevent.get(personId));
					Double endOfActivity;
					if (aee == null) {
						endOfActivity = simulationEndTime;
					} else {
						endOfActivity = aee.getTime();
					}
					// create em activity
					EmActivity emact = new EmActivity(startOfActivity,
							endOfActivity, personId, xBin, yBin,
							ase.getActType());
					activities.add(emact);
					// remove act end events from arrays
					// do not remove act start events - iteration
					// TODO sinnvoll? beschleunigt das entsprechend?
					// hinterher auch act start loeschen?
					//person2aeevent.get(personId).remove(aee);
				}				
			}
			if (person2aeevent.get(personId)!=null) {
				// TODO rethink this!
				// first activity might not have a start event but an end event
				// get first activityEndEvent
				Double firstActivityEndTime = Double.MAX_VALUE;
				ActivityEndEvent firstAee = null;
				for (ActivityEndEvent aee : person2aeevent.get(personId)) {
					if (aee.getTime() < firstActivityEndTime) {
						firstAee = aee;
					}
				}
				if (link2xbins.get(firstAee.getLinkId())!=null && link2ybins.get(firstAee.getLinkId())!=null) {
					int firstXBin = link2xbins.get(firstAee.getLinkId());
					int firstYBin = link2ybins.get(firstAee.getLinkId());
					EmActivity emFirst = new EmActivity(0.0,
							firstAee.getTime(), personId, firstXBin, firstYBin,
							firstAee.getActType());
					activities.add(emFirst);
				}
			}
		}		
	}

	private ActivityEndEvent findCorrespondingActivityEndEvent(
			ActivityStartEvent ase, ArrayList<ActivityEndEvent> arrayList) {
		if (arrayList !=null) {
			Double startTime = ase.getTime();
			Double currTime = Double.MAX_VALUE;
			ActivityEndEvent currEvent = null;
			for (ActivityEndEvent event : arrayList) {
				if (event.getTime() < currTime && event.getTime() > startTime) {
					currTime = event.getTime();
					currEvent = event;
				}
			}
			return currEvent;
		}
		return null;
	}


	

}
