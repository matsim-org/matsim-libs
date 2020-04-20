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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.events.CongestionEvent;

/** 
 * 
 * For each agent leaving a link: Compute a delay as the difference between free speed travel time and actual travel time.
 * 
 * If the delay is > 0, in this implementation, the headway to the previous agent is allocated to ALL agents ahead in the flow queue.
 * Each agent has to pay for 1 / c_flow.
 * 
 * Spill-back effects are not taken into account.
 *  
 * @author ikaddoura
 *
 */
public final class CongestionHandlerImplV10 implements CongestionHandler {

	private final static Logger log = Logger.getLogger(CongestionHandlerImplV10.class);

	private CongestionHandlerBaseImpl delegate;
	private EventsManager events;

	public CongestionHandlerImplV10(EventsManager events, Scenario scenario) {
		this.delegate = new CongestionHandlerBaseImpl(events, scenario);
		this.events = events;
	}

	@Override
	public final void reset(int iteration) {
		delegate.reset(iteration);
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

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		
		this.delegate.handleEvent( event ) ;

		if (this.delegate.getPtVehicleIDs().contains(event.getVehicleId())){
			// skip pt
		} else {
			
			Id<Person> personId = this.delegate.getVehicle2DriverEventHandler().getDriverOfVehicle( event.getVehicleId() ) ;

			if (this.delegate.getCarPersonIDs().contains(personId)) {
				// car
				LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get( event.getLinkId() ) ;
				DelayInfo delayInfo = linkInfo.getFlowQueue().getLast();
				calculateCongestion(event, delayInfo);
			}
		}
	}


	@Override
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
		
		double delayOnThisLink = delayInfo.linkLeaveTime - delayInfo.freeSpeedLeaveTime ;

		if (delayOnThisLink < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");

		} else if (delayOnThisLink == 0.) {
			// The agent was leaving the link without a delay.  Nothing to do ...

		} else {
			// The agent was leaving the link with a delay.

			LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get(event.getLinkId());
			
			List<DelayInfo> flowQueueAhead = new ArrayList<>();
			
			// go through the flow queue (proper order)
			for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().iterator() ; it.hasNext() ; ) {
				// Get the agents 'ahead' from the flow queue. The agents 'ahead' are considered as causing agents.
				DelayInfo causingAgentDelayInfo = it.next() ;
				if ( causingAgentDelayInfo.personId.equals( delayInfo.personId ) ) {
					// not charging to yourself:
					continue ;
				}
				flowQueueAhead.add(causingAgentDelayInfo);
			}
			
			for (int i = 0; i < flowQueueAhead.size() ; i++ ) {
				
				DelayInfo causingAgentDelayInfo = flowQueueAhead.get(i);

				double time1 = causingAgentDelayInfo.linkLeaveTime;
				double time2 = Double.MIN_VALUE;
				if (i == flowQueueAhead.size() - 1) {
					// last causing agent delay info
					time2 = event.getTime();
				} else {
					// following agent
					time2 = flowQueueAhead.get(i + 1).linkLeaveTime;
				}
								
				double headwayToNextAgent = time2 - time1;
				
				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "version10", causingAgentDelayInfo.personId, 
						delayInfo.personId, headwayToNextAgent, event.getLinkId(), causingAgentDelayInfo.linkEnterTime );
				this.events.processEvent(congestionEvent); 

				this.delegate.addToTotalInternalizedDelay(headwayToNextAgent);
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
