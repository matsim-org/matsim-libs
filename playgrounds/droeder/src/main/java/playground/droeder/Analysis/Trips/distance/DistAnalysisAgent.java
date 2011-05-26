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
import org.matsim.core.api.experimental.events.AgentEvent;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;

/**
 * @author droeder
 *
 */
public class DistAnalysisAgent {
	
	private LinkedList<AbstractAnalysisTrip> trips;
	private Id id;
	
	public DistAnalysisAgent(LinkedList<AbstractAnalysisTrip> linkedList, Id id){
		this.trips = linkedList;
		this.id = id;
	}

	public boolean processAgentEvent(AgentEvent e) {
		((DistAnalysisTrip) this.trips.getFirst()).processAgentEvent(e);
		return ((DistAnalysisTrip) this.trips.getFirst()).isFinished();
	}

	public void processLinkEnterEvent(double length) {
		((DistAnalysisTrip) this.trips.getFirst()).processLinkEnterEvent(length);
	}
	
	public void passedLinkInPt(double length) {
		((DistAnalysisTrip) this.trips.getFirst()).passedLinkInPt(length);
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
	 * @return
	 */
	public AbstractAnalysisTrip removeFinishedTrip() {
		return this.trips.removeFirst();
	}

}
