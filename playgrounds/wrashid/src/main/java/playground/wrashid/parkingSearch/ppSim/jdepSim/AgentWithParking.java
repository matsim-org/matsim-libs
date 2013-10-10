/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.ArrayList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingManagerZH;

public class AgentWithParking extends AgentEventMessage{

	public static ParkingStrategyManager parkingStrategyManager;
	public static ParkingManagerZH parkingManager;
	
	public AgentWithParking(Person person) {
		this.setPerson(person);
		this.setPlanElementIndex(0);
		ActivityImpl ai = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(getPlanElementIndex());
		setMessageArrivalTime(ai.getEndTime());
	}
	
	public void scheduleMessage(){
		messageQueue.schedule(this);
	}
	
	@Override
	public void processEvent() {
		if (getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex()) instanceof ActivityImpl){
			handleActivityEndEvent();
		} else {
			Leg leg=(Leg) getPerson().getSelectedPlan().getPlanElements().get(getPlanElementIndex());
			if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
				parkingStrategyManager.getParkingStrategyForCurrentLeg(getPerson(),planElementIndex).handleAgentLeg(this);
			} else {
				handleLeg();
			}
		}
	}
	
	public void processLegInDefaultWay(){
		handleLeg();
	}
	
	
	



}

