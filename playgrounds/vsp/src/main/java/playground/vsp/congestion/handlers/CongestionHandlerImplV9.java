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
import java.util.Iterator;
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
import playground.vsp.congestion.events.CongestionEvent;

/** 
 * 
 * For each agent leaving a link: Compute a delay as the difference between free speed travel time and actual travel time.
 * 
 * In this implementation, the delay is partially allocated to ALL agents ahead in the flow queue.
 * Each agent has to pay for 1 / c_flow.
 * 
 * Spill-back effects are taken into account by saving the delay resulting from the storage capacity constraint
 * for later when possibly reaching the bottleneck link
 * 
 * @author ikaddoura
 *
 */
public final class CongestionHandlerImplV9 implements CongestionHandler, ActivityEndEventHandler {

	private final static Logger log = Logger.getLogger(CongestionHandlerImplV9.class);

	private CongestionHandlerBaseImpl delegate;
	private EventsManager events;

	private final Map<Id<Person>, Double> agentId2storageDelay = new HashMap<Id<Person>, Double>();
	private double delayNotInternalized_spillbackNoCausingAgent = 0.;

	public CongestionHandlerImplV9(EventsManager events, Scenario scenario) {
		this.delegate = new CongestionHandlerBaseImpl(events, scenario);
		this.events = events;
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
		
		final double totalDelayOnThisLink = delayInfo.linkLeaveTime - delayInfo.freeSpeedLeaveTime ;
		double totalAllocatedDelay = 0.;

		if (totalDelayOnThisLink < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");

		} else if (totalDelayOnThisLink == 0.) {
			// The agent was leaving the link without a delay.  Nothing to do ...

		} else {
			// The agent was leaving the link with a delay.
			
			// FIRST: Go throw the flow queue and charge all causing agents the inverse of the flow capacity
			LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get(event.getLinkId());
			
			for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().descendingIterator() ; it.hasNext() ; ) {
				// Get the agent 'ahead' from the flow queue. The agents 'ahead' are considered as causing agents.
				DelayInfo causingAgentDelayInfo = it.next() ;
				if ( causingAgentDelayInfo.personId.equals( delayInfo.personId ) ) {
					// not charging to yourself:
					continue ;
				}

				// each causing agent only imposes the inverse of the flow capacity on the affected agent
				double allocatedBottleneckDelay = linkInfo.getMarginalDelayPerLeavingVehicle_sec();
				totalAllocatedDelay += allocatedBottleneckDelay;

				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "BottleneckDelay", causingAgentDelayInfo.personId, 
						delayInfo.personId, allocatedBottleneckDelay, event.getLinkId(), causingAgentDelayInfo.linkEnterTime );
				this.events.processEvent(congestionEvent); 

				this.delegate.addToTotalInternalizedDelay(allocatedBottleneckDelay);
			}
			
			// SECOND: Check if this (affected) agent was previously delayed due to spill-back.

			double agentSpillbackDelayOnPreviousLinks = 0.;		
			if (this.agentId2storageDelay.containsKey(delayInfo.personId)) {				
				agentSpillbackDelayOnPreviousLinks = this.agentId2storageDelay.get(delayInfo.personId);
			}
			
			if (agentSpillbackDelayOnPreviousLinks > 0.) {
				// the agent was previously delayed due to spill-back
				
				if (linkInfo.getFlowQueue().isEmpty()) {
					// the bottleneck is inactive
					
				} else {
					
					// this is the first active bottleneck after being delayed due to spill-back
					// each agent in the queue should also pay for the upstream spill-back delays
					// charge again each agent the inverse of the flow capacity
					
					for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().descendingIterator() ; it.hasNext() ; ) {

						DelayInfo causingAgentDelayInfo = it.next() ;
						if ( causingAgentDelayInfo.personId.equals( delayInfo.personId ) ) {
							// not charging to yourself:
							continue ;
						}

						// each agent was blocking the link for the time equal to the inverse of the flow capacity
						double allocatedSpillBackDelay = linkInfo.getMarginalDelayPerLeavingVehicle_sec();

						CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "SpillBackDelay", causingAgentDelayInfo.personId, 
								delayInfo.personId, allocatedSpillBackDelay, event.getLinkId(), causingAgentDelayInfo.linkEnterTime );
						this.events.processEvent(congestionEvent); 

						this.delegate.addToTotalInternalizedDelay(allocatedSpillBackDelay);
					}
					
					this.agentId2storageDelay.remove(delayInfo.personId);
				}
				
			} else {
				// the agent was not previously delayed due to spill-back
			}
			
			// THIRD: Compute the spill-back delay.
			
			if (totalDelayOnThisLink > totalAllocatedDelay) {
				
				// The remaining delay results from spill-back delays, which in turn result from an active bottleneck further downstream.
				// Store this delay to (hopefully) charge the agents in the queue of the active bottleneck further downstream.
				
				this.agentId2storageDelay.put(delayInfo.personId, (totalDelayOnThisLink - totalAllocatedDelay) );
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
