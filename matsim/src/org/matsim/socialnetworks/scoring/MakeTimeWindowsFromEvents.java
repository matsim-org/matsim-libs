package org.matsim.socialnetworks.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.log4j.Logger;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.facilities.Facility;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.socialnetworks.algorithms.EventsPostProcess;
import org.matsim.socialnetworks.mentalmap.TimeWindow;

public class MakeTimeWindowsFromEvents {

	Hashtable<Facility,ArrayList<TimeWindow>> timeWindowMap=new Hashtable<Facility,ArrayList<TimeWindow>>();
	static final private Logger log = Logger.getLogger(MakeTimeWindowsFromEvents.class);
	
	public MakeTimeWindowsFromEvents(EventsPostProcess epp){
		HashMap<Person, ArrayList<ActStartEvent>> startMap = epp.startMap;
		HashMap<Person, ArrayList<ActEndEvent>> endMap = epp.endMap;
		Object[] persons = startMap.keySet().toArray();
		for (int i=0;i<persons.length;i++){
			//for each startEvent and endEvent
			//
			Person person=(Person) persons[i];
			ArrayList<TimeWindow> twList;
			Plan plan =person.getSelectedPlan();
			ArrayList<ActStartEvent> startEvents =startMap.get(person);
			ArrayList<ActEndEvent> endEvents = endMap.get(person);
			for (int j=0;j<endEvents.size()+1;j++){
				double startTime=0;
				double endTime=0;
				if(j==0){
//					startTime=startEvents.get(startEvents.size()-1).time-86400.;
					startTime=0;
					endTime=endEvents.get(j).time;
				}
				else if(j < endEvents.size()){
					startTime=startEvents.get(j-1).time;
					endTime=endEvents.get(j).time;
				}
				else if(j==endEvents.size()){
					startTime=startEvents.get(j-1).time;
					endTime=30.*3600.;
				}
//				endTime=endEvents.get(j).time;
				
				//DEBUG
				if(j*2>plan.getActsLegs().size()){
					System.out.println("stop");
				}
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

	public Hashtable<Facility,ArrayList<TimeWindow>> getTimeWindowMap(){
		return this.timeWindowMap;
	}
}

