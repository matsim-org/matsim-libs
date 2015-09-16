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

package playground.vsp.congestion.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

/** 
 * 
 * This implementation is (hopefully) equal to the previous implementation V3.
 * 
 * For each delay, the causing agent(s) are identified and a 'marginal congestion event' is thrown which can be used for pricing.
 *
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to the free speed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. In this implementation the causing agent for a delay resulting from the storage capacity is assumed to be the agent who caused the spill-back at the bottleneck link.
 * That is, the affected agent keeps the delay caused by the storage capacity constraint until he/she reaches the bottleneck link and (hopefully) identifies there the agent who is causing the spill-backs.
 * 
 * @author ikaddoura
 *
 */
public class CongestionHandlerImplV3a implements
CongestionInternalization,
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
ActivityEndEventHandler {
	
	private final static Logger log = Logger.getLogger(CongestionHandlerImplV3a.class);

	// If the following parameter is false, a Runtime Exception is thrown in case an agent is delayed by the storage capacity.
	private final boolean allowingForStorageCapacityConstraint = true;

	// If the following parameter is false, the delays resulting from the storage capacity are not internalized.
	private final boolean calculatingStorageCapacityConstraints = true;
	
	private final CongestionInfoHandler congestionInfoHandlerDelegate;
	private final EventsManager events;
	
	// statistics
	private double totalDelay = 0.;
	private double totalInternalizedDelay = 0.;
	private double delayNotInternalized_storageCapacity = 0.0;
	private double delayNotInternalized_roundingErrors = 0.0;
	private double delayNotInternalized_spillbackNoCausingAgent = 0.0;
	
	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<Id<Person>, Double>();

	
	public CongestionHandlerImplV3a(EventsManager events, Scenario scenario) {
		this.events = events;
		congestionInfoHandlerDelegate = new CongestionInfoHandler(scenario);
	}

	public final void reset(int iteration) {
		congestionInfoHandlerDelegate.reset(iteration);
		
		this.totalDelay = 0.;
		this.totalInternalizedDelay = 0.;
		
		this.delayNotInternalized_storageCapacity = 0.;
		this.delayNotInternalized_roundingErrors = 0.;
		this.delayNotInternalized_spillbackNoCausingAgent = 0.;
	}

	@Override
	public final void handleEvent(TransitDriverStartsEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonStuckEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		
		if (this.congestionInfoHandlerDelegate.getPtVehicleIDs().contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else {
			// car!
			if (this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get(event.getLinkId()) == null){
				// no one left this link before
				this.congestionInfoHandlerDelegate.createLinkInfo(event.getLinkId());
			}

			updateFlowQueue(event);
			calculateCongestion(event);
			addAgentToFlowQueue(event);
		}		
	}

	public void updateFlowQueue(LinkLeaveEvent event) {
		this.congestionInfoHandlerDelegate.updateFlowQueue(event);
	}
	
	public void addAgentToFlowQueue(LinkLeaveEvent event) {
		this.congestionInfoHandlerDelegate.addAgentToFlowQueue(event);
	}

	@Override
	public void calculateCongestion(LinkLeaveEvent event) {
		
		LinkCongestionInfo linkInfo = this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		
		// global book-keeping:
		this.totalDelay += delayOnThisLink;
		
		// Check if this (affected) agent was previously delayed without internalizing the delay.
		double agentDelayWithDelaysOnPreviousLinks = 0.;
		
		if (this.agentId2storageDelay.get(event.getVehicleId()) == null) {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink;
		
		} else {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink + this.agentId2storageDelay.get(event.getVehicleId());
			this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), 0.);
		}
		
		if (agentDelayWithDelaysOnPreviousLinks < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");
			
		} else if (agentDelayWithDelaysOnPreviousLinks == 0.) {
			// The agent was leaving the link without a delay.
			
		} else {
			// The agent was leaving the link with a delay.
						
			double storageDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), event.getVehicleId(), agentDelayWithDelaysOnPreviousLinks);
			
			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");
			
			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.
				
			} else if (storageDelay > 0.) {	
				
				if (this.allowingForStorageCapacityConstraint) {
				
					if (this.calculatingStorageCapacityConstraints) {
						// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
						this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), storageDelay);
					
					} else {
						this.delayNotInternalized_storageCapacity += storageDelay;
					}
					
				} else {
					throw new RuntimeException("There is a delay resulting from the storage capacity on link " + event.getLinkId() + ": " + storageDelay + ". Aborting...");
				}
			} 
		}
	}
	
	final double computeFlowCongestionAndReturnStorageDelay(double now, Id<Link> linkId, Id<Vehicle> vehicleId, double agentDelay) {
		LinkCongestionInfo linkInfo = this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get( linkId);

		// Search for agents causing the delay on that link and throw delayEffects for the causing agents.
		List<Id<Person>> reverseList = new ArrayList<Id<Person>>();
		reverseList.addAll(linkInfo.getLeavingAgents());
		Collections.reverse(reverseList);

		for (Id<Person> personId : reverseList){
			double delayForThisPerson = Math.min( linkInfo.getMarginalDelayPerLeavingVehicle_sec(), agentDelay ) ;
			// (marginalDelay... is based on flow capacity of link, not time headway of vehicle)

			if (vehicleId.toString().equals(personId.toString())) {
				//					log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
			} else {
				// using the time when the causing agent entered the link
				CongestionEvent congestionEvent = new CongestionEvent(now, "flowStorageCapacity", personId, 
						Id.createPersonId(vehicleId), delayForThisPerson, linkId, 
						linkInfo.getPersonId2linkEnterTime().get(personId));
				this.events.processEvent(congestionEvent);
				this.totalInternalizedDelay += delayForThisPerson ;
			}
			agentDelay = agentDelay - delayForThisPerson ; 
		}

		if (agentDelay <= 1.) {
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += agentDelay;
			agentDelay = 0.;
		}

		return agentDelay;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
	
		if (this.agentId2storageDelay.get(event.getPersonId()) == null) {
			// skip that person
			
		} else {
			if (this.agentId2storageDelay.get(event.getPersonId()) != 0.) {
//				log.warn("A delay of " + this.agentId2storageDelay.get(event.getPersonId()) + " sec. resulting from spill-back effects was not internalized. Setting the delay to 0.");
				this.delayNotInternalized_spillbackNoCausingAgent += this.agentId2storageDelay.get(event.getPersonId());
			}
			this.agentId2storageDelay.put(event.getPersonId(), 0.);
		}
	}
	
	@Override
	public final double getTotalInternalizedDelay() {
		return this.totalInternalizedDelay;
	}

	@Override
	public final double getTotalDelay() {
		return this.totalDelay;
	}
	
	@Override
	public final void writeCongestionStats(String fileName) {
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Total delay [hours];" + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.totalInternalizedDelay / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.delayNotInternalized_roundingErrors / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (spill-back related delays without identifiying the causing agent) [hours];" + this.delayNotInternalized_spillbackNoCausingAgent / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (in case delays resulting from the storage capacity are ignored) [hours];" + this.delayNotInternalized_storageCapacity / 3600.);
			bw.newLine();
			
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Congestion statistics written to " + fileName);		
	}
	
}
