/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level1;

import java.util.LinkedList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.utils.collections.Tuple;

/**
 * Collects travel time data for one single pt trip consisting of transit walk legs and pt legs
 * 
 * @author aneumann
 *
 */
public class AgentId2PtTripTravelTimeMapData {
	
	private final Logger log = Logger.getLogger(AgentId2PtTripTravelTimeMapData.class);
	private final Level logLevel = Level.DEBUG;

	private ActivityEndEvent startAct;
	private ActivityStartEvent endAct;
	
	LinkedList<Tuple<String, Double>> type2TravelTimeMap = new LinkedList<Tuple<String,Double>>();
	
	public AgentId2PtTripTravelTimeMapData(ActivityEndEvent startAct){
		this.log.setLevel(this.logLevel);
		this.startAct = startAct;
	}

	protected void handle(AgentDepartureEvent event) {
		// preregister
		this.type2TravelTimeMap.add(new Tuple<String, Double>(event.getLegMode(), new Double(event.getTime())));		
	}

	protected void handle(AgentArrivalEvent event) {		
		if(this.type2TravelTimeMap.getLast().getFirst().equalsIgnoreCase(event.getLegMode())){
			// calculate leg travel time
			Tuple<String, Double> lastEntry = this.type2TravelTimeMap.removeLast();
			this.type2TravelTimeMap.add(new Tuple<String, Double>(event.getLegMode(), new Double(event.getTime() - lastEntry.getSecond().doubleValue())));
		} else {
			this.log.warn("Got two different legs to handle. Start act: " + this.startAct + " Last event is: " + event);
		}
	}

	protected void setStartEvent(ActivityStartEvent event) {
		this.endAct = event;		
	}
	
	/**
	 * @return Returns the total trip travel time including transit walks and pt legs
	 */
	public double getTotalTripTravelTime(){
		double travelTime = 0.0;
		for (Tuple<String, Double> tuple : this.type2TravelTimeMap) {
			travelTime += tuple.getSecond().doubleValue();
		}
		return travelTime;
	}
	
	/**
	 * @return Returns the number of transfers
	 */
	public int getNumberOfTransfers(){
		int numberOfTransfers = -1;
		for (Tuple<String, Double> tuple : this.type2TravelTimeMap) {
			if(tuple.getFirst().equalsIgnoreCase(TransportMode.pt)){
				numberOfTransfers++;
			}
		}
		return numberOfTransfers;		
	}
	
	@Override
	public String toString() {
		return "PT Leg from: " + this.startAct + " to " + this.endAct + " took " + this.getTotalTripTravelTime() + " seconds and " + this.getNumberOfTransfers() + " transfers";
	}
	
}
