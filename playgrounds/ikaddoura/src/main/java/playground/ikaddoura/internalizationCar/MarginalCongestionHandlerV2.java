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
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * This handler calculates car delays (due to flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. The marginal congestion event is thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 
 * In this version the causing agent for a delay due to the storage capacity is assumed to be the last agent who left the link before.
 *  
 * TODO: Adjust for other modes than car and mixed modes. (Adjust for different effective cell sizes than 7.5 meters.)
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerV2 implements
	LinkEnterEventHandler,
	LinkLeaveEventHandler,
	TransitDriverStartsEventHandler,
	PersonDepartureEventHandler, 
	PersonStuckEventHandler {
	
	private final static Logger log = Logger.getLogger(MarginalCongestionHandlerV2.class);
	
	private final boolean allowForStorageCapacityConstraint = true; // Runtime Exception if storage capacity active
	private final boolean calculateStorageCapacityConstraints = true;
	private double delayNotInternalized = 0.;

	private final ScenarioImpl scenario;
	private final EventsManager events;
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
	private final Map<Id, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id, LinkCongestionInfo>();
	
	public MarginalCongestionHandlerV2(EventsManager events, ScenarioImpl scenario) {
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
		
		if (this.scenario.getConfig().qsim().getStuckTime() < 3600.){
			log.warn("Stuck time is very short. If an agent is moved to the next link even though the next link is full, the calculation of delay effects may be wrong.");
		}
		
		if (this.scenario.getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles() != true) {
			throw new RuntimeException("Expecting the qSim to insert waiting vehicles before driving vehicles. Aborting...");
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
		this.delayNotInternalized = 0.;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {	
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("Agent stuck event. No garantee for right calculation of marginal congestion effects: " + event.toString());

//		if (event.getLegMode().equals(TransportMode.car)) {
////			log.warn("Agent stuck event. No garantee for right calculation of marginal congestion effects: " + event.toString());
//			throw new RuntimeException("Agent stuck event (leg mode: car). No garantee for right calculation of marginal congestion effects: " + event.toString());
//		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);

		} else {			
//			log.warn("Not tested for other modes than car.");
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
//			log.warn("Not tested for pt.");
		
		} else {
			// car!
			
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getVehicleId(), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
		}	
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
//			log.warn("Not tested for pt.");
		
		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one left this link before
				collectLinkInfos(event.getLinkId());
			}
						
			updateTrackingMarginalDelays(event);
			calculateCongestion(event);
			trackMarginalDelay(event);

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.setLastLeavingAgent(event.getVehicleId());
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());
		}
	}
	
	
	// ############################################################################################################################################################

	
	private void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double totalDelay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		
//		System.out.println(event.toString());
//		System.out.println("free travel time: " + linkInfo.getFreeTravelTime() + " // marginal flow delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec());
//		System.out.println("relevant agents (previously leaving the link): " + linkInfo.getLeavingAgents());
//		System.out.println("total delay: " + totalDelay);
		
		if (totalDelay < -1.0) {
			throw new RuntimeException("Aborting...");
			
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
				throw new RuntimeException("Oups, negative storage delay: " + storageDelay);
			}
			
		}
	}
	
	private double throwFlowCongestionEventsAndReturnStorageDelay(double totalDelay, LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id> reverseList = new ArrayList<Id>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);
		
		double delayToPayFor = totalDelay;
		for (Id id : reverseList){
			if (delayToPayFor > linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
				
//				System.out.println("	Person " + id.toString() + " --> Marginal delay: " + linkInfo.getMarginalDelayPerLeavingVehicle_sec() + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
				MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowCapacity", id, event.getVehicleId(), linkInfo.getMarginalDelayPerLeavingVehicle_sec(), event.getLinkId());
				this.events.processEvent(congestionEvent);	
				
				delayToPayFor = delayToPayFor - linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				
			} else {
				if (delayToPayFor > 0) {
					
//					System.out.println("	Person " + id + " --> Marginal delay: " + delayToPayFor + " linkLeaveTime: " + linkInfo.getPersonId2linkLeaveTime().get(id));
					MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "flowCapacity", id, event.getVehicleId(), delayToPayFor, event.getLinkId());
					this.events.processEvent(congestionEvent);
					
					delayToPayFor = 0;
				}
			}
		}
		
		if (delayToPayFor == 1.) { // Check if "< 1.0" is also ok...
//			log.warn("Remaining delay of 1.0 sec may result from rounding errors. Setting the remaining delay to 0.0 sec.");
			delayToPayFor = 0.;
		}
		return delayToPayFor;
	}

	private void calculateStorageCongestion(LinkLeaveEvent event, double remainingDelay) {
				
		Id causingAgent = null;
		causingAgent = this.linkId2congestionInfo.get(event.getLinkId()).getLastLeavingAgent();
		
		if (causingAgent == null){
			log.warn("An agent is delayed due to storage congestion on link " + event.getLinkId() + " but no agent left the link before. " +
					"That is, storage congestion appears due to agents departing on that link. In this version, these delays are not internalized.");
		} else {
			MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "storageCapacity", causingAgent, event.getVehicleId(), remainingDelay, event.getLinkId());
			this.events.processEvent(congestionEvent);
		}
	}
	
	private void updateTrackingMarginalDelays(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
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
	
	private void trackMarginalDelay(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		// start tracking delays caused by that agent leaving the link	
		if (linkInfo.getLeavingAgents().contains(event.getVehicleId())){
			throw new RuntimeException(event.getVehicleId() + " is already being tracked for link " + event.getLinkId() + " (in List 'leavingAgents'). Aborting...");
		}
		if (linkInfo.getPersonId2linkLeaveTime().containsKey(event.getVehicleId())){
			log.warn(" Map 'personId2linkLeaveTime' at time step " + event.getTime());
			for (Id id : linkInfo.getPersonId2linkLeaveTime().keySet()) {
				log.warn(id + " // " + linkInfo.getPersonId2linkLeaveTime().get(id));
			}
			throw new RuntimeException(event.getVehicleId() + " is already being tracked for link " + event.getLinkId() + " (in Map 'personId2linkLeaveTime'). Aborting...");
		}
		linkInfo.getLeavingAgents().add(event.getVehicleId());
		linkInfo.getPersonId2linkLeaveTime().put(event.getVehicleId(), event.getTime());
	}

	private void collectLinkInfos(Id linkId) {
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

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
