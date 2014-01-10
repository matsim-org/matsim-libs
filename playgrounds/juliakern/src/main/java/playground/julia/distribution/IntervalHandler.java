package playground.julia.distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.dom3.as.ASElementDeclaration;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class IntervalHandler implements LinkEnterEventHandler,
LinkLeaveEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{

	HashMap<Id, ArrayList<LinkLeaveEvent>> person2llevent = new HashMap<Id, ArrayList<LinkLeaveEvent>>();
	HashMap<Id, ArrayList<LinkEnterEvent>> person2leevent = new HashMap<Id, ArrayList<LinkEnterEvent>>();
	HashMap<Id, ArrayList<ActivityStartEvent>> person2asevent = new HashMap<Id, ArrayList<ActivityStartEvent>>();
	HashMap<Id, ArrayList<ActivityEndEvent>> person2aeevent = new HashMap<Id, ArrayList<ActivityEndEvent>>();
	
	@Override
	public void reset(int iteration) {
		person2llevent.clear();
		person2leevent.clear();
		person2asevent.clear();
		person2aeevent.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		ArrayList<LinkLeaveEvent> events;
		if(person2llevent.containsKey(personId)){
			events = person2llevent.get(personId);
		}else{
			events= new ArrayList<LinkLeaveEvent>();
		}
		events.add(event);
		person2llevent.put(personId, events);
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		ArrayList<LinkEnterEvent> events;
		if(person2leevent.containsKey(personId)){
			events = person2leevent.get(personId);
		}else{
			events = new ArrayList<LinkEnterEvent>();
		}
		events.add(event);
		person2leevent.put(personId, events);
		
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

	public void addActivitiesToTimetables(ArrayList<EmActivity> activities, Map<Id,Integer> link2xbins, Map<Id,Integer> link2ybins, Double simulationEndTime) {
		
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
				int firstXBin = link2xbins.get(firstAee.getLinkId());
				int firstYBin = link2ybins.get(firstAee.getLinkId());
				EmActivity emFirst = new EmActivity(0.0, firstAee.getTime(),
						personId, firstXBin, firstYBin, firstAee.getActType());
				activities.add(emFirst);
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

	public void addCarTripsToTimetables(ArrayList<EmCarTrip> carTrips, Double simulationEndTime) {
		// combine link enter events with link leave events to em car trip
		for(Id personId: person2leevent.keySet()){
			for(LinkEnterEvent lee: person2leevent.get(personId)){
				Double startOfEvent = lee.getTime();
				Id linkId = lee.getLinkId();
				LinkLeaveEvent lle = findCorrespondingLinkLeaveEvent(lee, person2llevent.get(personId));
				Double endOfEvent;
				if(lle==null){
					endOfEvent = simulationEndTime;
				}else{
					endOfEvent = lle.getTime();
				}
				EmCarTrip emcar = new EmCarTrip(startOfEvent, endOfEvent, personId, linkId);
				carTrips.add(emcar);
			}
		}
		
	}

	private LinkLeaveEvent findCorrespondingLinkLeaveEvent(LinkEnterEvent lee,
			ArrayList<LinkLeaveEvent> arrayList) {
		Double enterTime = lee.getTime();
		Double currTime = Double.MAX_VALUE;
		LinkLeaveEvent currEvent = null;
		for(LinkLeaveEvent event: arrayList){
			if(event.getTime()<currTime && event.getTime()>enterTime){
				currTime = event.getTime();
				currEvent = event;
			}
		}
		return currEvent;
	}
	

}
