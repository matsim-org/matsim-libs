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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplanner;

/**
 * Identifies agents that perform a walk leg in the insecure area. They might be
 * picked up by a vehicle coming by.
 * 
 * Collect vehicles that have just entered a link in the previous time step. If
 * they have free capacity, they might pick up additional agents.
 * 
 * @author cdobler
 */
public class AgentsToPickupIdentifier extends DuringLegIdentifier implements LinkEnterEventHandler, 
		LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, 
		AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler,
		SimulationInitializedListener, SimulationAfterSimStepListener {
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final PersonalizableTravelTime walkTravelTime;

	/*
	 * Queue that contains information on when agents are going to 
	 * leave one link and enter another one.
	 */
	private final Queue<Tuple<Double, MobsimAgent>> agentsLeaveLinkQueue = new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TravelTimeComparator());
	
	private Map<Id, MobsimAgent> agents;
	private Map<Id, List<Id>> lastTimeStepLinkEnteredVehicles;
	private Map<Id, List<Id>> recentLinkEnteredVehicles;
	private final Map<Id, AtomicInteger> vehicleCapacities;
	private final Set<Id> walkLegPerformingAgents;
	private final Set<Id> insecureWalkLegPerformingAgents;
	private final Set<Id> enrouteDrivers;
	
	/*package*/ AgentsToPickupIdentifier(Scenario scenario, CoordAnalyzer coordAnalyzer,  VehiclesTracker vehiclesTracker,
			PersonalizableTravelTime walkTravelTime) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.walkTravelTime = walkTravelTime;
		
		this.enrouteDrivers = new HashSet<Id>();
		this.vehicleCapacities = new HashMap<Id, AtomicInteger>();
		this.walkLegPerformingAgents = new HashSet<Id>();
		this.insecureWalkLegPerformingAgents = new HashSet<Id>();
		
		this.lastTimeStepLinkEnteredVehicles = new HashMap<Id, List<Id>>();
		this.recentLinkEnteredVehicles = new HashMap<Id, List<Id>>();
	}
	
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		Set<PlanBasedWithinDayAgent> insecureLegPerformingAgents = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
//		for (Id agentId : this.insecureWalkLegPerformingAgents) {
//			insecureLegPerformingAgents.add((PlanBasedWithinDayAgent) this.agents.get(agentId));
//		}
		
		Tuple<Double, MobsimAgent> tuple = null;
		while ((tuple = agentsLeaveLinkQueue.peek()) != null) {
			if (tuple.getFirst() > time) {
				break;
			} else if (tuple.getFirst() < time) {
				agentsLeaveLinkQueue.poll();
			} else {
				agentsLeaveLinkQueue.poll();
				insecureLegPerformingAgents.add((PlanBasedWithinDayAgent) tuple.getSecond());
			}
		}
		
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());		

		/*
		 * Create a map with all vehicles that have just entered
		 * a new link and use it to check whether one of them 
		 * has free capacity.
		 */
		Map<Id, AtomicInteger> availableVehicles = new HashMap<Id, AtomicInteger>();
		for (List<Id> vehicleIds : lastTimeStepLinkEnteredVehicles.values()) {
			for (Id vehicleId : vehicleIds) {
				int capacity = this.vehicleCapacities.get(vehicleId).get();
				availableVehicles.put(vehicleId, new AtomicInteger(capacity));
			}
		}
		
		for (MobsimAgent personAgent : insecureLegPerformingAgents) {
			/*
			 * The agent wants to be picked up if its leg mode is walk and if its
			 * activity is not from type rescue.
			 */
			if (personAgent.getMode().equals(TransportMode.walk)) {
				
				// check whether next activity types match
				Activity acivity = (Activity)((PlanBasedWithinDayAgent) personAgent).getNextPlanElement();
				if (!acivity.getType().equals(JoinedHouseholdsReplanner.activityType)) continue;
				
				/*
				 * Check whether there are vehicle available on the link
				 */
//				List<Id> vehicleIds = lastTimeStepLinkEnteredVehicles.get(personAgent.getCurrentLinkId());
				List<Id> vehicleIds = vehiclesTracker.getEnrouteVehiclesOnLink(personAgent.getCurrentLinkId());
//				if (vehicleIds == null) continue;

				for (Id vehicleId : vehicleIds) {

					if (vehicleId.toString().equals("807518_veh1")) {
						System.out.println("Found!");
					}
					
					AtomicInteger capacity = availableVehicles.get(vehicleId);
					
					/*
					 * If the vehicle in on the link but has not entered it in the previous time step
					 * we ignore it and continue with the next vehicle.
					 */
					if (capacity == null) continue;
					else if (capacity.get() > 0) {
						
						/*
						 * Check whether the vehicle has the same destination
						 * as the agent has.
						 */
						Id vehicleDestinationLinkId = this.vehiclesTracker.getVehicleDestination(vehicleId);
						Id agentDestinationLinkId = ((ExperimentalBasicWithindayAgent) personAgent).getCurrentLeg().getRoute().getEndLinkId();
						if (!vehicleDestinationLinkId.equals(agentDestinationLinkId)) continue;
						
						/*
						 * decrement the remaining capacity by one
						 */
						capacity.decrementAndGet();
						
						/*
						 * mark the agent as to be replanned and
						 * add an entry in the map which connects
						 * the agent and the car that will pick him up.
						 */
						agentsToReplan.add((PlanBasedWithinDayAgent) personAgent);
						
						// inform vehiclesTracker
//						this.vehiclesTracker.addPassengerToVehicle(personAgent.getId(), vehicleId);
//						this.passengerVehiclesMap.put(personAgent.getId(), vehicleId);
						this.vehiclesTracker.addPlannedPickupVehicle(personAgent.getId(), vehicleId);
						break;
					}
				}
			}
		}
		return agentsToReplan;
	}
	
	@Override
	public void reset(int iteration) {
		this.insecureWalkLegPerformingAgents.clear();
		this.lastTimeStepLinkEnteredVehicles.clear();
		this.recentLinkEnteredVehicles.clear();
		this.walkLegPerformingAgents.clear();
		this.vehicleCapacities.clear();
		this.agentsLeaveLinkQueue.clear();
		this.enrouteDrivers.clear();
		
		Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			int capacity = vehicle.getType().getCapacity().getSeats();
			this.vehicleCapacities.put(vehicle.getId(), new AtomicInteger(capacity));
		}	
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (enrouteDrivers.contains(event.getPersonId())) {
			List<Id> list = recentLinkEnteredVehicles.get(event.getLinkId());
			if (list == null) {
				list = new ArrayList<Id>();
				recentLinkEnteredVehicles.put(event.getLinkId(), list);
			}
			list.add(event.getVehicleId());			
		}
		
		if (this.walkLegPerformingAgents.contains(event.getPersonId())) {
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			boolean affected = this.coordAnalyzer.isLinkAffected(link);
			if (affected) {
				this.insecureWalkLegPerformingAgents.add(event.getPersonId());

				/* 
				 * Check whether the agent ends its leg on the current link.
				 * If yes, skip the agent.
				 */
				MobsimAgent agent = this.agents.get(event.getPersonId());
				Leg leg = ((ExperimentalBasicWithindayAgent) agent).getCurrentLeg();
				Id destinationLinkId = leg.getRoute().getEndLinkId();
				if (destinationLinkId.equals(event.getLinkId())) return;
				
				/*
				 * Otherwise add the agent to the agentsLeaveLinkQueue.
				 */
				Person person = ((PersonDriverAgentImpl) agent).getPerson();
				this.walkTravelTime.setPerson(person);
				double travelTime = walkTravelTime.getLinkTravelTime(link, event.getTime());
				double departureTime = event.getTime() + travelTime;
				departureTime = Math.round(departureTime);
				this.agentsLeaveLinkQueue.add(new Tuple<Double, MobsimAgent>(departureTime, agent));
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		int capacity = this.vehicleCapacities.get(event.getVehicleId()).incrementAndGet();
		
		/*
		 * Check whether the vehicle has as many free seats as seats in total.
		 * If true, the vehicle has been parked and is not available anymore
		 * to pickup agents.
		 */
		Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
		int seats = vehicles.getVehicles().get(event.getVehicleId()).getType().getCapacity().getSeats();
		if (seats == capacity) {
			Id linkId = this.vehiclesTracker.getVehicleLinkId(event.getVehicleId());
			if (linkId == null) return;
			
			List<Id> vehicleIds = this.recentLinkEnteredVehicles.get(linkId);
			if (vehicleIds == null) return;
			else vehicleIds.remove(event.getVehicleId());			
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicleCapacities.get(event.getVehicleId()).decrementAndGet();
	}
	
	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.enrouteDrivers.remove(event.getPersonId());
		this.walkLegPerformingAgents.remove(event.getPersonId());
		this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.walk)) {
			this.walkLegPerformingAgents.remove(event.getPersonId());
			this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
		} else if (event.getLegMode().equals(TransportMode.car)) {			
			this.enrouteDrivers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.walk)) {
			this.walkLegPerformingAgents.add(event.getPersonId());
			
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			boolean affected = this.coordAnalyzer.isLinkAffected(link);
			if (affected) this.insecureWalkLegPerformingAgents.add(event.getPersonId());
		} else if (event.getLegMode().equals(TransportMode.car)) {
			this.enrouteDrivers.add(event.getPersonId());
		}
	}	
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();
		
		agents = new HashMap<Id, MobsimAgent>();
		for (MobsimAgent agent : (sim).getAgents()) {
			agents.put(agent.getId(), agent);
		}
	}
	
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		lastTimeStepLinkEnteredVehicles = recentLinkEnteredVehicles;
		recentLinkEnteredVehicles = new HashMap<Id, List<Id>>();
	}
	
	private static class TravelTimeComparator implements Comparator<Tuple<Double, MobsimAgent>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final Tuple<Double, MobsimAgent> o1, final Tuple<Double, MobsimAgent> o2) {
			int ret = o1.getFirst().compareTo(o2.getFirst()); // first compare time information
			if (ret == 0) {
				ret = o2.getSecond().getId().compareTo(o1.getSecond().getId()); // if they're equal, compare the Ids: the one with the larger Id should be first
			}
			return ret;
		}
	}
}
