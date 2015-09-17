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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.events.CongestionEvent;

/** 
 * In this implementation, the delay is allocated to all agents ahead that are in the queue.
 * 
 * @author ikaddoura
 *
 */
public final class CongestionHandlerImplV7 implements CongestionHandler {

	private final static Logger log = Logger.getLogger(CongestionHandlerImplV7.class);

	private CongestionHandlerBaseImpl delegate;

	private double totalDelay = 0.;

	private Scenario scenario;
	private EventsManager events;
	
	public CongestionHandlerImplV7(EventsManager events, Scenario scenario) {
		this.scenario = scenario;
		this.events = events;
		this.delegate = new CongestionHandlerBaseImpl(events, scenario);
	}

	@Override
	public final void reset(int iteration) {

		delegate.reset(iteration);
		this.totalDelay = 0.;
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
	public final void handleEvent(Wait2LinkEvent event) {
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
			bw.write("Total delay [hours];" + this.totalDelay/ 3600.);
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
		return this.totalDelay;
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {

		if (this.delegate.getPtVehicleIDs().contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else { // car!
			Id<Person> personId = this.delegate.getVehicleId2personId().get( event.getVehicleId() ) ;

			LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo(event.getLinkId(), delegate.getLinkId2congestionInfo(), scenario);

			AgentOnLinkInfo agentInfo = linkInfo.getAgentsOnLink().get( personId ) ;

			DelayInfo delayInfo = new DelayInfo.Builder().setPersonId( personId ).setLinkEnterTime( agentInfo.getEnterTime() )
					.setFreeSpeedLeaveTime(agentInfo.getFreeSpeedLeaveTime()).setLinkLeaveTime( event.getTime() ).build() ;

			CongestionHandlerBaseImpl.updateFlowAndDelayQueues(event.getTime(), delayInfo, linkInfo );

			calculateCongestion(event, delayInfo);

			linkInfo.getFlowQueue().add( delayInfo ) ;
			linkInfo.memorizeLastLinkLeaveEvent( event );

			linkInfo.getAgentsOnLink().remove( personId ) ;
		}
	}


	@Override
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
		LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getAgentsOnLink().get(delayInfo.personId).getFreeSpeedLeaveTime();

		// global book-keeping:
		this.totalDelay += delayOnThisLink;

		if (delayOnThisLink < 0.) {
			throw new RuntimeException("The delay is below 0. Aborting...");

		} else if (delayOnThisLink == 0.) {
			// The agent was leaving the link without a delay.  Nothing to do ...

		} else {
			// The agent was leaving the link with a delay.

			// go throw the flow queue and charge all causing agents with the delay on this link
			for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().descendingIterator() ; it.hasNext() ; ) {
				DelayInfo causingAgentDelayInfo = it.next() ;
	
				// let each agent in the queue pay for the total delay
				double allocatedDelay = delayOnThisLink;

				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "version7", causingAgentDelayInfo.personId, 
						delayInfo.personId, allocatedDelay, event.getLinkId(), causingAgentDelayInfo.linkEnterTime );
				this.events.processEvent(congestionEvent); 

				this.delegate.addToTotalInternalizedDelay(allocatedDelay);
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
}
