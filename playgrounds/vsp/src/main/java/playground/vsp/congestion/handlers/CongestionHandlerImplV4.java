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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

//import playground.vsp.congestion.CombinedFlowAndStorageDelayTest;
//cannot import from test area: will fail on build server.  kai, sep'15

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

public final class CongestionHandlerImplV4  extends AbstractCongestionHandler implements PersonArrivalEventHandler,
Wait2LinkEventHandler {

	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		super(events, scenario);
		this.scenario = scenario;
	}

	private Scenario scenario;
	private Map<Id<Link>,Deque<Id<Link>>> linkId2SpillBackCausingLinks = new HashMap<>();

	/**
	 * This list is used to store entering agents, (1) which can not be cleared in personId2EnteringAgents map because 
	 * linkEnterTime is required later (2) and these agents should not be charged since they already left the link. 
	 */
	private Map<Id<Link>,List<Id<Person>>> linkId2ExcludeEnteringAgentsList = new HashMap<>();

	@Override
	public void handleEvent(PersonArrivalEvent event){
		super.handleEvent(event);

		if(event.getLegMode().equals(TransportMode.car)) {
			this.getLinkId2congestionInfo().get(event.getLinkId()).getPersonId2linkEnterTime().remove(event.getPersonId());

			for (Id<Link>linkId : this.linkId2ExcludeEnteringAgentsList.keySet()){
				// This is necessary so that an agent once charged can be charged again if causing storageDelay.
				this.linkId2ExcludeEnteringAgentsList.get(linkId).remove(event.getPersonId());
			}
		}
	}

	@Override
	final void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {

		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());
		
		double delayOnTheLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		if(delayOnTheLink==0) return;

		delayOnTheLink = checkForFlowDelayWhenLeavingAgentsListIsEmpty(event, delayInfo, delayOnTheLink);
		// yyyy check if we really need this

		if( linkInfo.getFlowQueue().isEmpty()){
			// (flow queue contains only those agents where time headway approx 1/cap. So we get here only if we are spillback delayed, 
			// and our own bottleneck is not active)

			Id<Person> driverId = this.vehicleId2personId.get( event.getVehicleId() ) ;
			Id<Link> spillBackCausingLink = getDownstreamLinkInRoute(driverId);

			memorizeSpillBackCausingLinkForCurrentLink(event.getLinkId(), spillBackCausingLink);
		} 

		// charge for the flow delay; remaining delays are said to be storage delay
		// (might be able to skip this if flow queue is empty, but maybe do this just in case ...)
		double storageDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), event.getVehicleId(), delayOnTheLink);

		if(this.isCalculatingStorageCapacityConstraints() && storageDelay > 0){

			// !! calling the following method is the big difference to V3 !!!
			double remainingStorageDelay = allocateStorageDelayToDownstreamLinks(storageDelay, event.getLinkId(), event);

			if(remainingStorageDelay > 0.) {
				throw new RuntimeException(remainingStorageDelay+" sec delay is not internalized. Aborting...");
			}

		} else {
			this.addToDelayNotInternalized_storageCapacity(storageDelay);
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

	private double  allocateStorageDelayToDownstreamLinks(double remainingDelay, Id<Link> linkId, LinkLeaveEvent event){
		final Deque<Id<Link>> spillBackCausingLinks = this.linkId2SpillBackCausingLinks.get(linkId);

		// if linkId is not registered (by other vehicles) as having spill-back, we return:
		if( spillBackCausingLinks==null || spillBackCausingLinks.isEmpty() ) {
			return remainingDelay;
		}

		// Go through all those outgoing links that have (ever) reported a blockage ...
		for ( Iterator<Id<Link>> it = spillBackCausingLinks.descendingIterator() ; it.hasNext() && remainingDelay > 0. ; ) {
			Id<Link> spillBackCausingLink = it.next();

			remainingDelay = processSpillbackDelays(remainingDelay, event, spillBackCausingLink);

			if(remainingDelay<=0) {
				break;
			} else {
				// !! this is where the recursive call is !!
				remainingDelay = allocateStorageDelayToDownstreamLinks(remainingDelay, spillBackCausingLink, event);
				// yy this looks like depth-first allocation to me; should probably also try breadth-first allocation. kai, sep'15
			}
		}

		return remainingDelay;
	}

	private double processSpillbackDelays(double remainingDelay, LinkLeaveEvent event, Id<Link> spillbackCausingLink){
		Id<Person> affectedPersonId = event.getPersonId();

		// first charge for agents present on the link or in other words agents entered on the link
		LinkCongestionInfo spillbackLinkCongestionInfo = this.getLinkId2congestionInfo().get(spillbackCausingLink);

		final LinkedList<AgentOnLinkInfo> agentsOnLinksAsDeque = new LinkedList<AgentOnLinkInfo>( spillbackLinkCongestionInfo.getAgentsOnLink().values() );
		for ( Iterator<AgentOnLinkInfo> it = agentsOnLinksAsDeque.descendingIterator() ; it.hasNext() ; ) {
			// (didn't find an easier way both to be able to remove by Id and to iterate from back. sep'15, kai)
			AgentOnLinkInfo agentInfo = it.next() ;
			Id<Person> causingPersonId = agentInfo.getPersonId() ;
			//			if ( !this.linkId2ExcludeEnteringAgentsList.get( spillbackCausingLink ).contains( causingPersonId ) ) 
			{
				double agentDelay = Math.min(spillbackLinkCongestionInfo.getMarginalDelayPerLeavingVehicle_sec(), remainingDelay);

				CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "StorageCapacity", causingPersonId, affectedPersonId, 
						agentDelay, spillbackCausingLink, spillbackLinkCongestionInfo.getPersonId2linkEnterTime().get(causingPersonId) );
				this.getEventsManager().processEvent(congestionEvent); 

				this.addToTotalInternalizedDelay(agentDelay);

				remainingDelay = remainingDelay - agentDelay;
				if (remainingDelay <=0 ) {
					break ;
				}
			}
		}

		if(remainingDelay>0){
			// now charge agents that have already left:
			// TODO here one can argue that delay are due to storageCapacity but congestion event from this method will say delay due to flowStorageCapacity
			remainingDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), spillbackCausingLink, event.getVehicleId(), remainingDelay);
		}

		return remainingDelay;
	}

	/**
	 * @param event
	 * <p> This is a very special case (possible only at intersections), where agent is first delayed due to flow Capacity and then delayed due to storage capacity.
	 * For this, the time gap (tau) between freeSpeedLinkLeave time of two consecutive vehicles and leavingAgents list are checked. 
	 * Thus, 
	 * <p> <code> if( leavingAgents.isEmpty() ) { checkForTimeGap} </code>
	 * <p> 
	 * <p> A test is available, see {@link playground.vsp.congestion.CombinedFlowAndStorageDelayTest}.
	 * @param affectedAgentDelayInfo TODO
	 * @param delayOnTheLink TODO
	 */
	private double checkForFlowDelayWhenLeavingAgentsListIsEmpty(LinkLeaveEvent event, DelayInfo affectedAgentDelayInfo, double remainingDelay ){
		// this class, when I found it, started from the personId2freeSpeedLeaveTime data structure, and somehow reduced it, presumably
		// by those who had not yet left he link, those who had in the meantime arrived, "self", etc.  I say "presumably" because
		// I did not fully find out.  The tests, however, do not fail if one simply takes the "delay queue" as input. kai, sep'15

		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());

		if(linkInfo.getFlowQueue().isEmpty()){
			// (i.e. time headway of current vehicle to previous vehicle > 1/cap)

			double timeGap = 0;

			for ( Iterator<DelayInfo> it = linkInfo.getDelayQueue().descendingIterator() ; it.hasNext() ; ) {
				DelayInfo causingAgentDelayInfo = it.next() ;
				Id<Person> causingAgentId = causingAgentDelayInfo.personId ;


				timeGap = affectedAgentDelayInfo.freeSpeedLeaveTime - causingAgentDelayInfo.freeSpeedLeaveTime ;

				if(timeGap < linkInfo.getMarginalDelayPerLeavingVehicle_sec()) {
					double agentDelay = Math.min(linkInfo.getMarginalDelayPerLeavingVehicle_sec(), remainingDelay);
					// this person should be charged here.
					CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "FlowCapacity", causingAgentId, 
							event.getPersonId(), agentDelay, event.getLinkId(), linkInfo.getPersonId2linkEnterTime().get(event.getPersonId()) );
					this.getEventsManager().processEvent(congestionEvent); 
					this.addToTotalInternalizedDelay(agentDelay);

					remainingDelay = remainingDelay - agentDelay;
				} else {
					// since timeGap is higher than marginalFlowDelay, no need for further look up.
					break;
				}
			}
		}
		return remainingDelay;
	}

	private Id<Link> getDownstreamLinkInRoute(Id<Person> personId){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();
		Leg leg = TripStructureUtils.getLegs(planElements).get( this.personId2legNr.get( personId ) ) ;
		return ((NetworkRoute) leg.getRoute()).getLinkIds().get( this.personId2linkNr.get( personId ) ) ;
	}

}
