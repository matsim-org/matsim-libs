/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.events.HouseholdEnterMeetingPointEvent;
import playground.christoph.evacuation.events.HouseholdEnterMeetingPointEventImpl;
import playground.christoph.evacuation.events.HouseholdJoinedEvent;
import playground.christoph.evacuation.events.HouseholdLeaveMeetingPointEvent;
import playground.christoph.evacuation.events.HouseholdSeparatedEvent;
import playground.christoph.evacuation.events.HouseholdSetMeetingPointEvent;
import playground.christoph.evacuation.events.handler.HouseholdEnterMeetingPointEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdJoinedEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdLeaveMeetingPointEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdSeparatedEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdSetMeetingPointEventHandler;
import playground.christoph.evacuation.mobsim.PassengerEventsCreator;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdInfo;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

/**
 *  Define which households will relocate to another (secure!) location
 *  at which time.
 *  
 *  Moreover it is decided which transport mode will be used for the evacuation.
 *  If a car is available, it is used. Otherwise the people will walk.
 *  
 *  @author cdobler
 */
public class JoinedHouseholdsIdentifier extends DuringActivityIdentifier implements 
	HouseholdJoinedEventHandler, HouseholdSeparatedEventHandler, HouseholdSetMeetingPointEventHandler,
	HouseholdEnterMeetingPointEventHandler, HouseholdLeaveMeetingPointEventHandler,	SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final HouseholdsUtils householdUtils;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final PassengerEventsCreator passengerEventsCreator;
	private final Map<Id, PlanBasedWithinDayAgent> agentMapping;
	private final Map<Id, String> transportModeMapping;
	private final Set<Id> joinedHouseholds;
	private final Queue<HouseholdDeparture> householdDepartures;
	
	public JoinedHouseholdsIdentifier(HouseholdsUtils householdUtils, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			ModeAvailabilityChecker modeAvailabilityChecker, PassengerEventsCreator passengerEventsCreator) {
		this.householdUtils = householdUtils;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.passengerEventsCreator = passengerEventsCreator;
		
		this.agentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.transportModeMapping = new ConcurrentHashMap<Id, String>();
		this.householdDepartures = new PriorityBlockingQueue<HouseholdDeparture>(500, new DepartureTimeComparator());
		this.joinedHouseholds = new HashSet<Id>();
	}

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();
	
		while (this.householdDepartures.peek() != null) {
			
			HouseholdDeparture householdDeparture = this.householdDepartures.peek();
			if (householdDeparture.getDepartureTime() <= time) {
				this.householdDepartures.poll();
				
				Id householdId = householdDeparture.getHouseholdId();
				Id facilityId = householdDeparture.getFacilityId();
				HouseholdInfo householdInfo = householdUtils.getHouseholdInfoMap().get(householdId);
				selectHouseholdMeetingPoint.selectRescueMeetingPoint(householdId);
				
				Map<Id, String> transportModes = getTransportModes(householdInfo, facilityId);			
				Id firstPersonWithCarId = null;
				for (Entry<Id, String> entry : transportModes.entrySet()) {
					if (entry.getValue().equals(TransportMode.car)) {
						firstPersonWithCarId = entry.getKey();
						break;
					}
				}
				
				/*
				 *  If we found an agent with a car available this agent
				 *  will be the driver (TransportMode.car), otherwise the
				 *  agent will be a passenger (TransportMode.ride).
				 *  TODO: add check for car capacity...
				 */
				if (firstPersonWithCarId != null) {
					List<Id> passengers = new ArrayList<Id>();
					for (Id agentId : householdInfo.getHousehold().getMemberIds()) {
						if (agentId == firstPersonWithCarId) {
							transportModeMapping.put(agentId, TransportMode.car);
						} else {
							transportModeMapping.put(agentId, PassengerEventsCreator.passengerTransportMode);
							passengers.add(agentId);
						}
						passengerEventsCreator.addDriverPassengersSet(firstPersonWithCarId, passengers);
					}
				}
				// all agents will walk
				else {
					for (Id agentId : householdInfo.getHousehold().getMemberIds()) {
						transportModeMapping.put(agentId, TransportMode.walk);
					}
				}

				// finally add agents to replanning set
				for (Id agentId : householdInfo.getHousehold().getMemberIds()) {
					set.add(agentMapping.get(agentId));
				}
			} else {
				break;
			}
		}
		
		return set;
	}

	/**
	 * @return The mapping between an agent and the transportMode that should be used.
	 */
	public Map<Id, String> getTransportModeMapping() {
		return this.transportModeMapping;
	}
	
	private Map<Id, String> getTransportModes(HouseholdInfo householdInfo, Id facilityId) {
		Map<Id, String> transportModes = new TreeMap<Id, String>();
		
		for (Id personId : householdInfo.getHousehold().getMemberIds()) {
			if (modeAvailabilityChecker.isCarAvailable(personId, facilityId)) transportModes.put(personId, TransportMode.car);
			else transportModes.put(personId, TransportMode.walk);
		}
				
		return transportModes;
	}
	
	@Override
	public void reset(int iteration) {
		agentMapping.clear();
		joinedHouseholds.clear();
		householdDepartures.clear();
		transportModeMapping.clear();
	}


	@Override
	public void handleEvent(HouseholdLeaveMeetingPointEvent event) {
		joinedHouseholds.remove(event.getHouseholdId());
		
		/*
		 * The household has been separated, therefore remove scheduled departure.
		 * 
		 * If the evacuation has started, households should leave their meeting point
		 * before their scheduled departure. 
		 */
		if (householdDepartures.remove(new HouseholdDeparture(event.getHouseholdId(), null, 0.0)) && event.getTime() > EvacuationConfig.evacuationTime) {
			log.warn("Household has left its meeting point before scheduled departure. Id " + event.getHouseholdId());
		}
	}

	@Override
	public void handleEvent(HouseholdEnterMeetingPointEvent event) {
		joinedHouseholds.add(event.getHouseholdId());
		
		boolean secureFacility = selectHouseholdMeetingPoint.isFacilitySecure(event.getFacilityId());

		/*
		 * If the household is located in the secure area, we don't have to do
		 * further replanning.
		 * 
		 * TODO: add a function to assume whether a household "feels" secure
		 * (depending on the distance to the boarder of the evacuation area)
		 */
		if (secureFacility) return;
		
		/*
		 * The household has joined and is going to evacuate united.
		 */
		else {
			/*
			 * Schedule the household departure. If the evacuation has not started yet,
			 * set the earliest possible departure time to the start of the evacuation.
			 * We don't want the households to evacuate earlier...
			 */
			double time;
			if (event.getTime() < EvacuationConfig.evacuationTime) {
				time = EvacuationConfig.evacuationTime;
			} else time = event.getTime();
			
			// TODO: use a function to estimate the departure time
			HouseholdDeparture householdDeparture = new HouseholdDeparture(event.getHouseholdId(), event.getFacilityId(), time + 600);
			
			// if there is an old entry: remove it first
			householdDepartures.remove(householdDeparture);
			householdDepartures.add(householdDeparture);			
		}
	}

	@Override
	public void handleEvent(HouseholdSetMeetingPointEvent event) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(HouseholdSeparatedEvent event) {
//		joinedHouseholds.remove(event.getHouseholdId());
//		
//		/*
//		 * The household has been separated, therefore remove scheduled departure.
//		 */
//		if (householdDepartures.remove(new HouseholdDeparture(event.getHouseholdId(), null, 0.0))) {
//			log.warn("Household has been separated before scheduled departure. Id " + event.getHouseholdId());
//		}
	}

	@Override
	public void handleEvent(HouseholdJoinedEvent event) {
//		joinedHouseholds.add(event.getHouseholdId());
//		
//		boolean secureFacility = selectHouseholdMeetingPoint.isFacilitySecure(event.getFacilityId());
//
//		/*
//		 * If the household is located in the secure area, we don't have to do
//		 * further replanning.
//		 * 
//		 * TODO: add a function to assume whether a household "feels" secure
//		 * (depending on the distance to the boarder of the evacuation area)
//		 */
//		if (secureFacility) return;
//		
//		/*
//		 * The household has joined and is going to evacuate united.
//		 */
//		else {
//			// TODO: use a function to estimate the departure time
//			HouseholdDeparture householdDeparture = new HouseholdDeparture(event.getHouseholdId(), event.getFacilityId(), event.getTime() + 600);
//			
//			// if there is an old entry: remove it first
//			householdDepartures.remove(householdDeparture);
//			householdDepartures.add(householdDeparture);			
//		}
	}
	
	/*
	 * Create a mapping between personIds and the agents in the mobsim.
	 * 
	 * Moreover ensure that the joinedHouseholds and householdDeparture
	 * data structures are filled properly. When the simulation starts,
	 * all households are joined at their home facility.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		QSim sim = (QSim) e.getQueueSimulation();

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
			agentMapping.put(withinDayAgent.getId(), withinDayAgent);				
		}
		
		for (HouseholdInfo householdInfo : this.householdUtils.getHouseholdInfoMap().values()) {
			
			/*
			 * We create a dummy event. 
			 * By using the evacuation time as event time we schedule the evacuation 
			 * of households that do not move until the evacuation starts. Otherwise they
			 * would be ignored.
			 */
			double time = EvacuationConfig.evacuationTime;
			Id householdId = householdInfo.getHousehold().getId();
