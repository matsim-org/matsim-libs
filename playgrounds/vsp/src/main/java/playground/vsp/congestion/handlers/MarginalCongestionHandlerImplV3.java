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
package playground.vsp.congestion.handlers;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.congestion.LinkCongestionInfo;

/** 
 * In this implementation the causing agent for a delay resulting from the storage capacity is assumed to be the agent who caused the spill-back at the bottleneck link.
 * That is, the affected agent keeps the delay caused by the storage capacity constraint until he/she reaches the bottleneck link and (hopefully) identifies there the agent who is causing the spill-backs.
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerImplV3 extends MarginalCongestionHandler implements ActivityEndEventHandler {
	private final static Logger log = Logger.getLogger(MarginalCongestionHandlerImplV3.class);
	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<Id<Person>, Double>();
	
	public MarginalCongestionHandlerImplV3(EventsManager events, ScenarioImpl scenario) {
		super(events, scenario);
	}
	
	void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		this.totalDelay = this.totalDelay + delayOnThisLink;
		
		// Check if this (affected) agent was previously delayed without internalizing the delay.
		double totalDelayWithDelaysOnPreviousLinks = 0.;
		if (this.agentId2storageDelay.get(event.getVehicleId()) == null) {
			totalDelayWithDelaysOnPreviousLinks = delayOnThisLink;
		} else {
			totalDelayWithDelaysOnPreviousLinks = delayOnThisLink + this.agentId2storageDelay.get(event.getVehicleId());
			this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), 0.);
		}
		
		if (totalDelayWithDelaysOnPreviousLinks < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");
			
		} else if (totalDelayWithDelaysOnPreviousLinks == 0.) {
			// The agent was leaving the link without a delay.
			
		} else {
			// The agent was leaving the link with a delay.
						
			double storageDelay = throwFlowCongestionEventsAndReturnStorageDelay(totalDelayWithDelaysOnPreviousLinks, event);
			
			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");
			
			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.
				
			} else if (storageDelay > 0.) {	
				if (this.allowForStorageCapacityConstraint) {
					if (this.calculateStorageCapacityConstraints) {
						// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
						this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), storageDelay);
					} else {
						this.delayNotInternalized_storageCapacity += storageDelay;
						log.warn("Delay which is not internalized: " + this.delayNotInternalized_storageCapacity);
					}
					
				} else {
					throw new RuntimeException("There is a delay resulting from the storage capacity on link " + event.getLinkId() + ": " + storageDelay + ". Aborting...");
				}
			} 
		}
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

}
