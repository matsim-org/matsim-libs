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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.vsp.congestion.DelayInfo;

/** 
 * 
 * For each agent leaving a link: Compute a delay as the difference between free speed travel time and actual travel time.
 * 
 * In this implementation, the delay is partially allocated to the agents ahead in the flow queue until the delay is fully internalized (cost recovery).
 * Each causing agent has to pay for 1 / c_flow which is deducted from the delay to be internalized.
 * 
 * Spill-back effects are taken into account by saving the remaining delay (considered to result from the storage capacity constraint)
 * for later when possibly reaching the bottleneck link
 * 
 * @author ikaddoura
 *
 */
public final class CongestionHandlerImplV3 implements CongestionHandler, ActivityEndEventHandler {

	private final static Logger log = Logger.getLogger(CongestionHandlerImplV3.class);

	private CongestionHandlerBaseImpl delegate;

	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<>();
	private double delayNotInternalized_spillbackNoCausingAgent = 0.;

	public CongestionHandlerImplV3(EventsManager events, Scenario scenario) {
		this.delegate = new CongestionHandlerBaseImpl(events, scenario);
	}

	@Override
	public final void reset(int iteration) {

		delegate.reset(iteration);

		this.agentId2storageDelay.clear();
		this.delayNotInternalized_spillbackNoCausingAgent = 0.;
	}

	@Override
	public final void handleEvent(TransitDriverStartsEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonStuckEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public final void writeCongestionStats(String fileName) {
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Total delay [hours];" + this.delegate.getTotalDelay() / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.delegate.getTotalInternalizedDelay() / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.delegate.getDelayNotInternalized_roundingErrors() / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (spill-back related delays without identifiying the causing agent) [hours];" + this.delayNotInternalized_spillbackNoCausingAgent / 3600.);
			bw.newLine();

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Congestion statistics written to " + fileName);	
	}

	@Override
	public final double getTotalDelay() {
		return this.delegate.getTotalDelay();
	}
	
	

	public final double getDelayNotInternalizedSpillbackNoCausingAgent() {
		return this.delayNotInternalized_spillbackNoCausingAgent;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// I read the following as: remove non-allocated delays from agents once they are no longer on a leg. kai, sep'15

		if (this.agentId2storageDelay.get(event.getPersonId()) == null) {
			// skip that person

		} else {
			if (this.agentId2storageDelay.get(event.getPersonId()) != 0.) {
				// log.warn("A delay of " + this.agentId2storageDelay.get(event.getDriverId()) + " sec. resulting from spill-back effects was not internalized. Setting the delay to 0.");
				this.delayNotInternalized_spillbackNoCausingAgent += this.agentId2storageDelay.get(event.getPersonId());
			}
			this.agentId2storageDelay.put(event.getPersonId(), 0.);
		}
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		// yy see my note under CongestionHandlerBaseImpl.handleEvent( LinkLeaveEvent ... ) . kai, sep'15
		
		// coming here ...
		this.delegate.handleEvent( event ) ;

		if (this.delegate.getPtVehicleIDs().contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		} else { // car!
			LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get( event.getLinkId() ) ;
			DelayInfo delayInfo = linkInfo.getFlowQueue().getLast();
			calculateCongestion(event, delayInfo);
		}
	}


	@Override
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
		// yy see my note under CongestionHandlerBaseImpl.handleEvent( LinkLeaveEvent ... ) . kai, sep'15
		
		double delayOnThisLink = delayInfo.linkLeaveTime - delayInfo.freeSpeedLeaveTime ;

		// Check if this (affected) agent was previously delayed without internalizing the delay.

		double agentDelayWithDelaysOnPreviousLinks = 0.;		

		if (this.agentId2storageDelay.get(delayInfo.personId) == null) {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink;
		} else {
			agentDelayWithDelaysOnPreviousLinks = delayOnThisLink + this.agentId2storageDelay.get(delayInfo.personId);
			this.agentId2storageDelay.put(Id.createPersonId(delayInfo.personId), 0.);
		}

		if (agentDelayWithDelaysOnPreviousLinks < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");

		} else if (agentDelayWithDelaysOnPreviousLinks == 0.) {
			// The agent was leaving the link without a delay.  Nothing to do ...

		} else {
			// The agent was leaving the link with a delay.

			double storageDelay = this.delegate.computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), delayInfo, agentDelayWithDelaysOnPreviousLinks);

			if (storageDelay < 0.) {
				throw new RuntimeException("The delay resulting from the storage capacity is below 0. (" + storageDelay + ") Aborting...");

			} else if (storageDelay == 0.) {
				// The delay resulting from the storage capacity is 0.

			} else if (storageDelay > 0.) {	
				// Saving the delay resulting from the storage capacity constraint for later when reaching the bottleneck link.
				this.agentId2storageDelay.put(Id.createPersonId(delayInfo.personId), storageDelay);
			} 
		}
	}

	@Override
	public double getTotalInternalizedDelay() {
		return this.delegate.getTotalInternalizedDelay();
	}

	@Override
	public double getTotalRoundingErrorDelay() {
		return this.delegate.getDelayNotInternalized_roundingErrors();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}
}
