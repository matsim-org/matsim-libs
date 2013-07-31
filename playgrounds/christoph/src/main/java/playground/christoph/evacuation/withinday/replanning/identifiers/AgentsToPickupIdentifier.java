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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

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
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel.PickupDecision;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplanner;

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
public class AgentsToPickupIdentifier extends DuringLegIdentifier implements LinkEnterEventHandler, LinkLeaveEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler, MobsimInitializedListener {

	/*
	 * If true, agents are only picked up if their destination
	 * matches the drivers destination. If false, agents are picked up
	 * and dropped of as soon as the vehicle has left the affected
	 * area.
	 */
	private final static boolean destinationsMustMatch = false;
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final VehiclesTracker vehiclesTracker;
	private final TravelTime walkTravelTime;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	private final Map<Id, MobsimAgent> agents;
	private final Map<Id, Double> earliestLinkLeaveTime;
	private final Set<Id> carLegPerformingAgents;
	private final Set<Id> walkLegPerformingAgents;
	private final Set<Id> insecureWalkLegPerformingAgents;
	private final InformedAgentsTracker informedAgentsTracker;
	private final DecisionDataProvider decisionDataProvider;
	
	
	/*
	 * Queue that contains information on when agents are going to leave one
	 * link and enter another one.
	 */
	private final Queue<Tuple<Double, MobsimAgent>> agentsLeaveLinkQueue = new PriorityQueue<Tuple<Double, MobsimAgent>>(30, new TravelTimeComparator());

	/*package*/ AgentsToPickupIdentifier(Scenario scenario, CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, TravelTime walkTravelTime,
			InformedAgentsTracker informedAgentsTracker, DecisionDataProvider decisionDataProvider, JointDepartureOrganizer jointDepartureOrganizer) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.informedAgentsTracker = informedAgentsTracker;
		this.walkTravelTime = walkTravelTime;
		this.jointDepartureOrganizer = jointDepartureOrganizer;

