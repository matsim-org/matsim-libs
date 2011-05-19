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
package playground.droeder.Analysis.Trips.distance;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;

/**
 * @author droeder
 *
 */
public class DistAnalysisAgent {
	
	private LinkedList<DistAnalysisTrip> trips;
	private Id id;
	
	public DistAnalysisAgent(LinkedList<DistAnalysisTrip> trips, Id id){
		this.trips = trips;
		this.id = id;
	}


	/**
	 * @param e
	 */
	public boolean processEvent(AgentEvent e) {
		// TODO Auto-generated method stub
		if(e instanceof AgentArrivalEvent){
			
		}else if(e instanceof AgentDepartureEvent){
			
		}
		return this.trips.getFirst().isFinished();
	}

	/**
	 * @param e
	 * @param length 
	 */
	public boolean processLinkEnterEvent(LinkEnterEvent e, double length) {
		// TODO Auto-generated method stub
		
		return this.trips.getFirst().isFinished();
	}

	public Id getId(){
		return this.id;
	}
	
	@Override
	public boolean equals(final Object other){
		if(!(other instanceof DistAnalysisAgent)){
			return false;
		}else{
			if(((DistAnalysisAgent) other).getId().equals(this.id)){
				return true;
			}else{
				return false;
			}
		}
	}


	/**
	 * @param linkLength
	 */
	public void passedLinkOnRide(double linkLength) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * @return
	 */
	public AbstractAnalysisTrip removeFinishedTrip() {
		return this.trips.removeFirst();
	}

}
