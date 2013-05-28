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
import java.util.Iterator;
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
 * TODO: Adjust for other flow / storage capacity factors than 1.0.
 * only working if	<param name="insertingWaitingVehiclesBeforeDrivingVehicles" value="true" />
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
	private Map<Id, Boolean> personId2isDeparting = new HashMap<Id, Boolean>();
	
	private final boolean withStorageCapacityConstraint = false;

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
			this.personId2isDeparting.put(event.getPersonId(), true);
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
//			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
			this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());

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
			
			this.personId2isDeparting.put(event.getPersonId(), false);
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
			
//			updateLinkInfo_agentEntersLink(event.getTime(), event.getPersonId(), event.getLinkId());
			this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());

		}	
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
//			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());
			
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
			
			calculateMarginalCongestion(event, this.withStorageCapacityConstraint);
			trackMarginalDelay(event);
//			updateLinkInfo_agentLeavesLink(event.getTime(), event.getPersonId(), event.getLinkId());
		}
	}

	private void calculateMarginalCongestion(LinkLeaveEvent event, boolean withStorageCapacityConstraint) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		Double time = event.getTime();
		
//		System.out.println("************** leaving link event ******************");
//		System.out.println("agent Id: " + personId);
//		System.out.println("link Id: " + linkId);
//		System.out.println("leave time: " + time);
//		System.out.println("enter time: " + linkInfo.getPersonId2linkEnterTime().get(event.getPersonId()));
//		System.out.println("free travel time: " + linkInfo.getFreeTravelTime());

		
		double delay = Double.NEGATIVE_INFINITY;
		if (this.personId2isDeparting.get(event.getPersonId())){
			delay = event.getTime() - linkInfo.getPersonId2linkEnterTime().get(event.getPersonId()) - 1.0;
		} else {
			delay = event.getTime() - linkInfo.getPersonId2linkEnterTime().get(event.getPersonId()) - linkInfo.getFreeTravelTime();
		}
		
		if (delay == 0.) {
			// person was leaving that link without delay
		
		} else {
			
//			System.out.println("---------------------------------------------------------------------------------------------------------------------");
//			System.out.println(personId + " may be delayed on link " + linkId + " due to flow capacity constraints. Delay [sec]: " + delay);
//			System.out.println("   Causing agents: " + linkInfo.getLeavingAgents().toString());

			// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
			List<Id> reverseList = new ArrayList<Id>();
			reverseList.addAll(linkInfo.getLeavingAgents());
			Collections.reverse(reverseList);
			
			double delayToPayFor = delay;
			for (Id id : reverseList){
				if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
					
//					System.out.println("	Person " + id.toString() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec() + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
					MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "flowCapacity", id, personId, linkInfo.getMarginalDelayPerLeavingVehicle_sec(), linkId);																	

//					this.events.processEvent(congestionEvent);
					List<MarginalCongestionEvent> congestionEvents_FlowCapacity = null;
					if (linkInfo.getCongestionEvents_FlowCapacity() == null) {
						congestionEvents_FlowCapacity = new ArrayList<MarginalCongestionEvent>();
						congestionEvents_FlowCapacity.add(congestionEvent);

					} else {
						congestionEvents_FlowCapacity = linkInfo.getCongestionEvents_FlowCapacity();
						congestionEvents_FlowCapacity.add(congestionEvent);
					}
					linkInfo.setCongestionEvents_FlowCapacity(congestionEvents_FlowCapacity);		
					
					delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
					
				} else {
					if (delayToPayFor > 0) {

//						System.out.println("	Person " + id + " --> Marginal delay: " + delayToPayFor + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
						MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "flowCapacity", id, personId, delayToPayFor, linkId);
						
//						this.events.processEvent(congestionEvent);
						List<MarginalCongestionEvent> congestionEvents_FlowCapacity = null;
						if (linkInfo.getCongestionEvents_FlowCapacity() == null) {
							congestionEvents_FlowCapacity = new ArrayList<MarginalCongestionEvent>();
							congestionEvents_FlowCapacity.add(congestionEvent);

						} else {
							congestionEvents_FlowCapacity = linkInfo.getCongestionEvents_FlowCapacity();
							congestionEvents_FlowCapacity.add(congestionEvent);
						}
						linkInfo.setCongestionEvents_FlowCapacity(congestionEvents_FlowCapacity);
						
						delayToPayFor = 0;
					}
				}
			}
