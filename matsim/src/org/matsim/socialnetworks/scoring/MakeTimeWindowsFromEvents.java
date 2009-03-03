package org.matsim.socialnetworks.scoring;
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
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.socialnetworks.algorithms.EventsMapStartEndTimes;
import org.matsim.socialnetworks.mentalmap.TimeWindow;

public class MakeTimeWindowsFromEvents {

	LinkedHashMap<Facility,ArrayList<TimeWindow>> timeWindowMap=new LinkedHashMap<Facility,ArrayList<TimeWindow>>();
	static final private Logger log = Logger.getLogger(MakeTimeWindowsFromEvents.class);
	
	public MakeTimeWindowsFromEvents(){
	}
	public void makeTimeWindows(EventsMapStartEndTimes epp){
		LinkedHashMap<Person, ArrayList<ActStartEvent>> startMap = epp.startMap;
		LinkedHashMap<Person, ArrayList<ActEndEvent>> endMap = epp.endMap;
		Object[] persons = startMap.keySet().toArray();
		for (int i=0;i<persons.length;i++){
			//for each startEvent and endEvent
			//
			Person person=(Person) persons[i];
			ArrayList<TimeWindow> twList;
			Plan plan =person.getSelectedPlan();
			ArrayList<ActStartEvent> startEvents =startMap.get(person);
			ArrayList<ActEndEvent> endEvents = endMap.get(person);
//30.12			for (int j=0;j<endEvents.size()+1;j++){
			for (int j=0;j<endEvents.size();j++){
				double startTime=0;
				double endTime=0;
				if(j==0){
//					startTime=startEvents.get(startEvents.size()-1).time-86400.;
//30.12					startTime=0;
					startTime=(startEvents.get(startEvents.size()-1).time+86400.)%86400.;
					endTime=endEvents.get(j).time;
				}
				else if(j < endEvents.size()){
					startTime=startEvents.get(j-1).time;
					endTime=endEvents.get(j).time;
				}
//30.12				else if(j==endEvents.size()){
//30.12					startTime=startEvents.get(j-1).time;
//30.12					endTime=30.*3600.;
//30.12				}
				
				Act act = (Act) plan.getActsLegs().get(j*2);
				TimeWindow tw=new TimeWindow(startTime, endTime, person, act);
				if(!(timeWindowMap.containsKey(act.getFacility()))){
					twList=new ArrayList<TimeWindow>();
				}else{
					twList=timeWindowMap.get(act.getFacility());
				}
				twList.add(tw);
				timeWindowMap.remove(act.getFacility());
				timeWindowMap.put(act.getFacility(),twList);
			}
		}
	}
	
	public void clearTimeWindowMap(){
		this.timeWindowMap.clear();
	}

	public LinkedHashMap<Facility,ArrayList<TimeWindow>> getTimeWindowMap(){
		return this.timeWindowMap;
	}
}

