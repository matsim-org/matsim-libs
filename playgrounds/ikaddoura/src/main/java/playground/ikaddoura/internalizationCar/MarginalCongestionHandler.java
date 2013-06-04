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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * TODO: Adjust for other modes than car and mixed modes. (Adjust for different effective cell sizes than 7.5 meters.)
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
		
		if (this.scenario.getConfig().getQSimConfigGroup().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 not tested.");
		}
		
		if (this.scenario.getConfig().getQSimConfigGroup().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 not tested.");
		}
		
		if (this.scenario.getConfig().getQSimConfigGroup().isInsertingWaitingVehiclesBeforeDrivingVehicles() != true) {
			throw new RuntimeException("Expecting the qSim to insert waiting vehicles before driving vehicles. Aborting...");
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
	public void handleEvent(AgentStuckEvent event) {
		log.warn("Agent stuck event. No garantee for right calculation of marginal congestion effects: " + event.toString());
		throw new RuntimeException();
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
			
			checkQSimBehavior(event);
			clearTrackingMarginalDelays(event);
			calculateCongestion(event);
			trackMarginalDelay(event);
			
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getPersonId());
		}
	}
	
	// --------------------------------------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------------------------------------
	
	private void checkQSimBehavior(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
		double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			
		if (event.getTime() >= earliestLeaveTime){
			// expected			
			
		} else {
			Id nextLinkId = getNextLinkId(event.getPersonId(), event.getLinkId(), event.getTime());
			log.warn("Agent leaves link earlier than flow capacity would allow! Agent: " + event.getPersonId() + " // Link: " + event.getLinkId() + " // NextLink: " + nextLinkId + " // LeaveTime: " + event.getTime() + " // LastLeaveTime: " + lastLeavingFromThatLink + " // EarliestLeaveTime: " + earliestLeaveTime );
		}
	}

	private void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		double totalDelay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());

//		System.out.println(event.toString());
//		System.out.println("free travel time: " + linkInfo.getFreeTravelTime() + " // marginal flow delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
//		System.out.println("relevant agents (previously leaving the link): " + linkInfo.getLeavingAgents());
//		System.out.println("total delay: " + totalDelay);
		
		if (totalDelay == 0.) {
			// person was leaving that link without delay
		
		} else {

			double flowDelay = 0.;
			
			if (linkInfo.getLeavingAgents().size() == 0) {
				// no one leaving this link before
			} else {
				
				double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
				double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				double freeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());
				
//				System.out.println("earliestLeaveTime: " + earliestLeaveTime);
//				System.out.println("freeSpeedLeaveTime: " + freeSpeedLeaveTime);
				
				flowDelay =  earliestLeaveTime - freeSpeedLeaveTime;
		
			}
			
			double storageDelay = totalDelay - flowDelay;
			
//			System.out.println("flow delay: " + flowDelay);
//			System.out.println("storage delay: " + storageDelay);
			
			calculateFlowCongestion(flowDelay, event);
				
			if (storageDelay > 1.0) {
				// Allow for 1 sec difference due to rounding errors.

				if (this.allowForStorageCapacityConstraint) {
					if (this.calculateStorageCapacityConstraints) {
						// look who has to pay additionally for the left over delay due to the storage capacity constraint.
						calculateStorageCongestion(event.getTime(), event.getPersonId(), event.getLinkId(), storageDelay);
					} else {
						this.delayNotInternalized = this.delayNotInternalized + storageDelay;
						log.warn("Delay which is not internalized: " + this.delayNotInternalized);
					}
					
				} else {
					throw new RuntimeException("Delay due to storage capacity on link " + event.getLinkId() + ": " + storageDelay + ". Aborting...");
				}
					
			} else {
				// No delay resulting from storage capacity constraint.
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
		
		if (delayToPayFor > 1.) {
			// Allow for 1 sec difference due to rounding errors.
			log.warn("More flow delay than causing agents identified and charged for (remaining delay: " + delayToPayFor + " // agent: " + event.getPersonId() + " // time: " + event.getTime() + " // link: " + event.getLinkId() + ").");
		}
		
	}

	private void calculateStorageCongestion(double time, Id affectedAgent, Id linkId, double delay) {
			
		// For simplifying reasons: Find the agent who is in front of me right now (time) instead of (time - delay)
		
		Id causingAgent = null;
		
		if (causingAgent == null){
			// Get the last agent blocking the next link
			Id nextLinkId = getNextLinkId(affectedAgent, linkId, time);
			causingAgent = getCausingAgentNextLink(nextLinkId);
		}
		if (causingAgent == null){
			throw new RuntimeException("No agent identified who is causing the delay due to storage capacity. Aborting...");
		}
		
		MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "storageCapacity", causingAgent, affectedAgent, delay, linkId);
		this.events.processEvent(congestionEvent);
	}
	
	private Id getCausingAgentNextLink(Id nextLinkId) {
		Id causingAgent = null;
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(nextLinkId);
		
		double causingAgentFreeSpeedLeaveTime = Double.NEGATIVE_INFINITY;
		
		if ( (linkInfo.getAgentsOnLink().size() + 1) < linkInfo.getStorageCapacity_cars() ){
			log.warn("Number of agents on next link: " + linkInfo.getAgentsOnLink().size() + " /// storage capacity: " + linkInfo.getStorageCapacity_cars());
		}

		// all agents who are currently on that link
		for (Id id : linkInfo.getAgentsOnLink()){

			// get last agent in queue
			if (linkInfo.getPersonId2freeSpeedLeaveTime().get(id) > causingAgentFreeSpeedLeaveTime) {
				causingAgentFreeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(id);
				causingAgent = id;
			}
		}
		return causingAgent;
	}

	private Id getNextLinkId(Id affectedAgent, Id linkId, double time) {
		Id nextLinkId = null;
		
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
		
		nextLinkId = linkIDsAfterCurrentLink.get(0);
		return nextLinkId;
	}
	
	private void clearTrackingMarginalDelays(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		if (linkInfo.getLeavingAgents().size() == 0) {
			// no agent is being tracked for that link
			
		} else {
			// clear trackings of persons leaving that link previously
			double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
			double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			double freeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());
//			System.out.println("earliestLeaveTime: " + earliestLeaveTime);
//			System.out.println("freeSpeedLeaveTime: " + freeSpeedLeaveTime);
						
			if (freeSpeedLeaveTime > earliestLeaveTime + 1.0){
//				System.out.println("Flow congestion has disappeared on link " + event.getLinkId() + ". Delete agents leaving previously that link: " + linkInfo.getLeavingAgents().toString());
				linkInfo.getLeavingAgents().clear();
				linkInfo.getPersonId2linkLeaveTime().clear();
				
			}  else {
				
			}
		}
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
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.ceil(link.getLength() / link.getFreespeed()));
		
		double flowCapacity_hour = link.getCapacity() * this.scenario.getConfig().getQSimConfigGroup().getFlowCapFactor();
		double marginalDelay_sec = Math.floor((1 / (flowCapacity_hour / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
		
		int storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().getQSimConfigGroup().getStorageCapFactor() );
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);
		
		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}
	
	private double getLastLeavingTime(Map<Id, Double> personId2LinkLeaveTime) {
		
		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
	}
	
}