//			System.out.println("---------------------------------------------------------------------------------------------------------------------");
			
			if (delayToPayFor > 0.0) {
				if (withStorageCapacityConstraint) {
					
					// Delay resulting from storage capacity constraints are higher than delay resulting from flow capacity constraints
					// Delete marginal congestion events due to flow capacity constraints for this affected agent on this link
					List<MarginalCongestionEvent> congEvents = this.linkId2congestionInfo.get(linkId).getCongestionEvents_FlowCapacity();
					for (Iterator<MarginalCongestionEvent> iterator = congEvents.iterator(); iterator.hasNext();){
						MarginalCongestionEvent congEvent = iterator.next();
						
						if (congEvent.getAffectedAgentId().toString().equals(personId.toString())) {
							System.out.println(personId + " is further delayed on link " + linkId + " by storage capacity constraints.");
							System.out.println("Deleting (without throwing) MarginalCongestionEvent: " + congEvent.toString());
							iterator.remove();
						}
					}
					
					calculateStorageCongestion(event.getTime(), event.getPersonId(), event.getLinkId(), delay);
				
				} else {
					throw new RuntimeException("More congestion delays on that link " + linkId + " than causing agents are identified for (remaining delay: " + delayToPayFor + "). Probably because of storage capacity constraints. Aborting...");
				}
				
			} else {
				// No delay resulting from storage capacity constraints.
				// Throw marginal congestion events due to flow capacity constraints for this affected agent on this link and delete this event afterwards
				List<MarginalCongestionEvent> congEvents = this.linkId2congestionInfo.get(linkId).getCongestionEvents_FlowCapacity();
				for (Iterator<MarginalCongestionEvent> iterator = congEvents.iterator(); iterator.hasNext();){
					MarginalCongestionEvent congEvent = iterator.next();
					
					if (congEvent.getAffectedAgentId().toString().equals(personId.toString())) {
//						System.out.println(personId + " is not further delayed on link " + linkId + " by storage capacity constraints.");
//						System.out.println("Throwing and deleting MarginalCongestionEvent: " + congEvent.toString());
						this.events.processEvent(congEvent);
						iterator.remove();
					}
				}
			}
		}
		
	}
	
	private void calculateStorageCongestion(double time, Id personId, Id linkId, double delay) {
		
		System.out.println("---------------------------------------------------------------------------------------------------------------------");
		System.out.println(personId.toString() + " is delayed due to storage capacity constraints on links behind link " + linkId + ". Delay [sec]: " + delay);
		
		double flowLeaveTime = time - delay;
		System.out.println("Looking who was delaying me, when I wanted to leave the link (" + flowLeaveTime + ").");
		
		// TODO: only assume the next agent in front of me to cause my delay
		
//		List<Id> followingLinkIDs = getFollowingLinkIDs(personId, linkId, flowLeaveTime);
//		List<Id> causingAgentIDs = new ArrayList<Id>();
//		causingAgentIDs.addAll(getCausingAgentsCurrentLink(linkId, personId, flowLeaveTime));
//		causingAgentIDs.addAll(getCausingAgentsFollowingLinks(followingLinkIDs, flowLeaveTime));
//		
//		double delayPerCausingAgent = 0.;
//		if (causingAgentIDs.size() == 0) {
////			log.warn("No causing agent for " + delay + "sec identified.");
//			throw new RuntimeException("No causing agent for " + delay + "sec identified. Aborting...");
//		} else {
//			delayPerCausingAgent = delay / causingAgentIDs.size();
//		}
//		// throw delay effects for all of them
//		for (Id id : causingAgentIDs){
//			MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(time, "storageCapacity", id, personId, delayPerCausingAgent, linkId);
//			this.events.processEvent(congestionEvent);
//		}

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
						throw new RuntimeException("More cars on link than storage capacity allows. (storageCapacity: "+storageCapacity_cars + " /// cars: " + personsOnLink.size() + ") Aborting...");
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
	
	private List<Id> getFollowingLinkIDs(Id personId, Id linkId, double time) {
	
		// get current route linkIDs
		List<Id> currentRouteLinkIDs = getCurrentRouteLinkIDs(personId, linkId, time);
		
		// get all following linkIDs
		boolean linkAfterCurrentLink = false;
		List<Id> linkIDsAfterCurrentLink = new ArrayList<Id>();
		for (Id id : currentRouteLinkIDs){
			if (linkAfterCurrentLink){
				linkIDsAfterCurrentLink.add(id); // current linkID and all linkIDs after that one!
			}
			if (linkId.toString().equals(id.toString())){
				linkAfterCurrentLink = true;
			}
		}
		
		return linkIDsAfterCurrentLink;
	}
	
	private List<Id> getCurrentRouteLinkIDs(Id personId, Id linkId, double time) {
		
		List<Id> currentRouteLinkIDs = null;
		
		Plan selectedPlan = this.scenario.getPopulation().getPersons().get(personId).getSelectedPlan();
		for (PlanElement pE : selectedPlan.getPlanElements()) {
			if (pE instanceof Activity){
				Activity act = (Activity) pE;
				if (act.getEndTime() < 0.) {
					// act has no endtime
					
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
		
		return currentRouteLinkIDs;
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
	
	private void trackMarginalDelay(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// clear trackings of persons leaving that link previously
		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id id : linkInfo.getPersonId2linkLeaveTime().keySet()){
			if (linkInfo.getPersonId2linkLeaveTime().get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = linkInfo.getPersonId2linkLeaveTime().get(id);
			}
		}
		if (event.getTime() > lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec()){
			linkInfo.getLeavingAgents().clear();
			linkInfo.getPersonId2linkLeaveTime().clear();
		
		} else if (event.getTime() == lastLeavingFromThatLink ){
			throw new RuntimeException("Two agents are leaving the same link the same time. Aborting...");
		
//		} else if ((event.getTime() > lastLeavingFromThatLink) && (event.getTime() < lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec())) {
//			throw new RuntimeException("An agent is leaving earlier than flow capacity allows. Aborting...");
		}
		
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
	
}
