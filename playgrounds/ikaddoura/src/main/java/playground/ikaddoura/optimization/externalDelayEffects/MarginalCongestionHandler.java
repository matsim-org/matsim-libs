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
package playground.ikaddoura.optimization.externalDelayEffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * TODO: Adjust for other modes than car and mixed modes.
 * TODO: Adjust for different effective cell sizes than 7.5 meters.
 * 
 * Not yet tested: combination of flow capacity and storage capacity constraints...
 * TODO: only throw flow capacity constraints if agent is not affected by storage capacity constraints!
 * 
 * When an agent ends an activity on link A, the agent is affected by the flow capacity of link A.
 * What about the storage capacity?
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, TransitDriverStartsEventHandler, AgentDepartureEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCongestionHandler.class);

	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	
	private final Map<Id, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfo>();

	public MarginalCongestionHandler(EventsManager events, ScenarioImpl scenario) {
		this.events = events;
		this.scenario = scenario;
		log.warn("Not tested.");
		
		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			throw new RuntimeException("Expecting a capacity period of 1h. Aborting...");
			// TODO: adjust for other capacity periods.
		}		
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
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
			// pt vehicle!
		} else {
			// car!
						
			boolean useLinkTravelTime = true;
			calculateCongestionEffect(event.getTime(), event.getLinkId(), event.getPersonId(), useLinkTravelTime);			
		
			// update link info
			if (this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2enterLeaveInfo() == null) {
				List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
				LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
				linkEnterLeaveInfo.setPersonId(event.getPersonId());
				linkEnterLeaveInfo.setLinkEnterTime(event.getTime());
				
				personId2enterLeaveInfo.add(linkEnterLeaveInfo);
				linkId2congestionInfo.get(event.getLinkId()).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
			} else {
				List<LinkEnterLeaveInfo> personId2enterLeaveInfo = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2enterLeaveInfo();
				LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
				linkEnterLeaveInfo.setPersonId(event.getPersonId());
				linkEnterLeaveInfo.setLinkEnterTime(event.getTime());
				
				personId2enterLeaveInfo.add(linkEnterLeaveInfo);
				linkId2congestionInfo.get(event.getLinkId()).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
			}
		}

	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (this.ptDriverIDs.contains(event.getPersonId())
				|| event.getLegMode().toString().equals(TransportMode.transit_walk.toString())
				|| event.getLegMode().toString().equals(TransportMode.walk.toString())
				|| event.getLegMode().toString().equals(TransportMode.pt.toString())){
			log.warn("Not tested for other modes than car.");
			// pt vehicle or transit walk or walk
		} else {
			// car!
			
			// ??? wenn storage capacity verringert --> analog zu linkEnterEvent
			
			boolean useLinkTravelTime = false; // agent was not entering that link before (departure!), use 1.0 second.
			calculateCongestionEffect(event.getTime(), event.getLinkId(), event.getPersonId(), useLinkTravelTime);			
		}
	}

	private void calculateCongestionEffect(double time, Id linkId, Id personId, boolean useLinkTravelTime) {
			
		if (this.linkId2congestionInfo.get(linkId) == null){
			// no one entered this link before
			
			// collect link informations
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			LinkCongestionInfo linkInfo = new LinkCongestionInfo();	
			linkInfo.setLinkId(link.getId());
			linkInfo.setFreeTravelTime(link.getLength() / link.getFreespeed());
			linkInfo.setMarginalDelayPerLeavingVehicle(link.getCapacity());
			linkInfo.setStorageCapacity( ( link.getLength() * link.getNumberOfLanes() ) / 7.5);
			
			// start tracking the delay caused by that first person entering and leaving that link
			double uncongestedLinkLeaveTime;
			if (useLinkTravelTime) {
				uncongestedLinkLeaveTime = Math.ceil(time + linkInfo.getFreeTravelTime() + 1.0);
			} else {
				uncongestedLinkLeaveTime = Math.ceil(time + 1.0);
			}
						
			List<PersonDelayInfo> personDelayInfos = new ArrayList<PersonDelayInfo>();
			PersonDelayInfo personDelayInfo = new PersonDelayInfo();
			personDelayInfo.setPersonId(personId);
			personDelayInfo.setLinkLeaveTime(uncongestedLinkLeaveTime);
			personDelayInfo.setDelay(0.);
			personDelayInfos.add(personDelayInfo);
			linkInfo.setPersonDelayInfos(personDelayInfos);
			this.linkId2congestionInfo.put(link.getId(), linkInfo);
		
		} else {
			// at least one an agent was leaving this link before, see if I will be delayed by agents previously leaving that link...
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(linkId);
			List<PersonDelayInfo> personDelayInfos = linkInfo.getPersonDelayInfos();
			
			double lastLeaveTime = 0.;
			for (PersonDelayInfo info : personDelayInfos){
				if (info.getLinkLeaveTime() > lastLeaveTime){
					lastLeaveTime = info.getLinkLeaveTime();
				}
			}
			
			double uncongestedLinkLeaveTime;
			if (useLinkTravelTime) {
				uncongestedLinkLeaveTime = Math.ceil(time + linkInfo.getFreeTravelTime() + 1.0);
			} else {
				uncongestedLinkLeaveTime = Math.ceil(time + 1.0);
			}
			double earliestLinkLeaveTime = Math.ceil(lastLeaveTime + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
			
			if (uncongestedLinkLeaveTime >= earliestLinkLeaveTime) {
				// not delayed
//				System.out.println(personId + " will not be delayed on link " + linkId + ".");

				// No (more) congestion on that link. Remove all informations about agents previously entering and leaving that link.
				personDelayInfos = new ArrayList<PersonDelayInfo>();
				// Start tracking the delay caused by that person entering and leaving that link.
				PersonDelayInfo personDelayInfo = new PersonDelayInfo();
				personDelayInfo.setPersonId(personId);
				personDelayInfo.setLinkLeaveTime(uncongestedLinkLeaveTime);
				personDelayInfo.setDelay(0.);
				personDelayInfos.add(personDelayInfo);
				linkInfo.setPersonDelayInfos(personDelayInfos);					
				
			} else {
				// delayed!
				double delay = earliestLinkLeaveTime - uncongestedLinkLeaveTime;
				System.out.println("---------------------------------------------------------------------------------------------------------------------");
				System.out.println(personId + " will be delayed on link " + linkId + ". Delay [sec]: " + delay);
				System.out.println("   Causing agents:");
			
				// See who was causing the delay on that link and throw delayEffects for the causing agents.
				List<PersonDelayInfo> reverseList = new ArrayList<PersonDelayInfo>();
				reverseList.addAll(personDelayInfos);
				Collections.reverse(reverseList);
				
				
				double delayToPayFor = delay;
				for (PersonDelayInfo info : reverseList){
					if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
						MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, info.getPersonId(), personId, linkInfo.getMarginalDelayPerLeavingVehicle_sec(), linkId);
						System.out.println("	Person " + info.getPersonId() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
						this.events.processEvent(congestionEvent);
												
						delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
					} else {
						if (delayToPayFor > 0) {
							MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, info.getPersonId(), personId, delayToPayFor, linkId);
							System.out.println("	Person " + info.getPersonId() + " --> Marginal delay: " + delayToPayFor);
							this.events.processEvent(congestionEvent);
							delayToPayFor = 0;
						}
					}
				}
				System.out.println("---------------------------------------------------------------------------------------------------------------------");
				
				if (delayToPayFor != 0.) {
					throw new RuntimeException("More congestion delays on that link than possible. Aborting...");
				}
								
				// Get current person delay informations of agents entering and leaving that link. 
				PersonDelayInfo personDelayInfo = new PersonDelayInfo();
				// Start tracking the delay caused by that person entering and leaving that link.
				personDelayInfo.setPersonId(personId);
				personDelayInfo.setLinkLeaveTime(earliestLinkLeaveTime);
				personDelayInfo.setDelay(delay);
				personDelayInfos.add(personDelayInfo);
				linkInfo.setPersonDelayInfos(personDelayInfos);
			
			}
		}		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
			// pt vehicle!
		} else {
			
			updateLinkInfo(event.getTime(), event.getPersonId(), event.getLinkId());
			
			List<PersonDelayInfo> personDelayInfos = this.linkId2congestionInfo.get(event.getLinkId()).getPersonDelayInfos();
			
			for (PersonDelayInfo personDelayinfo : personDelayInfos){
				
				if (personDelayinfo.getPersonId().toString().equals(event.getPersonId().toString())){
				
					if (personDelayinfo.getLinkLeaveTime() < event.getTime()) {
						
						double delay = event.getTime() - personDelayinfo.getLinkLeaveTime();
						System.out.println("---------------------------------------------------------------------------------------------------------------------");
						System.out.println(event.getPersonId().toString() + " is delayed due to storage capacity constraints on links behind link " + event.getLinkId() + ". Delay [sec]: " + delay);
						
						List<Id> followingLinkIDs = getFollowingLinkIDs(event.getPersonId(), event.getLinkId());
						double linkLeaveTime = personDelayinfo.getLinkLeaveTime();
						
						// get queue at the linkLeaveTime (when this agent wanted to leave but could not because of storage capacity constraints)
						List<Id> causingAgentIDs = new ArrayList<Id>();
						causingAgentIDs.addAll(getCausingAgentsCurrentLink(event.getLinkId(), linkLeaveTime));
						causingAgentIDs.addAll(getCausingAgentsFollowingLinks(followingLinkIDs, linkLeaveTime));
						
						// throw events
						for (Id id : causingAgentIDs){
							MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), id, event.getPersonId(), delay, event.getLinkId());
							this.events.processEvent(congestionEvent);
						}
						
						System.out.println(causingAgentIDs.size() + " agents in front of me (on the same link or other links downstream) were causing this delay.");
						System.out.println("Causing agents: " + causingAgentIDs.toString());
						System.out.println("---------------------------------------------------------------------------------------------------------------------");
						
					} else {
//						System.out.println(event.getPersonId().toString() + " is not delayed due to storage capacity constraints on links behind link " + event.getLinkId() + ".");
					}
				}
			}
		}
	}

	private List<Id> getCausingAgentsFollowingLinks(List<Id> followingLinkIDs, double linkLeaveTime) {
		List<Id> causingAgentIDs = new ArrayList<Id>();
		
		boolean congestedLink = true;
		for (Id linkId : followingLinkIDs) {
										
			if (congestedLink){	
				List<LinkEnterLeaveInfo> enterLeaveInfosForThisLink = this.linkId2congestionInfo.get(linkId).getPersonId2enterLeaveInfo();
				double storageCapacity_cars = this.linkId2congestionInfo.get(linkId).getStorageCapacity();
				List<Id> personsOnLink = new ArrayList<Id>();
				
				for (LinkEnterLeaveInfo info : enterLeaveInfosForThisLink) {
					if ((info.getLinkEnterTime() < linkLeaveTime && info.getLinkLeaveTime() > linkLeaveTime) || (info.getLinkEnterTime() < linkLeaveTime && info.getLinkLeaveTime() == 0)){
						personsOnLink.add(info.getPersonId());
					} else {
						// person was not on the link at the linkLeaveTime
					}
				}
				
				if (personsOnLink.size() == storageCapacity_cars) {
					causingAgentIDs.addAll(personsOnLink);
				} else if (personsOnLink.size() < storageCapacity_cars) {
					// link is not full
					congestedLink = false;
					
				} else if (personsOnLink.size() > storageCapacity_cars) {	
					throw new RuntimeException("More cars on link than storage capacity allows. Aborting...");
				}
			} else {
				// stop following possible congested links...
			}
		}
		return causingAgentIDs;
	}

	private List<Id> getCausingAgentsCurrentLink(Id linkId, double linkLeaveTime) {
		// search for agents in queue in front of me when I wanted to leave the link
		List<Id> causingAgentIDs = new ArrayList<Id>();
		List<LinkEnterLeaveInfo> enterLeaveInfosCurrentLink = this.linkId2congestionInfo.get(linkId).getPersonId2enterLeaveInfo();
		for (LinkEnterLeaveInfo infoCurrentLink : enterLeaveInfosCurrentLink) {
			if ((infoCurrentLink.getLinkEnterTime() < linkLeaveTime && infoCurrentLink.getLinkLeaveTime() > linkLeaveTime) || (infoCurrentLink.getLinkEnterTime() < linkLeaveTime && infoCurrentLink.getLinkLeaveTime() == 0)){
				causingAgentIDs.add(infoCurrentLink.getPersonId());
			} else {
				// person was not on the link at the linkLeaveTime
			}
		}
		return causingAgentIDs;
	}

	private List<Id> getFollowingLinkIDs(Id personId, Id linkId) {

		// get current route linkIDs
		List<Id> currentRoute = null;
		Plan selectedPlan = this.scenario.getPopulation().getPersons().get(personId).getSelectedPlan();
		for (PlanElement pE : selectedPlan.getPlanElements()) {
			if (pE instanceof Leg){
				Leg leg = (Leg) pE;
				if (leg.getMode().toString().equals(TransportMode.car)){
					List<Id> relevantLinkIDs = new ArrayList<Id>();
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					relevantLinkIDs.add(route.getStartLinkId());
					relevantLinkIDs.addAll(route.getLinkIds());
					if (relevantLinkIDs.contains(linkId)){
						// probably current route
						if (currentRoute == null){
							currentRoute = relevantLinkIDs;
						} else {
							// TODO: adjust code for more than one route per link Id.
							throw new RuntimeException("The current link Id appears in more than one route. Cannot identify current route. Aborting...");
						}
					}
				}
			}
		}
		
		// get all following linkIDs
		boolean linkAfterCurrentLink = false;
		List<Id> linkIDsAfterCurrentLink = new ArrayList<Id>();
		for (Id id : currentRoute){
			if (linkAfterCurrentLink){
				linkIDsAfterCurrentLink.add(id); // current linkID and all linkIDs after that one!
			}
			if (linkId.toString().equals(id.toString())){
				linkAfterCurrentLink = true;
			}
		}
		
		return linkIDsAfterCurrentLink;
	}

	private void updateLinkInfo(double time, Id personId, Id linkId) {
		
		if (this.linkId2congestionInfo.get(linkId).getPersonId2enterLeaveInfo() == null) {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkLeaveTime(time);
			
			personId2enterLeaveInfo.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
		
		} else {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = this.linkId2congestionInfo.get(linkId).getPersonId2enterLeaveInfo();
			for (LinkEnterLeaveInfo info : personId2enterLeaveInfo) {
				if (info.getPersonId().toString().equals(personId.toString())){
					if (info.getLinkLeaveTime() == 0.){
						info.setLinkLeaveTime(time);
					} 
				}
			}
		}
	}
}
