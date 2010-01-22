package playground.jhackney.socialnetworks.scoring;

/* *********************************************************************** *
 * project: org.matsim.*
 * EventOverlap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.population.PersonImpl;

import playground.jhackney.socialnetworks.mentalmap.TimeWindow;


/**
 * Calculates overlapping between the selected plans of a given population
 * based on events.<br>
 *
 * @author jhackney
 */
public class TrackEventsOverlap implements ActivityStartEventHandler, ActivityEndEventHandler {

	LinkedHashMap<Id,ArrayList<TimeWindow>> timeWindowMap=new LinkedHashMap<Id,ArrayList<TimeWindow>>();
	LinkedHashMap<Activity,Double> startMap = new LinkedHashMap<Activity,Double>();
	LinkedHashMap<Activity,Double> endMap = new LinkedHashMap<Activity,Double>();
	
	final Population population;
	private Map<Id, Integer> agentActIndex = new HashMap<Id, Integer>(); // agent-id, planelement-index
	
	static final private Logger log = Logger.getLogger(TrackEventsOverlap.class);

	public TrackEventsOverlap(Population population) {
		super();
		this.population = population;
//		makeTimeWindows();
		log.info(" Looking through plans and mapping social interactions for scoring");
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {

		double eventStartTime=-999;// event start time is unknown
		double eventEndTime=event.getTime();
		Activity eventAct = getCurrentActivity(event.getPersonId());
		
		if(startMap!=null){
			if(startMap.get(eventAct)!=null){
				eventStartTime=startMap.get(eventAct);
			}
		}
		if(eventStartTime>0){// if a valid start event is found, make a timeWindow and add to Map
			PersonImpl agent = (PersonImpl) this.population.getPersons().get(event.getPersonId());

			Id facilityId = event.getFacilityId();
			if (this.timeWindowMap.containsKey(facilityId)) {
				ArrayList<TimeWindow> windowList=timeWindowMap.get(facilityId);
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, eventAct));
				timeWindowMap.remove(facilityId);
				timeWindowMap.put(facilityId, windowList);
			} else {
				ArrayList<TimeWindow> windowList= new ArrayList<TimeWindow>();
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, eventAct));
				timeWindowMap.put(facilityId, windowList);
			}
		} else {
			//do nothing immediately if there is no start event, just save this end event for later
			endMap.put(eventAct,event.getTime());
		}
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {

		double eventStartTime=event.getTime();
		double eventEndTime=-999;// the event end time is not known
		Activity eventAct = getNextActivity(event.getPersonId());
		if(endMap!=null){
			if(endMap.get(eventAct)!=null){
				eventEndTime=endMap.get(eventAct);
			}
		}
		if(eventEndTime>0){// if a valid end time is found, make a timeWindow and add to Map
			PersonImpl agent = (PersonImpl) this.population.getPersons().get(event.getPersonId());

			Id facilityId = event.getFacilityId();
			if(this.timeWindowMap.containsKey(facilityId)){
				ArrayList<TimeWindow> windowList=timeWindowMap.get(facilityId);
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, eventAct));
				timeWindowMap.remove(facilityId);
				timeWindowMap.put(facilityId, windowList);
			}else{
				ArrayList<TimeWindow> windowList= new ArrayList<TimeWindow>();
				windowList.add(new TimeWindow(eventStartTime,eventEndTime, agent, eventAct));
				timeWindowMap.put(facilityId, windowList);
			}
		} else {
			// if the event is not complete, save the start information for later
			startMap.put(eventAct,event.getTime());
		}
	}


	public void reset(final int iteration) {
//		this.timeWindowMap.clear();
	}
	public void clearTimeWindowMap(){
		this.timeWindowMap.clear();
	}

	public LinkedHashMap<Id,ArrayList<TimeWindow>> getTimeWindowMap(){
		return this.timeWindowMap;
	}

	private Activity getNextActivity(final Id agentId) {
		Integer idx = this.agentActIndex.get(agentId);
		if (idx == null) {
			idx = Integer.valueOf(1);
		} else {
			idx = Integer.valueOf(idx.intValue() + 2);
		}
		this.agentActIndex.put(agentId, idx);
		return (Activity) this.population.getPersons().get(agentId).getSelectedPlan().getPlanElements().get(idx.intValue());
	}
	
	private Activity getCurrentActivity(final Id agentId) {
		Integer idx = this.agentActIndex.get(agentId);
		if (idx == null) {
			return null;
		}
		return (Activity) this.population.getPersons().get(agentId).getSelectedPlan().getPlanElements().get(idx.intValue());
	}
	
}