		this.agents = new HashMap<Id, MobsimAgent>();
		this.earliestLinkLeaveTime = new HashMap<Id, Double>();
		this.carLegPerformingAgents = new HashSet<Id>();
		this.walkLegPerformingAgents = new HashSet<Id>();
		this.insecureWalkLegPerformingAgents = new HashSet<Id>();
		this.decisionDataProvider = decisionDataProvider;
	}

	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> insecureLegPerformingAgents = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());

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

		/*
		 * Apply filter to remove agents that should not be replanned.
		 * We need a workaround since applyFilters expects Ids and not Agents.
		 */
		Set<Id> agentIds = new HashSet<Id>();
		for (PlanBasedWithinDayAgent agent : insecureLegPerformingAgents) agentIds.add(agent.getId());
		this.applyFilters(agentIds, time);
		Iterator<PlanBasedWithinDayAgent> iter = insecureLegPerformingAgents.iterator();
		while(iter.hasNext()) {
			PlanBasedWithinDayAgent agent = iter.next();
			if (!agentIds.contains(agent.getId())) iter.remove();
		}
		
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		Map<Id, PlannedDeparture> plannedDepartures = new HashMap<Id, PlannedDeparture>();
		
		for (MobsimAgent personAgent : insecureLegPerformingAgents) {
			
			/*
			 * The agent wants to be picked up if its leg mode is walk and if
			 * its activity is from type rescue.
			 */
			if (personAgent.getMode().equals(TransportMode.walk)) {

				// check whether next activity types match
				Activity acivity = (Activity) ((PlanBasedWithinDayAgent) personAgent).getNextPlanElement();
				if (!acivity.getType().equals(JoinedHouseholdsReplanner.activityType)) continue;

				/*
				 * Check whether there are vehicle available on the link.
				 * If vehicles are found, check whether one of them has free capacity.
				 */
				Id linkId = personAgent.getCurrentLinkId();
				List<Id> vehicleIds = vehiclesTracker.getEnrouteVehiclesOnLink(linkId);
				for (Id vehicleId : vehicleIds) {
					MobsimDriverAgent driver = this.vehiclesTracker.getVehicleDriver(vehicleId);
					
					if (driver == null) {
						throw new RuntimeException("No driver for vehicle " + vehicleId.toString() +
								" was found in VehiclesTracker at time " + time + "!");
					}
					Id driverId = driver.getId();
					
					/*
					 * Check whether the driver is already informed. If not, we ignore him since he
					 * might change its destination after being informed to a not secure location.
					 */
					if (!this.informedAgentsTracker.isAgentInformed(driverId)) continue;
					
					Id driversDestinationLinkId = driver.getDestinationLinkId();

					// check whether the drivers destination is in a secure area
					Link driversDestinationLink = scenario.getNetwork().getLinks().get(driversDestinationLinkId);
					if (this.coordAnalyzer.isLinkAffected(driversDestinationLink)) continue;
					
					/*
					 * If only agents with exactly the same destination are picked up,
					 * check it. Otherwise pick up every agent and drop those with a different
					 * destination after the evacuation area has be left.
					 */
					if (destinationsMustMatch) {
						Id agentDestinationLinkId = personAgent.getDestinationLinkId();
						if (!driversDestinationLinkId.equals(agentDestinationLinkId)) continue;						
					}
					
					/*
					 * If the vehicle could leave the link before the agent has entered it skip 
					 * the vehicle and try the next one.
					 * Add two seconds because the walk agent has to stop and perform the pickup
					 * activity which takes some time.
					 */
					Double leaveLinkTime = this.earliestLinkLeaveTime.get(driverId);
					if (leaveLinkTime == null) {
						throw new RuntimeException("No leaveLinkTime was found for driver " + driverId.toString() +
								" at time " + time + "!");
					}
					if (time + 2 > leaveLinkTime) continue;
					
					int freeCapacity = this.vehiclesTracker.getFreeVehicleCapacity(vehicleId);
					
					/*
					 * If already other agents have reserved a seat in that vehicle, reduce
					 * the vehicle's available capacity.
					 */
					int reservedCapacity = this.vehiclesTracker.getReservedVehicleCapacity(vehicleId);
					
					int remainingCapacity = freeCapacity - reservedCapacity;
					
					/*
					 * Check whether free capacity is available in the vehicle. 
					 */
					if (remainingCapacity <= 0) continue;
					
					/*
					 * Check whether the driver would pick up the person.
					 */
					Person driverPerson = ((HasPerson) driver).getPerson();
					PickupDecision decision = checkPickup(((HasPerson) personAgent).getPerson(), driverPerson);
					
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
					else this.vehiclesTracker.reserveSeat(vehicleId);
					
					/*
					 * mark the agent as to be replanned and add an entry in the map which 
					 * connects the agent and the vehicle that will pick him up.
					 * Also replan the driver which has to perform a pickup activity.
					 */
					agentsToReplan.add((PlanBasedWithinDayAgent) personAgent);
					agentsToReplan.add((PlanBasedWithinDayAgent) driver);
					
					/*
					 * Create a joint departure object
					 */
					PlannedDeparture plannedDeparture = plannedDepartures.get(driverId);
					if (plannedDeparture == null) {
						plannedDeparture = new PlannedDeparture();
						plannedDepartures.put(driverId, plannedDeparture);
						plannedDeparture.driverId = driverId;
						plannedDeparture.linkId = linkId;
						plannedDeparture.vehicleId = vehicleId;
						plannedDeparture.passengerIds = new LinkedHashSet<Id>();
						
						/*
						 * Check which existing passengers will stay in the vehicle
						 * and therefore have to be included in the new JointDeparture.
						 */
						Collection<? extends PassengerAgent> currentPassengers = this.vehiclesTracker.getVehiclePassengers(vehicleId);
						for (PassengerAgent passenger : currentPassengers) {
							if (!passenger.getDestinationLinkId().equals(linkId)) {								
								plannedDeparture.passengerIds.add(passenger.getId());
							}
						}
					}
					
					// add the agent to be picked up
					plannedDeparture.passengerIds.add(personAgent.getId());
					
					// agent is being picked up, therefore stop searching
					break;
				}	// for possible vehicles that could pick up the agent
			}
		}
		
		/*
		 * Create joint departures for all planned pickups
		 */
		for (PlannedDeparture pd : plannedDepartures.values()) {
			this.jointDepartureOrganizer.createJointDeparture(pd.linkId, pd.vehicleId, pd.driverId, pd.passengerIds);
		}
		
		return agentsToReplan;
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

	@Override
	public void reset(int iteration) {
		this.agentsLeaveLinkQueue.clear();
		this.carLegPerformingAgents.clear();
		this.walkLegPerformingAgents.clear();
		this.insecureWalkLegPerformingAgents.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.walkLegPerformingAgents.contains(event.getPersonId())) {
			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
			boolean affected = this.coordAnalyzer.isLinkAffected(link);
			if (affected) {
				this.insecureWalkLegPerformingAgents.add(event.getPersonId());

				/*
				 * Check whether the agent ends its leg on the current link. If
				 * yes, skip the agent.
				 */
				MobsimAgent agent = this.agents.get(event.getPersonId());
				Leg leg = ((ExperimentalBasicWithindayAgent) agent).getCurrentLeg();
				Id destinationLinkId = leg.getRoute().getEndLinkId();
				if (destinationLinkId.equals(event.getLinkId())) return;

				/*
				 * Otherwise add the agent to the agentsLeaveLinkQueue.
				 */
				Person person = ((PersonDriverAgentImpl) agent).getPerson();
				double travelTime = walkTravelTime.getLinkTravelTime(link, event.getTime(), person, null);
				double departureTime = event.getTime() + travelTime;
				departureTime = Math.floor(departureTime);
				this.agentsLeaveLinkQueue.add(new Tuple<Double, MobsimAgent>(departureTime, agent));
			}
		} else if (this.carLegPerformingAgents.contains(event.getPersonId())) {
			
//			Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
//			double minTravelTime = Math.floor(link.getLength() / link.getFreespeed(event.getTime()));
//			this.earliestLinkLeaveTime.put(event.getPersonId(), event.getTime() + minTravelTime);
			QVehicle vehicle = (QVehicle) this.vehiclesTracker.getVehicle(event.getVehicleId());
			vehicle.getEarliestLinkExitTime();
			this.earliestLinkLeaveTime.put(event.getPersonId(), vehicle.getEarliestLinkExitTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.earliestLinkLeaveTime.remove(event.getPersonId());
		this.carLegPerformingAgents.remove(event.getPersonId());
		this.walkLegPerformingAgents.remove(event.getPersonId());
		this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.walk)) {
			this.walkLegPerformingAgents.remove(event.getPersonId());
			this.insecureWalkLegPerformingAgents.remove(event.getPersonId());
		} else if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegPerformingAgents.remove(event.getPersonId());
			this.earliestLinkLeaveTime.remove(event.getPersonId());
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
			this.carLegPerformingAgents.add(event.getPersonId());

			// the agent might leave the current link immediately
			this.earliestLinkLeaveTime.put(event.getPersonId(), event.getTime());
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

	private static class PlannedDeparture {
		Id driverId;
		Id linkId;
		Id vehicleId;
		Set<Id> passengerIds;
	}
	
	private static class TravelTimeComparator implements Comparator<Tuple<Double, MobsimAgent>>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Tuple<Double, MobsimAgent> o1, final Tuple<Double, MobsimAgent> o2) {
			// first compare time information
			int ret = o1.getFirst().compareTo(o2.getFirst());
			if (ret == 0) {
				// if they're equal, compare the Ids: the one with the larger Id should be first
				ret = o2.getSecond().getId().compareTo(o1.getSecond().getId()); 
			}
			return ret;
		}
	}
}
