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

/**
 * 
 */
package playground.ikaddoura.internalizationCar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * TODO: Adjust for other modes than car and mixed modes. (Adjust for different effective cell sizes than 7.5 meters.)
 * TODO: Adjust for other flow / storage capacity factors than 1.0.
 * only working correctly if	<param name="insertingWaitingVehiclesBeforeDrivingVehicles" value="true" />
 * TODO: Adjust for stucking agents.
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, TransitDriverStartsEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCongestionHandler.class);
	
	private final boolean allowForStorageCapacityConstraint = true; // Runtime Exception if storage capacity active
	private final boolean calculateStorageCapacityConstraints = true;
	private double delayNotInternalized = 0.;

	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final Map<Id, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfo>();
	
	public MarginalCongestionHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
				
		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			throw new RuntimeException("Expecting a capacity period of 1h. Aborting...");
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
				
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
			
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getAgentsOnLink().add(event.getPersonId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);

		} else {			
			log.warn("Not tested for other modes than car.");
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
		
		} else {
			// car!
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
			
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getAgentsOnLink().add(event.getPersonId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + linkInfo.getFreeTravelTime());
		}	
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getAgentsOnLink().remove(event.getPersonId());

		} else {			
			log.warn("Not tested for other modes than car.");
		}
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
		
		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one left this link before
				collectLinkInfos(event.getLinkId());
			}
			
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getAgentsOnLink().remove(event.getPersonId());
			
			calculateCongestion(event);
			clearTrackingMarginalDelays(event);
			trackMarginalDelay(event);
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getPersonId());
		}
	}

	private void clearTrackingMarginalDelays(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		// check: clear trackings of persons leaving that link previously?
		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id id : linkInfo.getPersonId2linkLeaveTime().keySet()){
			if (linkInfo.getPersonId2linkLeaveTime().get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = linkInfo.getPersonId2linkLeaveTime().get(id);
			}
		}
		
