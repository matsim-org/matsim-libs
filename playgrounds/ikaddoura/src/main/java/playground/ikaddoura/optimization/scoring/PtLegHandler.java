///* *********************************************************************** *
// * project: org.matsim.*
// * InVehWaitHandler.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// * 
// */
//package playground.ikaddoura.optimization.scoring;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.matsim.api.core.v01.Id;
//import org.matsim.core.api.experimental.events.ActivityEndEvent;
//import org.matsim.core.api.experimental.events.AgentArrivalEvent;
//import org.matsim.core.api.experimental.events.AgentDepartureEvent;
//import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
//import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
//import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
//import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
//import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
//import org.matsim.pt.PtConstants;
//
///**
// * @author Ihab
// *
// */
//public class PtLegHandler implements PersonEntersVehicleEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityEndEventHandler, TransitDriverStartsEventHandler {
//	private final Map <Id, Double> personId2WaitingTime = new HashMap<Id, Double>();
//	private final Map <Id, Double> personId2PersonEntersVehicleTime = new HashMap<Id, Double>();
//	private final Map <Id, Double> personId2AgentDepartureTime = new HashMap<Id, Double>();
//	private final Map <Id, Double> personId2InVehicleTime = new HashMap<Id, Double>();
//	private final List<Id> ptDriverIDs = new ArrayList<Id>();
//	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
//
//	private final Map <Id, Boolean> personId2IsEgress = new HashMap<Id, Boolean>();
//
//	@Override
//	public void reset(int iteration) {
//		personId2WaitingTime.clear();
//		personId2PersonEntersVehicleTime.clear();
//		personId2AgentDepartureTime.clear();
//		personId2InVehicleTime.clear();
//		ptDriverIDs.clear();
//		ptVehicleIDs.clear();
//	}
//	
//	@Override
//	public void handleEvent(PersonEntersVehicleEvent event) {
//		Id personId = event.getDriverId();
//		Id vehId = event.getVehicleId();
//		
//		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
//
//			personId2PersonEntersVehicleTime.put(personId, event.getTime());
//			
//			double waitingTime;
//			if (personId2AgentDepartureTime.get(personId) == null){
//				throw new RuntimeException("Person " + personId + " is entering vehicle " + vehId + " without having departed from an activity. Aborting...");
//			} else {
//				waitingTime =  event.getTime() - personId2AgentDepartureTime.get(personId);
//			}
//			if (personId2WaitingTime.get(personId) == null){
//				personId2WaitingTime.put(personId, waitingTime);
//			} else {
//				double waitingTimeSum = personId2WaitingTime.get(personId) + waitingTime;
//				personId2WaitingTime.put(personId, waitingTimeSum);
//			}
//						
//		}
//	}
//
//	@Override
//	public void handleEvent(AgentDepartureEvent event) {
//		Id personId = event.getDriverId();
//		
//		if (event.getLegMode().toString().equals("pt")){
//			personId2AgentDepartureTime.put(personId, event.getTime());
//		} else {
//			// not a pt Leg
//		}
//		
//	}
//
//	@Override
//	public void handleEvent(AgentArrivalEvent event) {
//		Id personId = event.getDriverId();
//		
//		if (event.getLegMode().toString().equals("pt")){
//
//			double inVehicleTime = 0.0;
//			if (personId2PersonEntersVehicleTime.get(personId) == null){
//				throw new RuntimeException("Person " + personId + " is arriving without having departed from an activity. Aborting...");
//			} else {
//				inVehicleTime = event.getTime() - personId2PersonEntersVehicleTime.get(personId);
//			}
//			if (personId2InVehicleTime.get(personId) == null) {
//				personId2InVehicleTime.put(personId, inVehicleTime);
//			} else {
//				double inVehicleTimeSum = personId2InVehicleTime.get(personId) + inVehicleTime;
//				personId2InVehicleTime.put(personId, inVehicleTimeSum);
//			}			
//			
//		} else {
//			// not a pt Leg
//		}
//	}
//	
//	@Override
//	public void handleEvent(ActivityEndEvent event) {
//		if (event.getActType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
//			this.personId2IsEgress.put(event.getDriverId(), true);
//		}
//	}
//	
//	@Override
//	public void handleEvent(TransitDriverStartsEvent event) {
//		Id ptDriverId = event.getDriverId();
//		Id ptVehicleId = event.getVehicleId();
//		
//		if (!this.ptDriverIDs.contains(ptDriverId)){
//			this.ptDriverIDs.add(ptDriverId);
//		}
//		
//		if (!this.ptVehicleIDs.contains(ptVehicleId)){
//			this.ptVehicleIDs.add(ptVehicleId);
//		}
//	}
//
//	public Map<Id, Double> getPersonId2WaitingTime() {
//		return personId2WaitingTime;
//	}
//
//	public Map<Id, Double> getPersonId2InVehicleTime() {
//		return personId2InVehicleTime;
//	}
//	
//	public Map<Id, Boolean> getPersonId2IsEgress() {
//		return personId2IsEgress;
//	}
//	
//}
