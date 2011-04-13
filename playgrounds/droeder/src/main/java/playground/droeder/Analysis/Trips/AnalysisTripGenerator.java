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
//	private static Map<Id, ArrayList<PersonEvent>> events;
//	private static Map<Id, ArrayList<PlanElement>> planElements;
//	private static AnalysisTripSet tripSet;
//	private static Geometry zone;
	
	private static final Logger log = Logger
			.getLogger(AnalysisTripGenerator.class);

//	public AnalysisTripSetGenerator(Map<Id, ArrayList<PersonEvent>> events, 
//			Map<Id, ArrayList<PlanElement>> planElements, Geometry zone){
//		AnalysisTripSetGenerator.events = events;
//		AnalysisTripSetGenerator.planElements = planElements;
//		AnalysisTripSetGenerator.zone = zone;
//	}
	
	public static AnalysisTripSet calculateTripSet(Map<Id, ArrayList<PersonEvent>> events, 
			Map<Id, ArrayList<PlanElement>> planElements, Geometry zone){
		AnalysisTripSet tripSet = new AnalysisTripSet(zone);
		int nextMsg = 1;
		Set<Id> ids = planElements.keySet();
		ArrayList<ArrayList<PlanElement>> splittedElements;
		ListIterator<PlanElement> elementsIterator;

		ArrayList<PlanElement> temp;
		PlanElement p;
		int counter;
		int msgCnt = 0;
		
		
		for(Id id : ids){
			elementsIterator = planElements.get(id).listIterator();
			splittedElements = new ArrayList<ArrayList<PlanElement>>(); 
			
			
			temp = new ArrayList<PlanElement>();
			while (elementsIterator.hasNext()){
				p = elementsIterator.next();
				
				if(p instanceof Activity){
					if(temp.size() == 0){
						temp.add(p);
					}else if(((Activity) p).getType().equals("pt interaction")){
						temp.add(p);
					}else{
						temp.add(p);
						splittedElements.add(temp);
						temp = new ArrayList<PlanElement>();
						
						if(elementsIterator.nextIndex() == planElements.get(id).size()){
							break;
						}else{
							elementsIterator.previous();
						}
					}
				}else if(p instanceof Leg){
					temp.add(p);
				}
			}
			
			
			//TODO Test this
			counter = 0;
			System.out.println(id);
			for(ArrayList<PlanElement> pes : splittedElements){
				AnalysisTrip trip = new AnalysisTrip((ArrayList<PersonEvent>) events.get(id).subList(counter, pes.size()-1), pes);
				trip.toString();
				tripSet.addTrip(trip);
				counter = pes.size();
			}
			
			msgCnt++;
			if(msgCnt % nextMsg == 0){
				log.info("processed " + nextMsg + " of " + ids.size() + " plans");
				nextMsg *= 2;
			}
		}
		
		return tripSet;
	}
}