//		System.out.println("free speed leave time: " + linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId()) + " // last leaving time from that link: " + lastLeavingFromThatLink);
//		System.out.println("--> earliest leave time: " + (lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec()));
		
		if (event.getTime() > (lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec()) + 1.0){
			// congestion due to flow capacity constraints disappeared
			linkInfo.getLeavingAgents().clear();
			linkInfo.getPersonId2linkLeaveTime().clear();
		
		} else if ( (event.getTime() == (lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec()) ) || (event.getTime() == (lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec()) + 1.0 )){
			// More or less the expected qsim behavior.
			// Since there are rounding errors allowing for 1 sec difference...
			
		} else {
			if (event.getTime() == (lastLeavingFromThatLink + 1.0 )){
				log.warn("Agent " + event.getPersonId() + " is leaving link " + event.getLinkId() + " one second after previous agent at time step " + event.getTime() );
			} else {
				throw new RuntimeException("Agent " + event.getPersonId() + " is leaving link before flow capacity allows. Aborting...");			
			}
		}
	}

	private void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		double totalDelay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());
		
		System.out.println("************** leaving link event ******************");
		System.out.println(event.toString());
		System.out.println("free travel time: " + linkInfo.getFreeTravelTime() + " // marginal flow delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
		System.out.println("agents previously leaving the link: " + linkInfo.getLeavingAgents());
		System.out.println("total delay: " + totalDelay);
		
		if (totalDelay == 0.) {
			// person was leaving that link without delay
		
		} else {
			
//			System.out.println(event.getPersonId() + " is delayed on link " + event.getLinkId() + ". Delay [sec]: " + delay);
//			System.out.println("   Agents causing a delay due to flow capacity: " + linkInfo.getLeavingAgents().toString());

			double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
			for (Id id : linkInfo.getPersonId2linkLeaveTime().keySet()){
				if (linkInfo.getPersonId2linkLeaveTime().get(id) > lastLeavingFromThatLink) {
					lastLeavingFromThatLink = linkInfo.getPersonId2linkLeaveTime().get(id);
				}
			}
			
			double earliestLinkLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			double flowDelay = earliestLinkLeaveTime - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());			
			double storageDelay = totalDelay - flowDelay;
			
			System.out.println("flow delay: " + flowDelay);
			System.out.println("storage delay: " + storageDelay);
			
			calculateFlowCongestion(flowDelay, event);
			
			if (storageDelay > 1.0) {
				// Allow for 1 sec difference due to rounding errors.
				// For part of the delay no causing agent identified when only considering the flow capacity.
				// Therefore: The left over delay has to result from storage capacity constraints.

				if (this.allowForStorageCapacityConstraint) {
					if (this.calculateStorageCapacityConstraints) {
						// look who has to pay additionally for the left over delay due to the storage capacity constraint.
						calculateStorageCongestion(event.getTime(), event.getPersonId(), event.getLinkId(), storageDelay);
					} else {
						this.delayNotInternalized = this.delayNotInternalized + storageDelay;
						System.out.println(this.delayNotInternalized);
					}
				
				} else {
					throw new RuntimeException("More congestion delays on that link " + event.getLinkId() + " than causing agents are charged for (remaining delay: " + storageDelay + "). Probably because of storage capacity constraints. Aborting...");
				}
				
			} else {
				// No delay resulting from storage capacity constraints.
			}
		}
		
	}
	
	private void calculateFlowCongestion(double flowDelay, LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);
		
		double delayToPayFor = flowDelay;
		for (Id id : reverseList){
			if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {

//				System.out.println("	Person " + id.toString() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec() + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
				MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowCapacity", id, event.getPersonId(), linkInfo.getMarginalDelayPerLeavingVehicle_sec(), event.getLinkId());
				this.events.processEvent(congestionEvent);	
				
				delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				
			} else {
				if (delayToPayFor > 0) {
					
//					System.out.println("	Person " + id + " --> Marginal delay: " + delayToPayFor + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
					MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowCapacity", id, event.getPersonId(), delayToPayFor, event.getLinkId());
					this.events.processEvent(congestionEvent);
					
					delayToPayFor = 0;
				}
			}
		}
		
	}

	private void calculateStorageCongestion(double time, Id affectedAgent, Id linkId, double delay) {
		
//		System.out.println(affectedAgent.toString() + " is delayed due to storage capacity. Delay [sec]: " + delay);
	
		double leaveTimeFlowCapacityConstraint = time - delay;
//		System.out.println("Agent leave time due to flow capacity constaint: " + leaveTimeFlowCapacityConstraint);
		// Find the agent who was causing the delay at this time
		// For simplifying reasons: Find the agent who is in front of me right now. (For a corridor this should be ok!) 
		
		Id causingAgent = null;
		causingAgent = getCausingAgentSameLink(linkId, affectedAgent);

		if (causingAgent == null){
			// no agent on this link identified, get last agent in queue of next link
//			System.out.println("No agent in queue in front of me on this link. Looking on next link...");
			Id nextLinkId = getNextLinkId(affectedAgent, linkId, time);
//			System.out.println("Next link Id: " + nextLinkId);
			causingAgent = getCausingAgentNextLink(nextLinkId);
		}
		if (causingAgent == null){
			throw new RuntimeException("No agent identified who is causing the delay. Aborting...");
		}
		
//		System.out.println("Causing agent: " + causingAgent);
		
		// throw delay effect
		MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "storageCapacity", causingAgent, affectedAgent, delay, linkId);
		this.events.processEvent(congestionEvent);
	}
	
	private Id getCausingAgentNextLink(Id nextLinkId) {
		Id causingAgent = null;
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(nextLinkId);
		
		double causingAgentFreeSpeedLeaveTime = Double.NEGATIVE_INFINITY;
		
//		System.out.println("Agents on next link: " + linkInfo.getAgentsOnLink().toString());
		// Get all agents who are currently on that link
		for (Id id : linkInfo.getAgentsOnLink()){

			if (linkInfo.getPersonId2freeSpeedLeaveTime().get(id) > causingAgentFreeSpeedLeaveTime) {
				// closer in queue
				causingAgentFreeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(id);
				causingAgent = id;
			}
		}

		return causingAgent;
	}

	private Id getCausingAgentSameLink(Id linkId, Id affectedAgent) {
		Id causingAgent = null;
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(linkId);
		
		double affectedAgentFreeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(affectedAgent);
		double causingAgentFreeSpeedLeaveTime = Double.POSITIVE_INFINITY;
		
		// Get all agents who are currently on that link
		for (Id id : linkInfo.getAgentsOnLink()){
			// See if this agent is supposed to leave the link before me
			if (linkInfo.getPersonId2freeSpeedLeaveTime().get(id) > affectedAgentFreeSpeedLeaveTime) {
				// agent behind affected agent
			} else {
				// agent before affected agent
				if (linkInfo.getPersonId2freeSpeedLeaveTime().get(id) < causingAgentFreeSpeedLeaveTime) {
					// closer in queue
					causingAgentFreeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(id);
					causingAgent = id;
				}
			}
		}

		return causingAgent;
	}

	private Id getNextLinkId(Id affectedAgent, Id linkId, double time) {
		Id nextLinkId = null;
		
		// current route link IDs
		List<Id> currentRouteLinkIDs = null;
		
		Plan selectedPlan = this.scenario.getPopulation().getPersons().get(affectedAgent).getSelectedPlan();
		for (PlanElement pE : selectedPlan.getPlanElements()) {
			if (pE instanceof Activity){
				Activity act = (Activity) pE;
				if (act.getEndTime() < 0.) {
					// act has no endtime
					log.warn("Activity without endtime.");
					
				} else if (time >= act.getEndTime()) {
					int nextLegIndex = selectedPlan.getPlanElements().indexOf(pE) + 1;
					if (selectedPlan.getPlanElements().size() <= nextLegIndex) {
						// last activity
					} else {
						if (selectedPlan.getPlanElements().get(nextLegIndex) instanceof Leg) {
							Leg leg = (Leg) selectedPlan.getPlanElements().get(nextLegIndex);
							List<Id> linkIDs = new ArrayList<Id>();
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							linkIDs.add(route.getStartLinkId());
							linkIDs.addAll(route.getLinkIds());
							linkIDs.add(route.getEndLinkId()); // assuming this link to be the last link where the storage capacity plays a role.
							if (linkIDs.contains(linkId)){
								// probably current route
								currentRouteLinkIDs = linkIDs;
							}
							
						} else {
							throw new RuntimeException("Plan element behind activity not instance of Leg. Aborting...");
						}
					}
				}
			}
		}
//		System.out.println(currentRouteLinkIDs.toString());
		
		// get all following linkIDs
		boolean linkAfterCurrentLink = false;
		List<Id> linkIDsAfterCurrentLink = new ArrayList<Id>();
		for (Id id : currentRouteLinkIDs){
			if (linkAfterCurrentLink){
				linkIDsAfterCurrentLink.add(id);
			}
			if (linkId.toString().equals(id.toString())){
				linkAfterCurrentLink = true;
			}
		}
		
//		System.out.println("Following link IDs: " + linkIDsAfterCurrentLink.toString());
		nextLinkId = linkIDsAfterCurrentLink.get(0);
//		System.out.println("Next link ID: " + nextLinkId.toString());
		return nextLinkId;
	}
	
	private void trackMarginalDelay(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		// start tracking delays caused by that agent leaving the link	
		if (linkInfo.getLeavingAgents().contains(event.getPersonId())){
			throw new RuntimeException(" Person already in List (leavingAgents). Aborting...");
		}
		if (linkInfo.getPersonId2linkLeaveTime().containsKey(event.getPersonId())){
			throw new RuntimeException(" Person already in Map (personId2linkLeaveTime). Aborting...");
		}
		linkInfo.getLeavingAgents().add(event.getPersonId());
		linkInfo.getPersonId2linkLeaveTime().put(event.getPersonId(), event.getTime());
	}

	private void collectLinkInfos(Id linkId) {
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.ceil(link.getLength() / link.getFreespeed()));
		linkInfo.setMarginalDelayPerLeavingVehicle(link.getCapacity());
		int storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) / 7.5));
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);
		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		throw new RuntimeException("Congestion calculation can't yet handle AgentStuckEvents. Aborting...");
	}
	
}
