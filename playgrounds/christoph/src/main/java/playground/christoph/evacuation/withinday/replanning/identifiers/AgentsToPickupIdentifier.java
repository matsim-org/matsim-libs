/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToPickupIdentifier.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel.PickupDecision;

/**
 * Identifies agents that perform a walk leg in the insecure area. They might be
 * picked up by a vehicle coming by.
 * 
 * Use a PriorityQueue that contains all AgentLeaveLink times. Whenever an agent
 * is going to leave a link it is checked whether there is a vehicle on the same
 * link available that has the same destination and available capacity.
 * 
 * @author cdobler
 */
public class AgentsToPickupIdentifier extends DuringLegAgentSelector {

	/*
	 * If true, agents are only picked up if their destination matches the drivers 
	 * destination. If false, agents are picked up and dropped of as soon as the vehicle 
	 * has left the affected area.
	 */
	private final static boolean destinationsMustMatch = false;
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final InformedAgentsTracker informedAgentsTracker;
	private final DecisionDataProvider decisionDataProvider;
	private final JointDepartureCoordinator jointDepartureCoordinator;
	
	private final Map<Id, JointDeparture> jointDepartures;
	
	/*package*/ AgentsToPickupIdentifier(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, 
			MobsimDataProvider mobsimDataProvider, EarliestLinkExitTimeProvider earliestLinkExitTimeProvider, 
			InformedAgentsTracker informedAgentsTracker, DecisionDataProvider decisionDataProvider, 
			JointDepartureOrganizer jointDepartureOrganizer, JointDepartureCoordinator jointDepartureCoordinator) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.earliestLinkExitTimeProvider = earliestLinkExitTimeProvider;
		this.informedAgentsTracker = informedAgentsTracker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.decisionDataProvider = decisionDataProvider;
		this.jointDepartureCoordinator = jointDepartureCoordinator;
		
		this.jointDepartures = new ConcurrentHashMap<Id, JointDeparture>();
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());
		
		/*
		 * Get all agents that could leave their current in this time step. If no agent
		 * can leave its current link, null is returned. As a result, the empty agentsToReplan
		 * set is returned.
		 */
		Set<Id<Person>> set = this.earliestLinkExitTimeProvider.getEarliestLinkExitTimesPerTimeStep(time);
		if (set == null) return agentsToReplan;
//		Set<Id> possibleLinkExitAgents = new HashSet<Id>(set);

//		// Apply filter to remove agents that should not be replanned.
//		this.applyFilters(possibleLinkExitAgents, time);
		
		Map<Id<Person>, PlannedDeparture> plannedDepartures = new HashMap<>();
		
