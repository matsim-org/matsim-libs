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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * This handler calculates delays (caused by the flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) At link Leave event, delay is calculated which is the difference of actual leaving time and the leaving time according to free speed.
 * 2) Persons leaving link are identified and these are charged until delay =0; if there is no leaving agents (=spill back delays), spill back causing link is stored
 * 3) Subsequently spill back delays are processed by identifying spill back causing link(s) and charging entering agents (i.e. persons currently on the link) and leaving agents alternatively until delay=0;
 * 
 * @author amit
 * 
 * warnings and structure is kept same as in previous implementation of congestion pricing by ihab.
 *
 */

public final class CongestionHandlerImplV4 implements  CongestionHandler {
	private final static Logger log = Logger.getLogger(CongestionHandlerImplV4.class);

	private CongestionHandlerBaseImpl delegate;

	private Scenario scenario;
	private EventsManager events;

	private double totalDelay = 0.0;

	private Map<Id<Link>,Deque<Id<Link>>> linkId2SpillBackCausingLinks = new HashMap<>();
	private Map<Id<Person>,Integer> personId2legNr = new HashMap<>() ;
	private Map<Id<Person>,Integer> personId2linkNr = new HashMap<>() ;


	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		this.scenario = scenario;
		this.events = events;
		this.delegate = new CongestionHandlerBaseImpl(events, scenario);
	}

	@Override
	public final void reset(int iteration) {
		delegate.reset(iteration);

		this.totalDelay = 0.0;

		this.linkId2SpillBackCausingLinks.clear();
		this.personId2legNr.clear();
		this.personId2linkNr.clear();
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

		//--
		final Integer cnt = this.personId2legNr.get( event.getPersonId() );
		if ( cnt == null ) {
			this.personId2legNr.put( event.getPersonId(), 0 ) ; // start counting with zero!!
		} else {
			this.personId2legNr.put( event.getPersonId(), cnt + 1 ) ; 
		}
		this.personId2linkNr.put( event.getPersonId(), 0 ) ; // start counting with zero!!
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		delegate.handleEvent(event);
		// ---
		int linkNr = this.personId2linkNr.get( event.getPersonId() ) ;
		this.personId2linkNr.put( event.getPersonId(), linkNr + 1 ) ;

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		delegate.handleEvent(event);
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
			linkInfo.getDelayQueue().add( delayInfo ) ;

			linkInfo.memorizeLastLinkLeaveEvent( event );

			//			linkInfo.getPersonId2freeSpeedLeaveTime().remove( personId ) ;
			// in V4, it is removed at agent _arrival_ and then it seems to work. 

			//			linkInfo.getPersonId2linkEnterTime().remove( personId ) ;
			// fails tests, dunno why. kai, sep'15

			linkInfo.getAgentsOnLink().remove( event.getPersonId() ) ;
		}
	}

	@Override
	public final void calculateCongestion(LinkLeaveEvent event, DelayInfo affectedAgentDelayInfo) {

		LinkCongestionInfo linkInfo = this.delegate.getLinkId2congestionInfo().get(event.getLinkId());
		double remainingDelay = event.getTime() - affectedAgentDelayInfo.freeSpeedLeaveTime ;

		// global book-keeping:
		this.totalDelay += remainingDelay;


		if( linkInfo.getFlowQueue().isEmpty() && remainingDelay > 0.0){
			// (flow queue contains only those agents where time headway approx 1/cap. So we get here only if we are spillback delayed, 
			// and our own bottleneck is not active)

			Id<Person> driverId = this.delegate.getVehicleId2personId().get( event.getVehicleId() ) ;
			Id<Link> spillBackCausingLink = getDownstreamLinkInRoute(driverId);

			memorizeSpillBackCausingLinkForCurrentLink(event.getLinkId(), spillBackCausingLink);
		} 

		// charge for the flow delay; remaining delays are said to be storage delay
		// (might be able to skip this if flow queue is empty, but maybe do this just in case ...)
		remainingDelay = this.delegate.computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(),affectedAgentDelayInfo, remainingDelay);

		if( remainingDelay > 0){

			// !! calling the following method is the big difference to V3 !!!
			remainingDelay = allocateStorageDelayToDownstreamLinks(remainingDelay, event.getLinkId(), event, affectedAgentDelayInfo);

			if(remainingDelay > 0.) {
				throw new RuntimeException( "time=" + event.getTime() + "; " + remainingDelay+" sec delay is not internalized. Aborting...");
			}
		} 
	}

	private void memorizeSpillBackCausingLinkForCurrentLink(Id<Link> currentLink, Id<Link> spillBackCausingLink) {
		if( this.linkId2SpillBackCausingLinks.containsKey( currentLink ) ) {
			/* since multiple spillback causing links are possible, thus to maintain the order correctly, 
			 * first removing the link (optional operation) and then adding it to the end of the list.
			 * Necessary in part because links are never removed from this data structure.
			 */
			this.linkId2SpillBackCausingLinks.get(currentLink).remove(spillBackCausingLink);
			this.linkId2SpillBackCausingLinks.get(currentLink).add(spillBackCausingLink);
		} else {
			this.linkId2SpillBackCausingLinks.put(currentLink, new LinkedList<Id<Link>>(Arrays.asList(spillBackCausingLink)));
		}
	}

	private double  allocateStorageDelayToDownstreamLinks(double remainingDelay, Id<Link> linkId, LinkLeaveEvent event, DelayInfo affectedAgentDelayInfo){
		final Deque<Id<Link>> spillBackCausingLinks = this.linkId2SpillBackCausingLinks.get(linkId);

		// if linkId is not registered (by other vehicles) as having spill-back, we return:
		if( spillBackCausingLinks==null || spillBackCausingLinks.isEmpty() ) {
			return remainingDelay;
		}

		// Go through all those outgoing links that have (ever) reported a blockage ...
		for ( Iterator<Id<Link>> it = spillBackCausingLinks.descendingIterator() ; it.hasNext() && remainingDelay > 0. ; ) {
			Id<Link> spillBackCausingLink = it.next();

			remainingDelay = processSpillbackDelays(remainingDelay, event, spillBackCausingLink, affectedAgentDelayInfo);

			if(remainingDelay<=0) {
				break;
			} else {
				// !! this is where the recursive call is !!
				remainingDelay = allocateStorageDelayToDownstreamLinks(remainingDelay, spillBackCausingLink, event, affectedAgentDelayInfo);
				// yy this looks like depth-first allocation to me; should probably also try breadth-first allocation. kai, sep'15
			}
		}
		return remainingDelay;
	}

	private double processSpillbackDelays(double remainingDelay, LinkLeaveEvent event, Id<Link> spillbackCausingLink, DelayInfo affectedAgentDelayInfo){
		Id<Person> affectedPersonId = event.getPersonId();

		// first charge for agents present on the link or in other words agents entered on the link
		LinkCongestionInfo spillbackLinkCongestionInfo = this.delegate.getLinkId2congestionInfo().get(spillbackCausingLink);

		final LinkedList<AgentOnLinkInfo> agentsOnLinksAsDeque = new LinkedList<AgentOnLinkInfo>( spillbackLinkCongestionInfo.getAgentsOnLink().values() );
		for ( Iterator<AgentOnLinkInfo> it = agentsOnLinksAsDeque.descendingIterator() ; remainingDelay> 0.0 && it.hasNext() ; ) {
			// (didn't find an easier way both to be able to remove by Id and to iterate from back. sep'15, kai)
			AgentOnLinkInfo agentInfo = it.next() ;
			Id<Person> causingPersonId = agentInfo.getPersonId() ;
			double agentDelay = Math.min(spillbackLinkCongestionInfo.getMarginalDelayPerLeavingVehicle_sec(), remainingDelay);

			CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "storageCapacity", causingPersonId, affectedPersonId, 
					agentDelay, spillbackCausingLink, spillbackLinkCongestionInfo.getPersonId2linkEnterTime().get(causingPersonId) );
			this.events.processEvent(congestionEvent); 

			this.delegate.addToTotalInternalizedDelay(agentDelay);

			remainingDelay = remainingDelay - agentDelay;
			if (remainingDelay <=0 ) {
				break ;
			}
		}

		if(remainingDelay>0){
			// now charge agents that have already left:
			// here one can argue that delay are due to storageCapacity but congestion event from this method will say delay due to flowStorageCapacity
			remainingDelay = this.delegate.computeFlowCongestionAndReturnStorageDelay(event.getTime(), spillbackCausingLink, affectedAgentDelayInfo, remainingDelay);
		}
		return remainingDelay;
	}

	private Id<Link> getDownstreamLinkInRoute(Id<Person> personId){
		List<PlanElement> planElements = this.scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();
		Leg leg = TripStructureUtils.getLegs(planElements).get( this.personId2legNr.get( personId ) ) ;
		return ((NetworkRoute) leg.getRoute()).getLinkIds().get( this.personId2linkNr.get( personId ) ) ;
	}

	@Override
	public double getTotalDelay() {
		return this.totalDelay;
	}

	@Override
	public double getTotalInternalizedDelay() {
		return  this.delegate.getTotalInternalizedDelay();
	}

	@Override
	public double getTotalRoundingErrorDelay() {
		return this.delegate.getDelayNotInternalized_roundingErrors();
	}

	@Override
	public void writeCongestionStats(String file) {
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
		log.info("Congestion statistics written to " + file);	
	}
}