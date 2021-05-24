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
package playground.vsp.analysis.modules.ptTripAnalysis.traveltime;

import java.util.ArrayList;
import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import playground.vsp.analysis.modules.ptTripAnalysis.AbstractAnalysisTrip;

/**
 * @author droeder
 *
 */
public class TTAnalysisTrip  extends AbstractAnalysisTrip implements TTAnalysisTripI {
	
	//all modes
	public double tripTTime = 0.0;
	
	// pt only
	public int accesWalkCnt = 0;
	public int accesWaitCnt = 0;
	public int egressWalkCnt = 0;
	public int switchWalkCnt= 0;
	public int switchWaitCnt = 0;
	public int lineCnt = 0;
	
	public double accesWalkTTime = 0.0;
	public double accesWaitTime = 0.0;
	public double egressWalkTTime = 0.0;
	public double switchWalkTTime = 0.0;
	public double switchWaitTime = 0.0;
	public double lineTTime = 0.0;
	
	
	private Integer nrOfExpEvents = null;
	private Integer nrOfElements = 0;
	private PtTimeHandler handler;
	
	private Collection<String> networkModes;
	private Collection<String> ptModes;
	
	public TTAnalysisTrip(Collection<String> ptModes, Collection<String> networkModes){
		this.ptModes = ptModes;
		this.networkModes = networkModes;
	}
	
	public void addElements(ArrayList<PlanElement> elements){
		this.nrOfElements = elements.size();
		this.nrOfExpEvents = this.findExpectedNumberOfEvents(elements);
		this.analyzeElements(elements);
		
		//handler is only needed for pt
		if(this.ptModes.contains(super.getMode())){
			this.handler = new PtTimeHandler();
		}
	}
	
	
	private void analyzeElements(ArrayList<PlanElement> elements) {
		this.findMode(elements);
		//if no zones in TripSet are defined, coords not necessary
		if(!(((Activity) elements.get(0)).getCoord() == null) && !(((Activity) elements.get(elements.size() - 1)).getCoord() == null)){
			super.setStart(new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
					((Activity) elements.get(0)).getCoord().getY()));
			super.setEnd(new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
					((Activity) elements.get(elements.size() - 1)).getCoord().getY()));
		}
	}
	
	// not essential but good to prevent mixing up different modes
	// might run into problems, when using multi-modal-trip-routing /dr dec '12
	private void findMode(ArrayList<PlanElement> elements) {
		for(PlanElement p : elements){
			if(p instanceof Leg){
				if(((Leg) p).getMode().equals(TransportMode.transit_walk)){
					super.setMode(TransportMode.transit_walk);
				}else{
					super.setMode(((Leg) p).getMode());
					return;
				}
			}
		}
	}
	
	public Integer getNrOfElements(){
		return this.nrOfElements;
	}
	
	private int findExpectedNumberOfEvents(ArrayList<PlanElement> elements){
		int temp = 0;
		for(PlanElement pe: elements){
			if( pe instanceof Leg){
			
				if(this.ptModes.contains(((Leg) pe).getMode())){
					// +4 for every pt-leg (end, enter, leave, start)
					temp +=6;
				}
				else if(this.networkModes.contains(((Leg) pe).getMode()))
					// +4 for every simulated-network-mode-leg (end, enter, leave, start)
					temp +=6;
				else{
					// +2 for teleported modes (end, start)
					temp +=4;
				}
			}
		}
		return temp;
	}

	private int handledEvents = 0;
	private Double first = null;
	private Double last = 0.0;
	/**
	 * returns true if enough events are handled and the trip is finished
	 * @param e
	 * @return
	 */
	public boolean handleEvent(Event e){
		this.handledEvents++;
		if(this.ptModes.contains(super.getMode())){
			handler.handleEvent(e);
			if(this.handledEvents == this.nrOfExpEvents){
				handler.finish(this);
				return true;
			}else{
				return false;
			}
		}else{
			if(first == null){
				first = e.getTime();
			}else{
				last = e.getTime();
			}
			
			if(this.handledEvents == nrOfExpEvents){
				this.tripTTime = last - first;
				return true;
			}else{
				return false;
			}
		}
		
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
