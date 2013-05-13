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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * TODO: Adjust for other modes than car and mixed modes. (Adjust for different effective cell sizes than 7.5 meters.)
 * 
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, TransitDriverStartsEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCongestionHandler.class);

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
			// TODO: adjust for other capacity periods.
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
				// no one entered this link before
				collectLinkInfos(event.getLinkId());
			}
			
			calculateFlowCongestion(event.getTime(), event.getLinkId(), event.getPersonId(), false);
			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
						
		} else {			
			log.warn("Not tested for other modes than car.");
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());
			
		} else {			
			log.warn("Not tested for other modes than car.");
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
			// pt vehicle!
		
		} else {
			// car!
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered this link before
				collectLinkInfos(event.getLinkId());
			}
			
			calculateFlowCongestion(event.getTime(), event.getLinkId(), event.getPersonId(), true);				
			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
		}
	
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Not tested for pt.");
			// pt vehicle!
		
		} else {
			// car!
			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());
			calculateStorageCongestion(event.getTime(), event.getPersonId(), event.getLinkId());
		}
	}

	private void calculateFlowCongestion(double time, Id linkId, Id personId, boolean useLinkTravelTime) {
					
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(linkId);
		
		// calculate link leave time without flow capacity restrictions (= uncongested link leave time)
		double uncongestedLinkLeaveTime;
		if (useLinkTravelTime) {
			uncongestedLinkLeaveTime = Math.ceil(time + linkInfo.getFreeTravelTime() + 1.0);
		} else {
			// agent was not entering that link before (departure), use 1.0 second.
			uncongestedLinkLeaveTime = Math.ceil(time + 1.0);
		}

		List<PersonDelayInfo> personDelayInfos = linkInfo.getPersonDelayInfos(); // why not null?
		
		// calculate link leave time with flow capacity restrictions (= earliest link leave time)
		double earliestLinkLeaveTime = uncongestedLinkLeaveTime;
		if (personDelayInfos.size() > 0){
			double lastLeaveTime = 0.;
			for (PersonDelayInfo info : personDelayInfos){
				if (info.getLinkLeaveTime() > lastLeaveTime){
					lastLeaveTime = info.getLinkLeaveTime();
				}
			}
			earliestLinkLeaveTime = Math.ceil(lastLeaveTime + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
		}		
		
		if (uncongestedLinkLeaveTime >= earliestLinkLeaveTime) {
			// agent is not delayed

			// No (more) flow congestion on that link. Delete all informations about agents previously entering and leaving that link.
			personDelayInfos.clear();
			
			// Start tracking the delay caused by that person entering and leaving that link.
			PersonDelayInfo personDelayInfo = new PersonDelayInfo();
			personDelayInfo.setPersonId(personId);
			personDelayInfo.setLinkLeaveTime(uncongestedLinkLeaveTime);
			personDelayInfo.setDelay(0.);
			personDelayInfos.add(personDelayInfo);
			linkInfo.setPersonDelayInfos(personDelayInfos);
						
		} else {
			// agent is delayed
			double delay = earliestLinkLeaveTime - uncongestedLinkLeaveTime;
			System.out.println("---------------------------------------------------------------------------------------------------------------------");
			System.out.println(personId + " may be delayed on link " + linkId + " due to flow capacity constraints. Delay [sec]: " + delay);
			System.out.println("   Causing agents:");
		
			// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
			List<PersonDelayInfo> reverseList = new ArrayList<PersonDelayInfo>();
			reverseList.addAll(personDelayInfos);
			Collections.reverse(reverseList);
			
			double delayToPayFor = delay;
			for (PersonDelayInfo info : reverseList){
				if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
					
					MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "flowCapacity", info.getPersonId(), personId, linkInfo.getMarginalDelayPerLeavingVehicle_sec(), linkId);
					System.out.println("	Person " + info.getPersonId() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
					List<MarginalCongestionEvent> congestionEvents_FlowCapacity = null;
					if (linkInfo.getflowCapacityCongestionEvents() == null) {
						congestionEvents_FlowCapacity = new ArrayList<MarginalCongestionEvent>();
						congestionEvents_FlowCapacity.add(congestionEvent);

					} else {
						congestionEvents_FlowCapacity = linkInfo.getflowCapacityCongestionEvents();
						congestionEvents_FlowCapacity.add(congestionEvent);
					}
					linkInfo.setflowCapacityCongestionEvents(congestionEvents_FlowCapacity);												
					delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
					
				} else {
					if (delayToPayFor > 0) {
						MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "flowCapacity", info.getPersonId(), personId, delayToPayFor, linkId);
						System.out.println("	Person " + info.getPersonId() + " --> Marginal delay: " + delayToPayFor);
						
						List<MarginalCongestionEvent> congestionEvents_FlowCapacity = null;
						if (linkInfo.getflowCapacityCongestionEvents() == null) {
							congestionEvents_FlowCapacity = new ArrayList<MarginalCongestionEvent>();
							congestionEvents_FlowCapacity.add(congestionEvent);

						} else {
							congestionEvents_FlowCapacity = linkInfo.getflowCapacityCongestionEvents();
							congestionEvents_FlowCapacity.add(congestionEvent);
						}
						linkInfo.setflowCapacityCongestionEvents(congestionEvents_FlowCapacity);
						delayToPayFor = 0;
					}
				}
			}
			System.out.println("---------------------------------------------------------------------------------------------------------------------");
			
			if (delayToPayFor != 0.) {
				throw new RuntimeException("More congestion delays on that link than causing agents are identified for. Aborting...");
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

	private void calculateStorageCongestion(double time, Id personId, Id linkId) {
			List<PersonDelayInfo> personDelayInfos = this.linkId2congestionInfo.get(linkId).getPersonDelayInfos();
			for (PersonDelayInfo personDelayinfo : personDelayInfos){
				
				if (personDelayinfo.getPersonId().toString().equals(personId.toString())){
				
					if (personDelayinfo.getLinkLeaveTime() < time) {
						
						double delay = time - personDelayinfo.getLinkLeaveTime();
						System.out.println("---------------------------------------------------------------------------------------------------------------------");
						System.out.println(personId.toString() + " is delayed due to storage capacity constraints on links behind link " + linkId + ". Delay [sec]: " + delay);
						
						List<Id> followingLinkIDs = getFollowingLinkIDs(personId, linkId);
						double linkLeaveTime = personDelayinfo.getLinkLeaveTime();
						
						// get queue at the linkLeaveTime (when this agent wanted to leave but could not because of storage capacity constraints)
						List<Id> causingAgentIDs = new ArrayList<Id>();
						causingAgentIDs.addAll(getCausingAgentsCurrentLink(linkId, personId, linkLeaveTime));
						causingAgentIDs.addAll(getCausingAgentsFollowingLinks(followingLinkIDs, linkLeaveTime));
						// divide delay by number of agents in front of me to get the delay caused per agent
						double delayPerCausingAgent;
						if (causingAgentIDs.size() == 0) {
							throw new RuntimeException("No causing agent identified. Aborting...");
						} else {
							delayPerCausingAgent = delay / causingAgentIDs.size();
						}
						// throw delay effects for all of them
						for (Id id : causingAgentIDs){
							MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "storageCapacity", id, personId, delayPerCausingAgent, linkId);
							this.events.processEvent(congestionEvent);
						}
						
						// TODO: alternativly: only assume the next agent in front of me to cause my delay
						
						System.out.println("Number of agents in front of me (on the same link or other links downstream) causing the delay: " + causingAgentIDs.size());
						System.out.println("Causing agent(s): " + causingAgentIDs.toString());
						System.out.println("---------------------------------------------------------------------------------------------------------------------");
						
						// Delay resulting from storage capacity constraints are higher than delay resulting from flow capacity constraints
						// Delete marginal congestion events due to flow capacity constraints for this affected agent on this link
						List<MarginalCongestionEvent> congEvents = this.linkId2congestionInfo.get(linkId).getflowCapacityCongestionEvents();
						for (Iterator<MarginalCongestionEvent> iterator = congEvents.iterator(); iterator.hasNext();){
							MarginalCongestionEvent congEvent = iterator.next();
							
							if (congEvent.getAffectedAgentId().toString().equals(personId.toString())) {
								System.out.println(personId + " is further delayed on link " + linkId + " by storage capacity constraints.");
								System.out.println("Deleting (without throwing) MarginalCongestionEvent: " + congEvent.toString());
								iterator.remove();
							}
						}
						
					} else {
	//					System.out.println(event.getPersonId().toString() + " is not delayed due to storage capacity constraints on links behind link " + event.getLinkId() + ".");
					
						// No delay resulting from storage capacity constraints.
						// Throw marginal congestion events due to flow capacity constraints for this affected agent on this link and delete this event afterwards
						List<MarginalCongestionEvent> congEvents = this.linkId2congestionInfo.get(linkId).getflowCapacityCongestionEvents();
						for (Iterator<MarginalCongestionEvent> iterator = congEvents.iterator(); iterator.hasNext();){
							MarginalCongestionEvent congEvent = iterator.next();
							
							if (congEvent.getAffectedAgentId().toString().equals(personId.toString())) {
								System.out.println(personId + " is not further delayed on link " + linkId + " by storage capacity constraints.");
								System.out.println("Throwing and deleting MarginalCongestionEvent: " + congEvent.toString());
								this.events.processEvent(congEvent);
								iterator.remove();
							}
						}
					}
				}
			}
		}

	private List<Id> getCausingAgentsFollowingLinks(List<Id> followingLinkIDs, double linkLeaveTime) {
		List<Id> causingAgentIDs = new ArrayList<Id>();
		
		boolean congestedLink = true;
		for (Id linkId : followingLinkIDs) {
			
			if (this.linkId2congestionInfo.get(linkId) == null) {
				// no one was on that link before, stop following congested links
	
			} else {
				
				if (congestedLink){
					List<LinkEnterLeaveInfo> enterLeaveInfosForThisLink = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();
					int storageCapacity_cars = this.linkId2congestionInfo.get(linkId).getStorageCapacity_cars();
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
						// storage capacity not reached
						congestedLink = false;
						
					} else if (personsOnLink.size() > storageCapacity_cars) {	
						throw new RuntimeException("More cars on link than storage capacity allows. Aborting...");
					}
				} else {
					// stop following possible congested links...
				}
			}
		}
		return causingAgentIDs;
	}

	private List<Id> getCausingAgentsCurrentLink(Id linkId, Id personId, double linkLeaveTime) {
		// search for agents who are in the queue in front of me when I wanted to leave the link
		List<Id> causingAgentIDs = new ArrayList<Id>();
		List<LinkEnterLeaveInfo> enterLeaveInfosCurrentLink = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();

		double linkEnterTime_delayedPerson = 0;
		for (LinkEnterLeaveInfo infoCurrentLink : enterLeaveInfosCurrentLink) {
			if (infoCurrentLink.getPersonId().toString().equals(personId.toString())) {
				linkEnterTime_delayedPerson = infoCurrentLink.getLinkEnterTime();
			}
		}
		
		for (LinkEnterLeaveInfo infoCurrentLink : enterLeaveInfosCurrentLink) {
			
			if (!infoCurrentLink.getPersonId().toString().equals(personId.toString())) {
				// not the affected agent
				
				if ((infoCurrentLink.getLinkEnterTime() < linkLeaveTime && infoCurrentLink.getLinkLeaveTime() > linkLeaveTime) || (infoCurrentLink.getLinkEnterTime() < linkLeaveTime && infoCurrentLink.getLinkLeaveTime() == 0)){
					// person was/is on the current link within the relevant time
				
					if (infoCurrentLink.getLinkEnterTime() < linkEnterTime_delayedPerson) {
						// agent is entering link before the affected agent
						causingAgentIDs.add(infoCurrentLink.getPersonId());
					}
				}
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
					relevantLinkIDs.add(route.getEndLinkId()); // assuming this link to be the last link where the storage capacity plays a role.
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

	private void collectLinkInfos(Id linkId) {
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(link.getLength() / link.getFreespeed());
		linkInfo.setMarginalDelayPerLeavingVehicle(link.getCapacity());
		int storageCapacity_cars = (int) ((link.getLength() * link.getNumberOfLanes()) / 7.5);
		linkInfo.setStorageCapacity_cars(storageCapacity_cars);
		this.linkId2congestionInfo.put(link.getId(), linkInfo);
	}

	private void updateLinkInfo_agentEntersLink(double time, Id personId, Id linkId) {
		if (this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos() == null) {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkEnterTime(time);
			linkEnterLeaveInfo.setLinkLeaveTime(0.);
			
			personId2enterLeaveInfo.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
		} else {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkEnterTime(time);
			linkEnterLeaveInfo.setLinkLeaveTime(0.);
			
			personId2enterLeaveInfo.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
		}
	}

	private void updateLinkInfo_agentLeavesLink(double time, Id personId, Id linkId) {
		
		if (this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos() == null) {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = new ArrayList<LinkEnterLeaveInfo>();
			LinkEnterLeaveInfo linkEnterLeaveInfo = new LinkEnterLeaveInfo();
			linkEnterLeaveInfo.setPersonId(personId);
			linkEnterLeaveInfo.setLinkLeaveTime(time);
			
			personId2enterLeaveInfo.add(linkEnterLeaveInfo);
			linkId2congestionInfo.get(linkId).setPersonId2enterLeaveInfo(personId2enterLeaveInfo);
		
		} else {
			List<LinkEnterLeaveInfo> personId2enterLeaveInfo = this.linkId2congestionInfo.get(linkId).getPersonEnterLeaveInfos();
			for (LinkEnterLeaveInfo info : personId2enterLeaveInfo) {
				if (info.getPersonId().toString().equals(personId.toString())){
					if (info.getLinkLeaveTime() == 0.){
						info.setLinkLeaveTime(time);
					} else {
						log.warn("Person " + info.getPersonId() + " was leaving link " + linkId + " before. Check calculation of marginal congestion effects.");
					}
				}
			}
		}
	}
}
