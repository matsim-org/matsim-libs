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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import playground.vsp.congestion.LinkCongestionInfo;
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

public final class CongestionHandlerImplV4  extends AbstractCongestionHandler implements PersonArrivalEventHandler{

	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		super(events, scenario);
		this.scenario = scenario;
		this.events = events;
	}

	private Scenario scenario;
	private EventsManager events ;
	private Map<Id<Link>,List<Id<Link>>> linkId2SpillBackCausingLink = new HashMap<Id<Link>, List<Id<Link>>>();

	public void handleEvent(PersonArrivalEvent event){
		if(event.getLegMode().equals(TransportMode.car)) {
			this.getLinkId2congestionInfo().get(event.getLinkId()).getPersonId2linkEnterTime().remove(event.getPersonId());
		}
	}


	@Override
	void calculateCongestion(LinkLeaveEvent event) {

		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId());

		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnTheLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		if(delayOnTheLink==0) return;

		// identify if agent is delayed due to storage capacity only i.e. if leavingAgentsList is empty, it is storage delay.
		if( linkInfo.getLeavingAgents().isEmpty()){

			Id<Link> spillBackCausingLink = getUpstreamLinkInRoute(delayedPerson);

			/* since multiple spillback causing links are possible, thus to maintain the order correctly, 
			 * first removing the link (optional operation) and then adding it to the end of the list.
			 */
			if( this.linkId2SpillBackCausingLink.containsKey(event.getLinkId()) ) {
				this.linkId2SpillBackCausingLink.get(event.getLinkId()).remove(spillBackCausingLink);
				this.linkId2SpillBackCausingLink.get(event.getLinkId()).add(spillBackCausingLink);
			} else {
				this.linkId2SpillBackCausingLink.put(event.getLinkId(), new ArrayList<Id<Link>>(Arrays.asList(spillBackCausingLink)));
			}
		} 

		double storageDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), event.getLinkId(), event.getVehicleId(), delayOnTheLink);

		if(this.isCalculatingStorageCapacityConstraints() && storageDelay > 0){

			double remainingStorageDelay = allocateStorageDelayToUpstreamLinks(storageDelay, event.getLinkId(), event);
			if(remainingStorageDelay > 0.) throw new RuntimeException(remainingStorageDelay+" sec delay is not internalized. Aborting...");

		} else {

			this.addToDelayNotInternalized_storageCapacity(storageDelay);


		}
	}

	private double  allocateStorageDelayToUpstreamLinks(double storageDelay, Id<Link> startAllocationFromThisLink, LinkLeaveEvent event){

		double remainingDelay = storageDelay;

		if(! this.linkId2SpillBackCausingLink.containsKey(startAllocationFromThisLink)) {
			return remainingDelay;
		}
		
		List<Id<Link>> spillBackCausingLinks = new ArrayList<>(this.linkId2SpillBackCausingLink.get(startAllocationFromThisLink));
		if(spillBackCausingLinks.isEmpty()) return remainingDelay;
		Collections.reverse(spillBackCausingLinks);

		Iterator<Id<Link>> spillBackLinkIterator = spillBackCausingLinks.iterator();

		while (remainingDelay > 0. && spillBackLinkIterator.hasNext()){
			Id<Link> spillBackCausingLink = spillBackLinkIterator.next();

			remainingDelay = processSpillbackDelays(remainingDelay, event, spillBackCausingLink);

			if(remainingDelay==0) break;
			else {
				remainingDelay = allocateStorageDelayToUpstreamLinks(remainingDelay, spillBackCausingLink, event);
			}
		}

		return remainingDelay;
	}

	private double processSpillbackDelays(double delayToChargeFor, LinkLeaveEvent event, Id<Link> spillbackCausingLink){

		double remainingDelay = delayToChargeFor;
		Id<Person> affectedPerson = Id.createPersonId(event.getVehicleId().toString());

		// first charge for agents present on the link or in other words agents entered on the link
		LinkCongestionInfo spillbackLinkCongestionInfo = this.getLinkId2congestionInfo().get(spillbackCausingLink);
		List<Id<Person>> personsEnteredOnSpillBackCausingLink = new ArrayList<Id<Person>>(spillbackLinkCongestionInfo.getPersonId2freeSpeedLeaveTime().keySet()); 
		Collections.reverse(personsEnteredOnSpillBackCausingLink);

		Iterator<Id<Person>> enteredPersonsListIterator = personsEnteredOnSpillBackCausingLink.iterator();
		double marginalDelaysPerLeavingVehicle = spillbackLinkCongestionInfo.getMarginalDelayPerLeavingVehicle_sec();

		while(remainingDelay > 0  && enteredPersonsListIterator.hasNext()){

			Id<Person> causingPerson = enteredPersonsListIterator.next();

			double agentDelay = Math.min(marginalDelaysPerLeavingVehicle, remainingDelay);

			CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "StorageCapacity", causingPerson, affectedPerson, agentDelay, spillbackCausingLink,
					spillbackLinkCongestionInfo.getPersonId2linkEnterTime().get(causingPerson) );
			this.events.processEvent(congestionEvent);

			remainingDelay = remainingDelay - agentDelay;
		}

		if(remainingDelay>0){
			remainingDelay = computeFlowCongestionAndReturnStorageDelay(event.getTime(), spillbackCausingLink, event.getVehicleId(), remainingDelay);
		}

		return remainingDelay;
	}


	private Id<Link> getUpstreamLinkInRoute(Id<Person> personId){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();
		Leg leg = TripStructureUtils.getLegs(planElements).get( this.personId2legNr.get( personId ) ) ;
		return ((NetworkRoute) leg.getRoute()).getLinkIds().get( this.personId2linkNr.get( personId ) ) ;
	}

}
