package playground.jhackney.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.handler.DeprecatedActivityEndEventHandler;
import org.matsim.core.events.handler.DeprecatedActivityStartEventHandler;

public class EventsMapStartEndTimes implements DeprecatedActivityStartEventHandler, DeprecatedActivityEndEventHandler {

	public LinkedHashMap<Person, ArrayList<ActivityStartEventImpl>> startMap = new LinkedHashMap<Person,ArrayList<ActivityStartEventImpl>>();
	public LinkedHashMap<Person, ArrayList<ActivityEndEventImpl>> endMap = new LinkedHashMap<Person,ArrayList<ActivityEndEventImpl>>();
	public double maxtime=0;
	private Population plans;
	static final private Logger log = Logger.getLogger(EventsMapStartEndTimes.class);

	public EventsMapStartEndTimes(Population plans) {
		super();
//		makeTimeWindows();
		this.plans=plans;
		log.info(" Looking through plans and mapping social interactions for scoring");
	}

	public void handleEvent(ActivityStartEventImpl event) {
		Person person = plans.getPersons().get(event.getPersonId());
		ArrayList<ActivityStartEventImpl> startList;
		if((startMap.get(person)==null)){
			startList=new ArrayList<ActivityStartEventImpl>();
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

	public void handleEvent(ActivityEndEventImpl event) {
		Person person = plans.getPersons().get(event.getPersonId());
		ArrayList<ActivityEndEventImpl> endList;
		if((endMap.get(person)== null)){
			endList=new ArrayList<ActivityEndEventImpl>();
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
