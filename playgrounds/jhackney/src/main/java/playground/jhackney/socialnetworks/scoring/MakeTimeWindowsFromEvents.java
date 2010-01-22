package playground.jhackney.socialnetworks.scoring;
/**
 * 
 * This class takes a log (LinkedHashMap) of Person and its Startevents and Endevents,
 * which is an instance of the EventHandler "EventsMapStartEndTimes".
 * 
 * It converts the log to TimeWindows, which are maps of who was
 * at a Facility performing an Activity at the same time.
 * 
 * The same TimeWindows could be calculated in one step in an EventHandler
 * in a MATSIM run, giving cleaner code,
 * because the Events contain a reference to the Act involved.
 * 
 * This is done in the class TrackEventsOverlap.java
 * 
 * However, because the Events that are written out do not include the reference
 * to the Act, but only to the Person, any postprocessing of Events
 * which require the Act must be done in three steps: 1) Plans are read in, 2)Events
 * are read in and united with the Plans via the agentId of the Event, 3) the Acts
 * are re-united with the corresponding Events.
 * 
 * To ensure that postprocessing of events and acts gives exactly the same results
 * as the MATSIM run that generated the events from the acts, use the combination
 * of EventsMapStartEndTimes and this class.
 * 
 * @author jhackney
 */
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.population.ActivityImpl;

import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;

public class MakeTimeWindowsFromEvents {

	LinkedHashMap<Id,ArrayList<TimeWindow>> timeWindowMap=new LinkedHashMap<Id,ArrayList<TimeWindow>>();
	static final private Logger log = Logger.getLogger(MakeTimeWindowsFromEvents.class);
	private final Population population;
	
	public MakeTimeWindowsFromEvents(Population population){
		this.population = population;
	}
	public void makeTimeWindows(EventsMapStartEndTimes epp){
		LinkedHashMap<Id, ArrayList<ActivityStartEvent>> startMap = epp.startMap;
		LinkedHashMap<Id, ArrayList<ActivityEndEvent>> endMap = epp.endMap;
		Id[] persons = startMap.keySet().toArray(new Id[startMap.size()]);
		for (int i=0;i<persons.length;i++){
			//for each startEvent and endEvent
			//
			Id personId=persons[i];
			ArrayList<TimeWindow> twList;
			Person person = this.population.getPersons().get(personId);
			Plan plan =person.getSelectedPlan();
			ArrayList<ActivityStartEvent> startEvents =startMap.get(personId);
			ArrayList<ActivityEndEvent> endEvents = endMap.get(personId);
//30.12			for (int j=0;j<endEvents.size()+1;j++){
			for (int j=0;j<endEvents.size();j++){
				double startTime=0;
				double endTime=0;
				if(j==0){
//					startTime=startEvents.get(startEvents.size()-1).time-86400.;
//30.12					startTime=0;
					startTime=(startEvents.get(startEvents.size()-1).getTime()+86400.)%86400.;
					endTime=endEvents.get(j).getTime();
				}
				else if(j < endEvents.size()){
					startTime=startEvents.get(j-1).getTime();
					endTime=endEvents.get(j).getTime();
				}
//30.12				else if(j==endEvents.size()){
//30.12					startTime=startEvents.get(j-1).time;
//30.12					endTime=30.*3600.;
//30.12				}
				
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(j*2);
				TimeWindow tw=new TimeWindow(startTime, endTime, person, act);
				if(!(timeWindowMap.containsKey(act.getFacilityId()))){
					twList=new ArrayList<TimeWindow>();
				}else{
					twList=timeWindowMap.get(act.getFacilityId());
				}
				twList.add(tw);
				timeWindowMap.remove(act.getFacilityId());
				timeWindowMap.put(act.getFacilityId(),twList);
			}
		}
	}
	
	public void clearTimeWindowMap(){
		this.timeWindowMap.clear();
	}

	public LinkedHashMap<Id,ArrayList<TimeWindow>> getTimeWindowMap() {
		return this.timeWindowMap;
	}
}

