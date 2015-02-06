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
package playground.ikaddoura.internalizationCar.old;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.congestion.events.CongestionEvent;

/**
 * This handler calculates car delays (due to flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. The marginal congestion event is thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 
 * In this version the causing agent for a delay due to the storage capacity is assumed to be the last agent who entered a downstream link.
 * 
 * TODO: Adjust for other modes than car and mixed modes. (Adjust for different effective cell sizes than 7.5 meters.)
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerV1 implements
	LinkEnterEventHandler,
	LinkLeaveEventHandler,
	TransitDriverStartsEventHandler,
	PersonDepartureEventHandler, 
	PersonArrivalEventHandler,
	PersonStuckEventHandler {
	
	private final static Logger log = Logger.getLogger(MarginalCongestionHandlerV1.class);
	
	private final boolean allowForStorageCapacityConstraint = true; // Runtime Exception if storage capacity active
	private final boolean calculateStorageCapacityConstraints = true;
	private double delayNotInternalized = 0.;

	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final Map<Id, LinkCongestionInfoV1> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfoV1>();
	
	public MarginalCongestionHandlerV1(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
				
		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			throw new RuntimeException("Expecting a capacity period of 1h. Aborting...");
		}
		
		if (this.scenario.getConfig().qsim().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 not tested.");
		}
		
		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 not tested.");
		}
		
		if (this.scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles() != true) {
			throw new RuntimeException("Expecting the qSim to insert waiting vehicles before driving vehicles. Aborting...");
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.delayNotInternalized = 0.;
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
	public void handleEvent(PersonStuckEvent event) {
//		log.warn("Agent stuck event. No garantee for right calculation of marginal congestion effects: " + event.toString());
		throw new RuntimeException("Agent stuck event. No garantee for right calculation of marginal congestion effects: " + event.toString());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
			
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
						
			LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
		}	
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());

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
						
			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());
			
			updateTrackingMarginalDelays1(event);
			calculateCongestion(event);
			updateTrackingMarginalDelays2(event);
			trackMarginalDelay(event);
			
			LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getPersonId());
		}
	}
	
	// ################################################################################################################################################################################

	private void updateTrackingMarginalDelays2(LinkLeaveEvent event) {
		LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		if (linkInfo.getLeavingAgents().size() == 0) {
			// no agent is being tracked for that link
			
		} else {
			// clear trackings of persons leaving that link previously
			double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
			double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

			if (event.getTime() > earliestLeaveTime + 1.0){
//				System.out.println("Flow congestion has disappeared on link " + event.getLinkId() + ". Delete agents leaving previously that link: " + linkInfo.getLeavingAgents().toString());
				linkInfo.getLeavingAgents().clear();
				linkInfo.getPersonId2linkLeaveTime().clear();
				
			}
		}
		
	}

	private void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		double totalDelay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getPersonId());
		
//		System.out.println(event.toString());
//		System.out.println("free travel time: " + linkInfo.getFreeTravelTime() + " // marginal flow delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
//		System.out.println("relevant agents (previously leaving the link): " + linkInfo.getLeavingAgents());
//		System.out.println("total delay: " + totalDelay);
//		
		
		if (totalDelay < -1.0) {
			throw new RuntimeException("Delay below -1.0 sec. Aborting...");
			
		} else if (totalDelay <= 0.) {
			// person was leaving that link without delay
			
		} else {
						
			double storageDelay = throwFlowCongestionEventsAndReturnStorageDelay(totalDelay, event);
			
			if (storageDelay == 0.) {
				// no storage delay
			
			} else if (storageDelay > 0.) {
				
				if (this.allowForStorageCapacityConstraint) {
					if (this.calculateStorageCapacityConstraints) {
						// look who has to pay additionally for the left over delay due to the storage capacity constraint.
						calculateStorageCongestion(event, storageDelay);
					} else {
						this.delayNotInternalized = this.delayNotInternalized + storageDelay;
						log.warn("Delay which is not internalized: " + this.delayNotInternalized);
					}
					
				} else {
					throw new RuntimeException("Delay due to storage capacity on link " + event.getLinkId() + ": " + storageDelay + ". Aborting...");
				}
			
			} else {
				log.warn("Oups, negative storage delay: " + storageDelay);
			}
			
		}
	}
	
	private double throwFlowCongestionEventsAndReturnStorageDelay(double totalDelay, LinkLeaveEvent event) {
		LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);
		
		double delayToPayFor = totalDelay;
		for (Id id : reverseList){
			if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
				
//				System.out.println("	Person " + id.toString() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec() + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "flowCapacity", id, event.getPersonId(), linkInfo.getMarginalDelayPerLeavingVehicle_sec(), event.getLinkId(), event.getTime());
				this.events.processEvent(congestionEvent);	
				
				delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				
			} else {
				if (delayToPayFor > 0) {
					
//					System.out.println("	Person " + id + " --> Marginal delay: " + delayToPayFor + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
					CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "flowCapacity", id, event.getPersonId(), delayToPayFor, event.getLinkId(), event.getTime());
					this.events.processEvent(congestionEvent);
					
					delayToPayFor = 0;
				}
			}
		}
		
		if (delayToPayFor == 1.) { // Maybe check if "< 1.0" is necessary
			log.warn("Remaining delay of 1.0 sec may result from rounding errors. Setting the remaining delay to 0.0 sec.");
			delayToPayFor = 0.;
		}
		return delayToPayFor;
	}

	private void calculateStorageCongestion(LinkLeaveEvent event, double remainingDelay) {
			
		// Find the last agent blocking the next link at the relevant time step.
		// Relevant time step: The free flow travel time.
			
		double relevantTimeStep = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2freeSpeedLeaveTime().get(event.getPersonId());
		
		Id causingAgent = null;
		
		List<Id> downstreamLinks = getDownStreamLinks(event.getLinkId());
		causingAgent = getCausingAgentFromDownstreamLinks(relevantTimeStep, downstreamLinks);
		
		if (causingAgent == null){
			throw new RuntimeException("No agent identified who is causing the delay due to storage capacity. Check downstream links with reached storage capacity. Aborting...");
		}
		
		CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "storageCapacity", causingAgent, event.getPersonId(), remainingDelay, event.getLinkId(), event.getTime());
		this.events.processEvent(congestionEvent);
	}
	
	private Id getCausingAgentFromDownstreamLinks(double relevantTimeStep, List<Id> downstreamLinks) {
		Id causingAgent = null;
		double lastLinkEnterTime = Double.NEGATIVE_INFINITY;

		for (Id linkId : downstreamLinks){
			LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(linkId);
			
			int agentsOnNextLink = 0;
			for (LinkEnterLeaveInfo info : linkInfo.getPersonEnterLeaveInfos()) {
				
				if ((info.getLinkEnterTime() <= relevantTimeStep && info.getLinkLeaveTime() > relevantTimeStep) || (info.getLinkEnterTime() <= relevantTimeStep && info.getLinkLeaveTime() == 0)){
					// person at relevant time (flowLeaveTime) on next link
					agentsOnNextLink++;
					
				} else {
					// person was not on the link at the linkLeaveTime
				}
			}
						
			if (agentsOnNextLink == linkInfo.getStorageCapacity_cars()){
				// storage capacity on link reached
				
				for (LinkEnterLeaveInfo info : linkInfo.getPersonEnterLeaveInfos()) {
					
					if ((info.getLinkEnterTime() <= relevantTimeStep && info.getLinkLeaveTime() > relevantTimeStep) || (info.getLinkEnterTime() <= relevantTimeStep && info.getLinkLeaveTime() == 0)){
						// person at relevant time (flowLeaveTime) on next link
						
						if (info.getLinkEnterTime() > lastLinkEnterTime) {
							// person entering this link after previously identified agents
							causingAgent = info.getPersonId();
							lastLinkEnterTime = info.getLinkEnterTime();
						} else {
							// person entering the link earlier, thus not relevant
						}
						
					} else {
						// person was not on the link at the linkLeaveTime
					}
				}
				
			} else {
				// storage capacity on link not reached, link not relevant
			}
		}
		
		return causingAgent;
	}

	private List<Id> getDownStreamLinks(Id linkId) {
		List<Id> downstreamLinks = new ArrayList<Id>();
		Link currentLink = this.scenario.getNetwork().getLinks().get(linkId);
		Id toNodeId = currentLink.getToNode().getId();
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if (link.getFromNode().getId().toString().equals(toNodeId.toString())) {
				downstreamLinks.add(link.getId());
			}
		}
		return downstreamLinks;
	}
	
	private void updateTrackingMarginalDelays1(LinkLeaveEvent event) {
		LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
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
		LinkCongestionInfoV1 linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
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
		LinkCongestionInfoV1 linkInfo = new LinkCongestionInfoV1();	

		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.ceil(link.getLength() / link.getFreespeed()));
		
		double flowCapacity_hour = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
		double marginalDelay_sec = Math.floor((1 / (flowCapacity_hour / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
		
		int storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);
		
		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}
	
	private void updateLinkInfo_agentEntersLink(double time, Id personId, Id linkId) {
		if (this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos() == null) {
			List<LinkEnterLeaveInfo> enterLeaveInfos = new ArrayList<LinkEnterLeaveInfo>();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkEnterTime(time);
			linkEnterLeaveInfo.setLinkLeaveTime(0.);
			
			enterLeaveInfos.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonEnterLeaveInfos(enterLeaveInfos);
		} else {
			List<LinkEnterLeaveInfo> enterLeaveInfos = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();			
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkEnterTime(time);
			linkEnterLeaveInfo.setLinkLeaveTime(0.);
			
			enterLeaveInfos.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonEnterLeaveInfos(enterLeaveInfos);
		}
	}

	private void updateLinkInfo_agentLeavesLink(double time, Id personId, Id linkId) {
		
		if (this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos() == null) {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkLeaveTime(time);
			
			personId2enterLeaveInfo.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonEnterLeaveInfos(personId2enterLeaveInfo);
		
		} else {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();
			for (LinkEnterLeaveInfo info : personId2enterLeaveInfo) {
				if (info.getPersonId().toString().equals(personId.toString())){
					if (info.getLinkLeaveTime() == 0.){
						// EnterLeaveInfo with not yet set leaving time
						info.setLinkLeaveTime(time);
					} else {
						// completed EnterLeaveInfo
					}
				}
			}
		}
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
