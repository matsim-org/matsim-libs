package playground.jhackney.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;

public class EventsMapStartEndTimes implements ActivityStartEventHandler, ActivityEndEventHandler {

	public LinkedHashMap<Person, ArrayList<ActivityStartEvent>> startMap = new LinkedHashMap<Person,ArrayList<ActivityStartEvent>>();
	public LinkedHashMap<Person, ArrayList<ActivityEndEvent>> endMap = new LinkedHashMap<Person,ArrayList<ActivityEndEvent>>();
	public double maxtime=0;
	private Population plans;
	static final private Logger log = Logger.getLogger(EventsMapStartEndTimes.class);

	public EventsMapStartEndTimes(Population plans) {
		super();
//		makeTimeWindows();
		this.plans=plans;
		log.info(" Looking through plans and mapping social interactions for scoring");
	}

	public void handleEvent(ActivityStartEvent event) {
		Person person = plans.getPersons().get(event.getPersonId());
		ArrayList<ActivityStartEvent> startList;
		if((startMap.get(person)==null)){
			startList=new ArrayList<ActivityStartEvent>();
		}else{
			startList=startMap.get(person);
		}
		startList.add(event);
		startMap.remove(person);
		startMap.put(person,startList);
		if(event.getTime()>=maxtime) maxtime=event.getTime();
	}

	public void reset(int iteration) {
		startMap.clear();
		endMap.clear();

	}

	public void handleEvent(ActivityEndEvent event) {
		Person person = plans.getPersons().get(event.getPersonId());
		ArrayList<ActivityEndEvent> endList;
		if((endMap.get(person)== null)){
			endList=new ArrayList<ActivityEndEvent>();
		}else{
			endList=endMap.get(person);
		}
		endList.add(event);
		endMap.remove(person);
		endMap.put(person,endList);
		if(event.getTime()>=maxtime) maxtime=event.getTime();
	}
	public double getMaxTime(){
		return maxtime;
	}
}
