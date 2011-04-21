/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEvent;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripGenerator {
	
	private static final Logger log = Logger
			.getLogger(AnalysisTripGenerator.class);
	
	public static Map<String, AnalysisTripSet> calculateTripSet(Map<Id, ArrayList<PersonEvent>> events, 
			Map<Id, ArrayList<PlanElement>> planElements, Geometry zone, boolean storeTrips){
		
		Map<String, AnalysisTripSet> mode2tripSet = new HashMap<String, AnalysisTripSet>();

		int nextMsg = 1;
		Set<Id> ids = planElements.keySet();
		ArrayList<PersonEvent> splittedEvents;
		ArrayList<PlanElement> splittedElements;

		ListIterator<PlanElement> elementsIterator;
		ListIterator<PersonEvent> eventIterator;
		AnalysisTrip trip;

		PlanElement p;
		int msgCnt = 0;
		
		String mode;
		AnalysisTripSet tripSet;
		
		for(Id id : ids){
			
			if( ((planElements.get(id).size() * 2) - 2) ==  events.get(id).size()){
				elementsIterator = planElements.get(id).listIterator();
				eventIterator = events.get(id).listIterator();
				
				// split planelements into trips
				splittedElements = new ArrayList<PlanElement>();
				splittedEvents = new ArrayList<PersonEvent>();
				while (elementsIterator.hasNext()){
					p = elementsIterator.next();
					
					if(p instanceof Activity){
						if(splittedElements.size() == 0){
							splittedElements.add(p);
							splittedEvents.add(eventIterator.next());
							splittedEvents.add(eventIterator.next());
						}else if(((Activity) p).getType().equals("pt interaction")){
							splittedElements.add(p);
							splittedEvents.add(eventIterator.next());
							splittedEvents.add(eventIterator.next());
						}else{
							splittedElements.add(p);
							trip = new AnalysisTrip(splittedEvents, splittedElements);
							mode = trip.getMode();
							if(mode2tripSet.containsKey(mode)){
								mode2tripSet.get(mode).addTrip(trip);
							}else{
								tripSet = new AnalysisTripSet(mode, zone, storeTrips);
								tripSet.addTrip(trip);
								mode2tripSet.put(mode, tripSet);
							}
							
							splittedElements = new ArrayList<PlanElement>();
							splittedEvents = new ArrayList<PersonEvent>();

							if(elementsIterator.nextIndex() == planElements.get(id).size()){
								break;
							}else{
								elementsIterator.previous();
							}
						}
					}else if(p instanceof Leg){
						splittedElements.add(p);
						splittedEvents.add(eventIterator.next());
						splittedEvents.add(eventIterator.next());
					}
				}
				
				msgCnt++;
				if(msgCnt % nextMsg == 0){
					log.info("processed " + nextMsg + " of " + ids.size() + " plans");
					nextMsg *= 2;
				}
			}
		}
		
		return mode2tripSet;
	}
}
