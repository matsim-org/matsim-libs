package playground.julia.distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.EventHandler;

public class CarIntervalHandler implements LinkEnterEventHandler,
LinkLeaveEventHandler {

	HashMap<Id, ArrayList<LinkLeaveEvent>> person2llevent = new HashMap<Id, ArrayList<LinkLeaveEvent>>();
	HashMap<Id, ArrayList<LinkEnterEvent>> person2leevent = new HashMap<Id, ArrayList<LinkEnterEvent>>();
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void handleEvent(LinkLeaveEvent lle){
		Id personId = lle.getPersonId();
		ArrayList<LinkLeaveEvent> events;
		if(person2llevent.containsKey(personId)){
			events = person2llevent.get(personId);
		}else{
			events= new ArrayList<LinkLeaveEvent>();
		}
		events.add(lle);
		person2llevent.put(personId, events);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent lee){
		Id personId = lee.getPersonId();
		ArrayList<LinkEnterEvent> events;
		if(person2leevent.containsKey(personId)){
			events = person2leevent.get(personId);
		}else{
			events = new ArrayList<LinkEnterEvent>();
		}
		events.add(lee);
		person2leevent.put(personId, events);
	}
	
	public void addIntervalsToTimetables(
			List<PersonalExposure> popExposure,
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1,
			Double timeBinSize, String pollutant2analyze, Double outdoorfactor) {
		// TODO Auto-generated method stub

		System.out.println("number of enter person ids: " + person2leevent.size());
		System.out.println("number of leave person ids: " + person2llevent.size());
		
		// fuer jede person - fruehstes enterevent -> naechstes leave event zuordnen
		// (jeweils aus der liste loeschen
		// dauer * belastung des jeweiligen links
		// stundenplan hinzufuegen
		
		for(Id personId: person2llevent.keySet()){
			PersonalExposure perEx = getPersonalExposureFromId(personId, popExposure);
			popExposure.remove(perEx);
			for(Event lee: person2leevent.get(personId)){
				Event lle = findNextLinkLeaveEvent(lee, person2llevent.get(personId));
				if (lle!=null) {
					Double duration = lle.getTime() - lee.getTime();
					Double endOfTimeInterval = Math.ceil(lle.getTime()/ timeBinSize)* timeBinSize;
					Id linkId = ((LinkLeaveEvent) lle).getLinkId();
					Double poll = 100*time2EmissionsTotalFilledAndFiltered1.get(endOfTimeInterval).get(linkId).get(pollutant2analyze)* duration * outdoorfactor;
					perEx.addExposureIntervall(lee.getTime(), lle.getTime(),poll, "car on link " + linkId.toString());
					//System.out.println("added car on link for person " + personId.toString());
					
				}
			}
			popExposure.add(perEx);
		}
		
	}

	private Event findNextLinkLeaveEvent(Event lee, ArrayList<LinkLeaveEvent> arrayList) {
		Double enterTime = lee.getTime();
		Double currTime = Double.MAX_VALUE;
		Event currEvent = null;
		for(Event event: arrayList){
			if(event.getTime()<currTime && event.getTime()>enterTime){
				currTime = event.getTime();
				currEvent = event;
			}
		}
		arrayList.remove(currEvent);
		return currEvent;
	}

	private PersonalExposure getPersonalExposureFromId(Id personId,
			List<PersonalExposure> popExposure) {
		
		for(PersonalExposure perEx: popExposure){
			if(perEx.getPersonalId().equals(personId)){
				return perEx;
			}
		}
		return new PersonalExposure(personId);
	}

}
