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
package playground.droeder.Analysis.Trips.V1;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.AnalysisTripSetAllMode;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripGeneratorV1 {
	
	private static final Logger log = Logger
			.getLogger(AnalysisTripGeneratorV1.class);
	
	public static AnalysisTripSetAllMode calculateTripSet(Map<Id, ArrayList<PersonEvent>> events, 
			Map<Id, ArrayList<PlanElement>> planElements, Geometry zone, boolean storeTrips){
		
		AnalysisTripSetAllMode tripSet = new AnalysisTripSetAllMode(storeTrips, zone);

		int nextMsg = 1;
		int errorCnt = 0;
		Set<Id> ids = planElements.keySet();
		ArrayList<PersonEvent> splittedEvents;
		ArrayList<PlanElement> splittedElements;

		ListIterator<PlanElement> elementsIterator;
		ListIterator<PersonEvent> eventIterator;

		PlanElement p;
		int handledCnt = 0;
		
		for(Id id : ids){
			
			// if number of PlanElements and Events of one Agent doesn't fit, don't process
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
						}
						//pt trips consist almost of act-leg-act-leg-act-leg-act or more
						else if(((Activity) p).getType().equals("pt interaction")){
							splittedElements.add(p);
							splittedEvents.add(eventIterator.next());
							splittedEvents.add(eventIterator.next());
						}
						// other modes consists only act-leg-act
						else{
							splittedElements.add(p);
							tripSet.addTrip(new AnalysisTripV1(splittedEvents, splittedElements));
							
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
				
				handledCnt++;
			}else{
				errorCnt++;
			}
			if((handledCnt + errorCnt)  % nextMsg == 0){
				log.info("processed " + nextMsg + " of " + ids.size() + " plans");
				nextMsg *= 2;
			}
		}
		if(errorCnt > 0){
			log.error("was not able to store " + errorCnt + " of " + ids.size() + 
					" plans, because number of events and planElements did not correspond... should be   events.size() == ((planElements.size() * 2) - 2)");
		}
		log.info(handledCnt + " of " + ids.size() + " plans are correct and stored in the TripSet...");
		log.info("finished...");
		
		return tripSet;
	}
}
