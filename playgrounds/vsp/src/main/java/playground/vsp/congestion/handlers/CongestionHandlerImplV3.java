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
public final class CongestionHandlerImplV3 extends AbstractCongestionHandler implements ActivityEndEventHandler {
	private final static Logger log = Logger.getLogger(CongestionHandlerImplV3.class);
	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<Id<Person>, Double>();
	
	public CongestionHandlerImplV3(EventsManager events, ScenarioImpl scenario) {
		super(events, scenario);
	}
	
	@Override
	void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		
		// global book-keeping:
		this.addToTotalDelay( delayOnThisLink );
		
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
			// The agent was leaving the link without a delay.  Nothing to do ...
			
		} else {
			// The agent was leaving the link with a delay.
						
			double storageDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), event.getVehicleId(), agentDelayWithDelaysOnPreviousLinks);
			
			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");
			
			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.
				
			} else if (storageDelay > 0.) {	
				if (this.isAllowingForStorageCapacityConstraint()) {
					if (this.isCalculatingStorageCapacityConstraints()) {
						// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
						this.agentId2storageDelay.put(Id.createPersonId(event.getVehicleId()), storageDelay);
					} else {
						this.addToDelayNotInternalized_storageCapacity( storageDelay ) ;
						log.warn("Delay which is not internalized: " + this.getDelayNotInternalized_storageCapacity());
					}
				} else {
					throw new RuntimeException("There is a delay resulting from the storage capacity on link " + event.getLinkId() + ": " 
							+ storageDelay + ". Aborting...");
					// not really sure why it aborts here when it is configured as not looking for storage constraints ... kai, spe'15
				}
			} 
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// I read the following as: remove non-allocated delays from agents once they are no longer on a leg. kai, sep'15
		
		if (this.agentId2storageDelay.get(event.getPersonId()) == null) {
			// skip that person
			
		} else {
			if (this.agentId2storageDelay.get(event.getPersonId()) != 0.) {
//				log.warn("A delay of " + this.agentId2storageDelay.get(event.getPersonId()) + " sec. resulting from spill-back effects was not internalized. Setting the delay to 0.");
				this.addToDelayNotInternalized_spillbackNoCausingAgent( this.agentId2storageDelay.get(event.getPersonId()) ) ;
			}
			this.agentId2storageDelay.put(event.getPersonId(), 0.);
		}
	}

}
