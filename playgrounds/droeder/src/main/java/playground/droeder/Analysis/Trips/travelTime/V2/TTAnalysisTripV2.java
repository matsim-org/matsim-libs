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
package playground.droeder.Analysis.Trips.travelTime.V2;

import java.util.ArrayList;
import java.util.ListIterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.travelTime.AbstractTTAnalysisTrip;


/**
 * @author droeder
 *
 */
public class TTAnalysisTripV2 extends AbstractTTAnalysisTrip {
	
	private Integer nrOfElements = 0;
	
	public TTAnalysisTripV2(){
		
	}
	
	@Override
	public void addElements(ArrayList<PlanElement> elements){
		super.addElements(elements);
		this.nrOfElements = elements.size();
	}

	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
	
	public void addEvents(ArrayList<PersonEvent> events){
		this.analyzeEvents(events);
	}
	
	private void analyzeEvents(ArrayList<PersonEvent> events){
		this.analyzeValuesForAllModes(events);
		if(super.getMode().equals(TransportMode.pt)){
			this.analyzeValuesForPT(events);
		}
	}
	
	private void analyzeValuesForAllModes(ArrayList<PersonEvent> events) {
		// from first departure to last arrival
		tripTTime = events.get(events.size() - 2).getTime() - events.get(1).getTime();
	}

	
	private void analyzeValuesForPT(ArrayList<PersonEvent> events) {
		ListIterator<PersonEvent> it = events.listIterator();
		PersonEvent pe;
		double switchWait = 0;
		
		while(it.hasNext()){
			pe = it.next();
		
			if(pe instanceof ActivityEndEvent){
				if(it.previousIndex() == 4){
					accesWaitTime += pe.getTime();
					if(accesWaitTime > 0) accesWaitCnt++;
				}
				
				// only pt interactions between 2 pt legs are registered for waitTime
				else if((it.previousIndex() > 6) && (it.previousIndex() < events.size() - 7)){
					switchWait += pe.getTime();
					if(switchWait > 0) switchWaitCnt++;
					switchWaitTime += switchWait;
					switchWait = 0;
				}
			}
			
			else if(pe instanceof AgentDepartureEvent){
				// every pt leg is a new line
				if(((AgentDepartureEvent) pe).getLegMode().equals(TransportMode.pt)){
					lineCnt++;
					lineTTime -= pe.getTime();
				}
				
				// deside if transit_walk is used for access, egress or switch
				if(((AgentDepartureEvent) pe).getLegMode().equals(TransportMode.transit_walk)){
					if(it.previousIndex() == 1){
						accesWalkTTime -= pe.getTime();
						accesWalkCnt++;
					}else if(it.previousIndex() == events.size() - 3){
						egressWalkTTime -= pe.getTime();
						egressWalkCnt++;
					}else{
						switchWalkTTime -= pe.getTime();
						switchWalkCnt++;
					}
				}
			}
			
			else if(pe instanceof AgentArrivalEvent){
				
				if(((AgentArrivalEvent) pe).getLegMode().equals(TransportMode.pt)){
					lineTTime += pe.getTime();
				}

				// see at departure
				if(((AgentArrivalEvent) pe).getLegMode().equals(TransportMode.transit_walk)){
					if(it.previousIndex() == 2){
						accesWalkTTime += pe.getTime();
					}else if(it.previousIndex() == events.size() - 2){
						egressWalkTTime += pe.getTime();
					}else{
						switchWalkTTime += pe.getTime();
					}
				}
				
			}
			
			else if(pe instanceof ActivityStartEvent){
				if(it.previousIndex() == 3){
					accesWaitTime -= pe.getTime();
				}
				
				// see at endEvent
				else if((it.previousIndex() > 6) && (it.previousIndex() < events.size() - 8)){
					switchWait -= pe.getTime();
				}
			}
		}
		
	}
}
