package org.matsim.socialnetworks.scoring;

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
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.socialnetworks.algorithms.CompareActs;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.socialnet.EgoNet;


/**
 * Calculates overlapping between the selected plans of a given population
 * based on events.<br>
 *
 * @author jhackney
 */
public class TrackEventsOverlap implements ActStartEventHandler, ActEndEventHandler {

	Hashtable<Facility,ArrayList<TimeWindow>> timeWindowMap=new Hashtable<Facility,ArrayList<TimeWindow>>();
	HashMap<Act,Double> startMap = new HashMap<Act,Double>();
	
	static final private Logger log = Logger.getLogger(TrackEventsOverlap.class);

	public TrackEventsOverlap() {
		super();
//		makeTimeWindows();
		log.info(" Looking through plans and mapping social interactions for scoring");
	}


	public void handleEvent(final ActStartEvent event) {
		startMap.put(event.act,event.time);
	}
	
	public void handleEvent(final ActEndEvent event) {
		double eventEndTime=event.time;
		double eventStartTime=startMap.get(event.act);
		Person agent = event.agent;
		
		Facility facility = event.act.getFacility();
		if(this.timeWindowMap.containsKey(facility)){
			ArrayList<TimeWindow> myTable=timeWindowMap.get(facility);
			myTable.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.act));
			timeWindowMap.remove(facility);
			timeWindowMap.put(facility, myTable);
		}else{
			ArrayList<TimeWindow> myTable= new ArrayList<TimeWindow>();
			myTable.add(new TimeWindow(eventStartTime,eventEndTime, agent, event.act));
			timeWindowMap.put(facility, myTable);
		}
		
	}


	public void reset(final int iteration) {
//		this.agentScorers.clear();
	}
	
	public Hashtable<Facility,ArrayList<TimeWindow>> getTimeWindowMap(){
		return this.timeWindowMap;
	}

}