//		for (Id agentId : possibleLinkExitAgents) {
		for (Id<Person> agentId : set) {
		
			// if the filters do not include the agent skip it
			if (!this.applyFilters(agentId, time)) continue;
		
			MobsimAgent passenger = this.mobsimDataProvider.getAgent(agentId);
			
			// check whether next activity type matches
			Activity acivity = (Activity) ((PlanAgent) passenger).getNextPlanElement();
			if (!acivity.getType().equals(EvacuationConstants.RESCUE_ACTIVITY)) continue;
			
			/*
			 * Check whether there are vehicle available on the link.
			 * If vehicles are found, check whether one of them has free capacity.
			 */
			Id<Link> linkId = passenger.getCurrentLinkId();
			Collection<MobsimVehicle> vehicles = this.mobsimDataProvider.getEnrouteVehiclesOnLink(linkId);
			for (MobsimVehicle vehicle : vehicles) {
				
				MobsimAgent driver = vehicle.getDriver();
				
				if (driver == null) {
					throw new RuntimeException("No driver for vehicle " + vehicle.getId().toString() +
							" was found at time " + time + "!");
				}
				
				/*
				 * Check whether the driver is already picking up an agent.
				 */
				if (agentsToReplan.contains(driver)) continue;
				else if (checkDriversNextActivity(driver)) continue;
				
				Id driverId = driver.getId();
				
				/*
				 * Check whether the driver has already scheduled a JointDeparture on the current link;
				 */
				if (this.jointDepartureCoordinator.isJointDepartureScheduled(driverId)) continue;
				
				/*
				 * Check whether the driver can still stop and perform a pickup activity on the
				 * current link.
				 */
				Double earliestLinkExitTime = this.earliestLinkExitTimeProvider.getEarliestLinkExitTime(driverId);
				if (earliestLinkExitTime == null) {
					throw new RuntimeException("No earliestLinkExitTime was found for driver " + driverId.toString() +
							" on link " + linkId.toString() + " at time " + time + "!");
				} else if (earliestLinkExitTime <= time) continue;
				
				/*
				 * Check whether the driver is already informed. If not, we ignore him since he
				 * might change its destination after being informed to a not secure location.
				 */
				if (!this.informedAgentsTracker.isAgentInformed(driverId)) continue;
				
				Id<Link> driversDestinationLinkId = driver.getDestinationLinkId();
				
				/*
				 * Check whether the driver will stop its trip on the current link. Skip such drivers
				 * since pickup up agents does not make sense.
				 */
				if (driversDestinationLinkId.equals(driver.getCurrentLinkId())) continue;
				
				// check whether the drivers destination is in a secure area
				Link driversDestinationLink = scenario.getNetwork().getLinks().get(driversDestinationLinkId);
				if (this.coordAnalyzer.isLinkAffected(driversDestinationLink)) continue;
				
				/*
				 * If only agents with exactly the same destination are picked up,
				 * check it. Otherwise pick up every agent and drop those with a different
				 * destination after the evacuation area has be left.
				 */
				if (destinationsMustMatch) {
					Id agentDestinationLinkId = passenger.getDestinationLinkId();
					if (!driversDestinationLinkId.equals(agentDestinationLinkId)) continue;						
				}
				
				int freeCapacity = this.vehiclesTracker.getFreeVehicleCapacity(vehicle.getId());
				
				/*
				 * If already other agents have reserved a seat in that vehicle, reduce
				 * the vehicle's available capacity.
				 */
				int reservedCapacity = this.vehiclesTracker.getReservedVehicleCapacity(vehicle.getId());
				
				int remainingCapacity = freeCapacity - reservedCapacity;
				
				/*
				 * Check whether free capacity is available in the vehicle. 
				 */
				if (remainingCapacity <= 0) continue;
				
				/*
				 * Check whether the driver would pick up the person.
				 */
				Person driverPerson = ((HasPerson) driver).getPerson();
				PickupDecision decision = checkPickup(((HasPerson) passenger).getPerson(), driverPerson);
				
				boolean pickup;
				if (decision == PickupDecision.ALWAYS) pickup = true;
				else if (decision == PickupDecision.NEVER) pickup = false;
				else if (decision == PickupDecision.IFSPACE) {
					/*
					 * Return true if after picking up the person at least one seat remains free,
					 * otherwise return false.
					 */
					if (remainingCapacity > 1) pickup = true;
					else pickup = false;
				} else {
					throw new RuntimeException("Undefined pickup agents behavior found: " + decision);
				}
				
				/*
				 * If the driver will not pick up the possible passenger, go on an try the next vehicle.
				 * Otherwise reserve seat in vehicle.
				 */
				if (!pickup) continue;
				else this.vehiclesTracker.reserveSeat(vehicle.getId());
				
				/*
				 * mark the agent as to be replanned and add an entry in the map which 
				 * connects the agent and the vehicle that will pick him up.
				 * Also replan the driver which has to perform a pickup activity.
				 */
				agentsToReplan.add(passenger);
				agentsToReplan.add(driver);
				
				/*
				 * Create a joint departure object
				 */
				PlannedDeparture plannedDeparture = plannedDepartures.get(driverId);
				if (plannedDeparture == null) {
					plannedDeparture = new PlannedDeparture();
					plannedDepartures.put(driverId, plannedDeparture);
					plannedDeparture.driverId = driverId;
					plannedDeparture.linkId = linkId;
					plannedDeparture.vehicleId = vehicle.getId();
					plannedDeparture.passengerIds = new LinkedHashSet<>();
					
					/*
					 * Check which existing passengers will stay in the vehicle
					 * and therefore have to be included in the new JointDeparture.
					 */
					Collection<? extends PassengerAgent> currentPassengers = vehicle.getPassengers();
					for (PassengerAgent currentPassenger : currentPassengers) {
						if (!currentPassenger.getDestinationLinkId().equals(linkId)) {								
							plannedDeparture.passengerIds.add(currentPassenger.getId());
						}
					}
				}
				
				// add the agent to be picked up
				plannedDeparture.passengerIds.add(agentId);
				
				// agent is being picked up, therefore stop searching
				break;
			}	// for possible vehicles that could pick up the agent
		}
		
		/*
		 * Create joint departures for all planned pickups
		 */
		for (PlannedDeparture pd : plannedDepartures.values()) {
			JointDeparture jointDeparture = this.jointDepartureOrganizer.createJointDeparture(pd.linkId, pd.vehicleId, pd.driverId, pd.passengerIds);
			this.jointDepartures.put(pd.driverId, jointDeparture);
			for (Id passengerId :pd.passengerIds) {
				this.jointDepartures.put(passengerId, jointDeparture);
			}
		}
		
		return agentsToReplan;
	}
	
	/*
	 * Checks the drivers next activity. If it is picking up or dropping off
	 * agents, we ignore that driver. Otherwise the logic "can the driver
	 * pick up another agent?" would become even more complex.
	 * Returns true if the next activity is from one of that types - then picking 
	 * up another agent is not possible on the agents current link.
	 */
	private boolean checkDriversNextActivity(MobsimAgent driver) {
		
		int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(driver);
		
		Activity nextActivity = (Activity) WithinDayAgentUtils.getModifiablePlan(driver).getPlanElements().get(currentPlanElementIndex + 1);
		if (nextActivity.getType().equals(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE) ||
				nextActivity.getType().equals(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE)) return true;
		else return false;
	}
	
	public JointDepartureOrganizer getJointDepartureOrganizer() {
		return this.jointDepartureOrganizer;
	}
	
	public JointDeparture getJointDeparture(Id agentId) {
		return this.jointDepartures.remove(agentId);
	}

	/*package*/ boolean isJointDepartureScheduled(Id agentId) {
		return this.jointDepartures.containsKey(agentId);
	}
	
	private PickupDecision checkPickup(Person passenger, Person driver) {
		
		if (EvacuationConfig.pickupAgents == EvacuationConfig.PickupAgentBehaviour.NEVER) return PickupDecision.NEVER;
		else if (EvacuationConfig.pickupAgents == EvacuationConfig.PickupAgentBehaviour.ALWAYS) return PickupDecision.ALWAYS;
		else if (EvacuationConfig.pickupAgents == EvacuationConfig.PickupAgentBehaviour.MODEL) {
			return this.decisionDataProvider.getPersonDecisionData(driver.getId()).getPickupDecision();
		}
		else {
			throw new RuntimeException("Unknown pickup agents behavior found: " + EvacuationConfig.pickupAgents);
		}
	}
	
	private static class PlannedDeparture {
		Id driverId;
		Id linkId;
		Id vehicleId;
		Set<Id<Person>> passengerIds;
	}

}