//			Id linkId = null;	// we don't need this in in the handleEvent method
			Id facilityId = householdInfo.getMeetingPointId();
//			String eventType = "home";
//			HouseholdJoinedEvent event = new HouseholdJoinedEventImpl(time, householdId, linkId, facilityId, eventType);
//			this.handleEvent(event);
			HouseholdEnterMeetingPointEvent event = new HouseholdEnterMeetingPointEventImpl(time, householdId, facilityId);
			this.handleEvent(event);
		}
	}
	
	/*
	 * A datastructure to store households and their planned departures.
	 */
	private static class HouseholdDeparture {
		
		private final Id householdId;
		private final Id facilityId;
		private final double departureTime;
		
		public HouseholdDeparture(Id householdId, Id facilityId, double departureTime) {
			this.householdId = householdId;
			this.facilityId = facilityId;
			this.departureTime = departureTime;
		}
		
		public Id getHouseholdId() {
			return this.householdId;
		}
		
		public Id getFacilityId() {
			return this.facilityId;
		}
		
		public double getDepartureTime() {
			return this.departureTime;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof HouseholdDeparture) {
				return ((HouseholdDeparture) o).getHouseholdId().equals(householdId);
			}
			return false;
		}
	}
	
	private static class DepartureTimeComparator implements Comparator<HouseholdDeparture>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(HouseholdDeparture h1, HouseholdDeparture h2) {
			int cmp = Double.compare(h1.getDepartureTime(), h2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return h2.getHouseholdId().compareTo(h2.getHouseholdId());
			}
			return cmp;
		}
	}

}