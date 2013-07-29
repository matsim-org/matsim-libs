/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToDropOffIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

/**
 * Identifies agents that are passengers in a vehicle which has as different
 * destination as they have. The leave the vehicle after it has left the affected
 * area.
 * 
 * So far, agents leave the vehicle at the very first link which is not affected.
 * Therefore, we only have to ensure that they enter only vehicles which have their
 * destination in the secure area. If we keep agents longer in a vehicle, we have to
 * ensure that they leave the vehicle if it arrives at its destination. Otherwise
 * they would wait their forever for another driver. 
 * 
 * @author cdobler
 */
public class AgentsToDropOffIdentifier extends DuringLegIdentifier implements LinkEnterEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler, MobsimInitializedListener {

	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	private final Map<Id, MobsimAgent> agents;
	private final Set<Id> carLegPerformingAgents;
	private final Set<Id> potentialDropOffVehicles;
	
	/*package*/ AgentsToDropOffIdentifier(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker,
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;

		this.agents = new HashMap<Id, MobsimAgent>();
		this.carLegPerformingAgents = new HashSet<Id>();
		this.potentialDropOffVehicles = new HashSet<Id>();
	}

	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		Set<PlanBasedWithinDayAgent> agentsToDropOff = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		Set<Id> driverFilterSet = new HashSet<Id>();
		Set<Id> agentsLeaveVehicle = new TreeSet<Id>();
		for (Id vehicleId : potentialDropOffVehicles) {
			QVehicle vehicle = (QVehicle) this.vehiclesTracker.getVehicle(vehicleId);
			agentsLeaveVehicle.clear();
			driverFilterSet.clear();
			
			/*
			 * If the link is very short (min travel time < 1 second), the vehicle is already 
			 * in the outgoing buffer and therefore cannot stop at the current link anymore.
			 * Probably "vehicle.getEarliestLinkExitTime() < time" would be also fine...
			 */
			if (vehicle.getEarliestLinkExitTime() <= time) {
				continue;
			}
			
			// identify passengers that have a different destination than the driver
			MobsimDriverAgent driver = vehicle.getDriver();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				if (!driver.getDestinationLinkId().equals(passenger.getDestinationLinkId())) {
					agentsLeaveVehicle.add(passenger.getId());
				}
			}
			
			if (agentsLeaveVehicle.size() == 0) continue;
			
			// Apply filter to driver
			driverFilterSet.add(driver.getId());
			this.applyFilters(driverFilterSet, time);
			if (driverFilterSet.size() == 0) continue;	// driver was removed by the filter(s), so perform no replanning
			
			// Apply filter to remove agents that should not be replanned.
			this.applyFilters(agentsLeaveVehicle, time);
			
			// add driver and remaining agents to replanning set
			agentsToDropOff.add((PlanBasedWithinDayAgent) driver);
			for (Id agentId : agentsLeaveVehicle) agentsToDropOff.add((PlanBasedWithinDayAgent) this.agents.get(agentId));
			
			/*
			 * Create a JointDeparture where the passenger(s) is(are) dropped off.
			 */
			Id linkId = driver.getCurrentLinkId();
			Id driverId = driver.getId();
			Set<Id> remainingPassengers = new LinkedHashSet<Id>();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				Id passengerId = passenger.getId();
				if (!agentsLeaveVehicle.contains(passengerId)) remainingPassengers.add(passengerId);
			}
			this.jointDepartureOrganizer.createJointDeparture(linkId, vehicleId, driverId, remainingPassengers);
		}
		potentialDropOffVehicles.clear();
		
		
		return agentsToDropOff;
	}

	@Override
	public void reset(int iteration) {
		this.carLegPerformingAgents.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getTime() < EvacuationConfig.evacuationTime) return;
		
		if (this.carLegPerformingAgents.contains(event.getPersonId())) {
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			boolean isAffected = this.coordAnalyzer.isLinkAffected(link);
			if (!isAffected) potentialDropOffVehicles.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.carLegPerformingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegPerformingAgents.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegPerformingAgents.add(event.getPersonId());
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();

		agents.clear();
		for (MobsimAgent agent : (sim).getAgents()) {
			agents.put(agent.getId(), agent);
		}
	}

}
