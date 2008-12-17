package org.matsim.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Facility;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.socialnetworks.mentalmap.TimeWindow;

public class EventsMapStartEndTimes implements ActStartEventHandler, ActEndEventHandler {

	public HashMap<Person, ArrayList<ActStartEvent>> startMap = new HashMap<Person,ArrayList<ActStartEvent>>();
	public HashMap<Person, ArrayList<ActEndEvent>> endMap = new HashMap<Person,ArrayList<ActEndEvent>>();
	private Population plans;
	static final private Logger log = Logger.getLogger(EventsMapStartEndTimes.class);

	public EventsMapStartEndTimes(Population plans) {
		super();
//		makeTimeWindows();
		this.plans=plans;
		log.info(" Looking through plans and mapping social interactions for scoring");
	}

	public void handleEvent(ActStartEvent event) {
		// TODO Auto-generated method stub
		Person person = plans.getPerson(event.agentId);
		ArrayList<ActStartEvent> startList;
		if((startMap.get(person)==null)){
			startList=new ArrayList<ActStartEvent>();
		}else{
			startList=startMap.get(person);
		}
		startList.add(event);
		startMap.remove(person);
		startMap.put(person,startList);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		startMap.clear();
		endMap.clear();

	}

	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub
		Person person = plans.getPerson(event.agentId);
		ArrayList<ActEndEvent> endList;
		if((endMap.get(person)== null)){
			endList=new ArrayList<ActEndEvent>();
		}else{
			endList=endMap.get(person);
		}
		endList.add(event);
		endMap.remove(person);
		endMap.put(person,endList);
	}
}
