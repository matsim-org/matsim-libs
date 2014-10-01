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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * In this implementation the causing agent for a delay resulting from the storage capacity is assumed to be the last agent who left the link before.
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionHandlerImplV2 extends MarginalCongestionHandler {
	
	private final static Logger log = Logger.getLogger(MarginalCongestionHandlerImplV2.class);
	
	public MarginalCongestionHandlerImplV2(EventsManager events, ScenarioImpl scenario) {
		super(events, scenario);
	}
	
	void calculateCongestion(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		this.totalDelay += delayOnThisLink;
		
		if (delayOnThisLink < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");
			
		} else if (delayOnThisLink == 0.) {
			// The agent was leaving the link without a delay.
			
		} else {
			// The agent was leaving the link with a delay.
						
			double storageDelay = throwFlowCongestionEventsAndReturnStorageDelay(delayOnThisLink, event);
			
			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");
			
			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.
			
			} else if (storageDelay > 0.) {
				if (this.allowForStorageCapacityConstraint) {
					if (this.calculateStorageCapacityConstraints) {
						// Search for the agent who has to pay additionally for the left over delay resulting from the storage capacity constraint.
						calculateStorageCongestion(event, storageDelay);
					} else {
						this.delayNotInternalized_storageCapacity = this.delayNotInternalized_storageCapacity + storageDelay;
						log.warn("The total delay resulting from storage capacity constraints and not being internalized amounts to " + this.delayNotInternalized_storageCapacity + ".");
					}
					
				} else {
					throw new RuntimeException("There is a delay resulting from the storage capacity on link " + event.getLinkId() + ": " + storageDelay + ". Aborting...");
				}
			}
		}
	}

	private void calculateStorageCongestion(LinkLeaveEvent event, double remainingDelay) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		
		Id causingAgent = null;
		causingAgent = linkInfo.getLastLeavingAgent();
		
		if (causingAgent == null){
			log.warn("An agent is delayed due to storage congestion on link " + event.getLinkId() + " but no agent has left the link before. " +
					"That is, storage congestion appears due to agents departing on that link. In this version, these delays are not internalized.");
			this.delayNotInternalized_spillbackNoCausingAgent += remainingDelay;
			
		} else {
			// using the time when the causing agent entered the link
			double emergenceTime = 0.;
			if (linkInfo.getPersonId2linkEnterTime().get(causingAgent) == null) {
				emergenceTime = event.getTime();
			} else {
				emergenceTime = linkInfo.getPersonId2linkEnterTime().get(causingAgent);
			}
			
			MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(event.getTime(), "storageCapacity", causingAgent, event.getVehicleId(), remainingDelay, event.getLinkId(), emergenceTime);
			this.events.processEvent(congestionEvent);
			this.totalInternalizedDelay += remainingDelay;
		}
	}
	
}
