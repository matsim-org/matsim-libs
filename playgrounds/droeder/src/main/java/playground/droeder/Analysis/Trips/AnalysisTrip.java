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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEvent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class AnalysisTrip {
	protected Coordinate start;
	protected Coordinate end;
	protected String mode = null;

	//all modes
	protected double tripTTime = 0.0;
	
	// pt only
	protected int accesWalkCnt = 0;
	protected int accesWaitCnt = 0;
	protected int egressWalkCnt = 0;
	protected int switchWalkCnt= 0;
	protected int switchWaitCnt = 0;
	protected int lineCnt = 0;
	
	protected double accesWalkTTime = 0.0;
	protected double accesWaitTime = 0.0;
	protected double egressWalkTTime = 0.0;
	protected double switchWalkTTime = 0.0;
	protected double switchWaitTime = 0.0;
	protected double lineTTime = 0.0;
	

	
	protected void analyzeElements(ArrayList<PlanElement> elements) {
		this.findMode(elements);
		//if no zones in TripSet are defined, coords not necessary
		if(!(((Activity) elements.get(0)).getCoord() == null) && !(((Activity) elements.get(elements.size() - 1)).getCoord() == null)){
			this.start = new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
					((Activity) elements.get(0)).getCoord().getY());
			this.end = new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
					((Activity) elements.get(elements.size() - 1)).getCoord().getY());
		}
	}
	
	// not essential but good to prevent mixing up different modes
	private void findMode(ArrayList<PlanElement> elements) {
		for(PlanElement p : elements){
			if(p instanceof Leg){
				if(((Leg) p).getMode().equals(TransportMode.transit_walk)){
					this.mode = TransportMode.transit_walk;
				}else{
					this.mode = ((Leg) p).getMode();
					return;
				}
			}
		}
	}
	
	protected void analyzeEvents(ArrayList<PersonEvent> events){
		this.analyzeValuesForAllModes(events);
		if(this.mode.equals(TransportMode.pt)){
			this.analyzeValuesForPT(events);
		}
	}
	
	protected void analyzeValuesForAllModes(ArrayList<PersonEvent> events) {
		// from first departure to last arrival
		tripTTime = events.get(events.size() - 2).getTime() - events.get(1).getTime();
	}

	
	protected void analyzeValuesForPT(ArrayList<PersonEvent> events) {
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
	
	public String getMode(){
		return this.mode;
	}
	
	/**
	 * @return
	 */
	public Geometry getStart() {
		return new GeometryFactory().createPoint(this.start);
	}

	/**
	 * @return
	 */
	public Geometry getEnd() {
		return new GeometryFactory().createPoint(this.end);
	}
	
	/**
	 * @return the tripTTime
	 */
	public Double getTripTTime() {
		return tripTTime;
	}

	/**
	 * @return the accesWalkCnt
	 */
	public int getAccesWalkCnt() {
		return accesWalkCnt;
	}

	/**
	 * @return the accesWaitCnt
	 */
	public int getAccesWaitCnt() {
		return accesWaitCnt;
	}

	/**
	 * @return the egressWalkCnt
	 */
	public int getEgressWalkCnt() {
		return egressWalkCnt;
	}

	/**
	 * @return the switchWalkCnt
	 */
	public int getSwitchWalkCnt() {
		return switchWalkCnt;
	}

	/**
	 * @return the switchWaitCnt
	 */
	public int getSwitchWaitCnt() {
		return switchWaitCnt;
	}

	/**
	 * @return the lineCnt
	 */
	public int getLineCnt() {
		return lineCnt;
	}

	/**
	 * @return the accesWalkTTime
	 */
	public double getAccesWalkTTime() {
		return accesWalkTTime;
	}

	/**
	 * @return the accesWaitTime
	 */
	public double getAccesWaitTime() {
		return accesWaitTime;
	}

	/**
	 * @return the egressWalkTTime
	 */
	public double getEgressWalkTTime() {
		return egressWalkTTime;
	}

	/**
	 * @return the switchWalkTTime
	 */
	public double getSwitchWalkTTime() {
		return switchWalkTTime;
	}

	/**
	 * @return the switchWaitTime
	 */
	public double getSwitchWaitTime() {
		return switchWaitTime;
	}

	/**
	 * @return the lineTTime
	 */
	public double getLineTTime() {
		return lineTTime;
	}


}
