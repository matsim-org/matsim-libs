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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

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

public final class CongestionHandlerImplV4  implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
PersonDepartureEventHandler,
PersonArrivalEventHandler,
PersonStuckEventHandler
{

	final static Logger log = Logger.getLogger(CongestionHandlerImplV4.class);
	private final Scenario scenario;
	private final EventsManager events;
	private final List<Id<Vehicle>> nonCarVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<>();
	private int roundingErrorWarnCount =0;
	private int sameAffectedCausingAgentWarnCount =0;

	private double totalInternalizedDelay = 0.0;
	private double totalDelay = 0.0;
	private double delayNotInternalized_roundingErrors = 0.0;

	private Map<Id<Person>,Integer> personId2legNr = new HashMap<>() ;
	private Map<Id<Person>,Integer> personId2linkNr = new HashMap<>() ;

	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		if (this.scenario.getConfig().transit().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

		if (this.scenario.getNetwork().getLinks().size()==0) {
			throw new RuntimeException("There are no links in scenario thus aborting...");
		}

		storeLinkInfo();
	}

	@Override
	public void reset(int iteration) {
		this.nonCarVehicleIDs.clear();
		this.totalDelay = 0.0;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.linkId2congestionInfo.clear();

		storeLinkInfo();
	}

	private void storeLinkInfo(){
		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

			linkInfo.setLinkId(link.getId());
			linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));

			double flowCapacity_CapPeriod = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_CapPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
			linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);

			this.linkId2congestionInfo.put(link.getId(), linkInfo);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects "
				+ "because there are no linkLeaveEvents for stucked agents.: \n" + event.toString());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
			linkInfo.getEnteringAgents().add(event.getPersonId());
		} else {
			this.nonCarVehicleIDs.add(Id.create(event.getPersonId(),Vehicle.class));
		}
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
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			this.linkId2congestionInfo.get(event.getLinkId()).getEnteringAgents().remove(event.getPersonId());
		} else {
			this.nonCarVehicleIDs.remove(Id.create(event.getPersonId(),Vehicle.class)); // same person can have different travel modes in different trips
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> enteredPerson = Id.createPersonId(event.getVehicleId());
		if (!this.nonCarVehicleIDs.contains(event.getVehicleId())){ // car
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getEnteringAgents().add(enteredPerson);
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		}	
		// ---
		int linkNr = this.personId2linkNr.get( event.getPersonId() ) ;
		this.personId2linkNr.put( event.getPersonId(), linkNr + 1 ) ;
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());

		if (!this.nonCarVehicleIDs.contains(event.getVehicleId())){ //only car

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			double delay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(personId);

			if(delay<0) throw new RuntimeException("Delays can not be negative, do a consistency check. Aborting ...");
			else if(delay > 0.0) {
				linkInfo.getPersonId2DelaysToPayFor().put(personId, delay);
				this.totalDelay += delay;

				updateAgentsTracking(event);

				startProcessingDelay(event);
			}

			linkInfo.getEnteringAgents().remove(personId);
			linkInfo.getLeavingAgents().add(personId);
			linkInfo.setLastLeavingAgent(personId);
			//linkInfo.getPersonId2freeSpeedLeaveTime().remove(personId);
			linkInfo.setLastLeaveTime(event.getTime());
		}
	}

	private void updateAgentsTracking(LinkLeaveEvent event){
		
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		Map<Id<Person>,Double> personId2FreeSpeedTime = linkInfo.getPersonId2freeSpeedLeaveTime();
		
		double minTimeHeadway = linkInfo.getMarginalDelayPerLeavingVehicle_sec();

		List<Id<Person>> agentsAlreadyLeft = new ArrayList<Id<Person>>(linkInfo.getLeavingAgents()); 
		Collections.reverse(agentsAlreadyLeft);
		// (leavingAgents is something like all agents in queue before current agent)
		
		// if the difference between free speed leave time of two agents is more than min time headway, no delay 
		double freeSpeedLeaveTimeOfNowAgent = personId2FreeSpeedTime.get(Id.createPersonId(event.getVehicleId()));
		
		for( Id<Person> agentId : agentsAlreadyLeft ){
			// (so we go through some (see below) agents in queue before current agent ...)
			
			double freeSpeedLeaveTimeAgentInList = personId2FreeSpeedTime.get(agentId);
			double timeHeadway = freeSpeedLeaveTimeOfNowAgent - freeSpeedLeaveTimeAgentInList;
			
			if (timeHeadway < minTimeHeadway){
				linkInfo.getAgentsCausingFlowDelays().add(agentId);
				// (... and add them to the agents causing FLOW delays if their time headway is small)
				// yyyy but I don't find the timeHeadway < minTimeHeadway very logical.  This can really
				// only happen if there is spillback and downstream fluctuations (movement from buffer
				// across node). kai, sep'15
			}
			freeSpeedLeaveTimeOfNowAgent = freeSpeedLeaveTimeAgentInList;
		}
		
		if (linkInfo.getLeavingAgents().size() != 0) {
			// Clear tracking of persons leaving that link previously.
			double lastLeavingFromCurrentLink = linkInfo.getLastLeaveTime();
			double earliestLeaveTime = lastLeavingFromCurrentLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			
			if (event.getTime() > Math.floor(earliestLeaveTime)+1 ){// Flow congestion has disappeared on that link.
				// Deleting the information of agents previously leaving that link.
				// yyyy however, agentsCausingFlowDelays will survive?!?!
				linkInfo.getLeavingAgents().clear();
			}
		}
		
		/*
		 *  TODO [AA] Might be make more sense to store persons only in one map (getAgentsCausingFlowDelays) and
		 *  use the above if statement to get the spill back causing link.
		 */
		
		//remove agents present in both the lists to avoid duplicate congestion events.		
		linkInfo.getAgentsCausingFlowDelays().removeAll(linkInfo.getLeavingAgents());
	}
	
	
	/**
	 * @param event
	 */
	private void startProcessingDelay(LinkLeaveEvent event) {

		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId());

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		List<Id<Person>> leavingAgentsList = new ArrayList<Id<Person>>(linkInfo.getLeavingAgents());

		if(leavingAgentsList.isEmpty()) {
			if(linkInfo.getAgentsCausingFlowDelays().isEmpty()){ 
				// agent is not delayed due to flow cap
			} else {
				// mix of flow and spill back delays, because time headway is lesser than minimum allowed time headway
				//charge such persons. These are not in the leaving agents list.
				// see test CombinedFlowAndStorageDelayTestV4

				List<Id<Person>> agentsCausingFlowDelays = new ArrayList<Id<Person>>(linkInfo.getAgentsCausingFlowDelays());
//				Collections.reverse(agentsCausingFlowDelays); // already stored in the reverse order, see updateAgentsTracking(..) method

				double delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);
				Iterator<Id<Person>> causingAgents = agentsCausingFlowDelays.iterator();
				while(delayToPayFor > 0 && causingAgents.hasNext()) {
					Id<Person> causingPerson = causingAgents.next();
					chargeAndThrowEvents(event, causingPerson, event.getLinkId());
					delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);
				}
			}
			// get spill back causing link now
			Id<Link> spillBackCausingLink = getNextLinkInRoute(delayedPerson);
			// remove previous occurance of this spillback causing link if any in order to update the order
			if(linkInfo.getSpillBackCausingLinks().contains(spillBackCausingLink)) linkInfo.getSpillBackCausingLinks().remove(spillBackCausingLink);	
			linkInfo.getSpillBackCausingLinks().add(spillBackCausingLink);  			

		} else {//flow cap delays
			Collections.reverse(leavingAgentsList);
			double delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);
			Iterator< Id<Person>> causingAgentsIterator = leavingAgentsList.iterator();

			while ( delayToPayFor > 0 && causingAgentsIterator.hasNext()){
				Id<Person> causingAgent = causingAgentsIterator.next();
				chargeAndThrowEvents(event, causingAgent,event.getLinkId());
				delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);
			}
		}

		double delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);

		if(delayToPayFor == 0) return;
		else if (delayToPayFor <= 1){
			this.roundingErrorWarnCount++;
			this.delayNotInternalized_roundingErrors += delayToPayFor;
			this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().put(delayedPerson, 0.0);

			// delays <= 1 are assumed to be rounding error.
			if(this.roundingErrorWarnCount==1){
				log.warn("Delays less than or equal to 1 are assumed to be rounding error.");
				log.warn(Gbl.ONLYONCE);
			}
		}
		else {
			//	Person have spill back delays. Internalizing such delays.
			identifyAndProcessSpillBackCausingLink(event, linkInfo);
		}

		delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));

		if(delayToPayFor > 0) {
			log.warn("Delay to pay for is "+delayToPayFor+". Including them in non Internalizing delays. This happened during event \n "+event.toString());
		}
	}

	private void identifyAndProcessSpillBackCausingLink(LinkLeaveEvent event, LinkCongestionInfo linkCongestionInfo){

		List<Id<Link>> spillBackCausingLinks = new ArrayList<>(linkCongestionInfo.getSpillBackCausingLinks());

		Collections.reverse(spillBackCausingLinks);

		if(spillBackCausingLinks.isEmpty()) return;

		Iterator<Id<Link>> spillBackLinkIterator = spillBackCausingLinks.iterator();

		double delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));

		while(delayToPayFor > 0 && spillBackLinkIterator.hasNext()) {

			Id<Link> spillBackCausingLink = spillBackLinkIterator.next();
			processSpillBackDelays(event, spillBackCausingLink);

			delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));

			if(delayToPayFor == 0) break;
			else if(delayToPayFor > 0 ) identifyAndProcessSpillBackCausingLink(event, this.linkId2congestionInfo.get(spillBackCausingLink));
		};
	}

	/**
	 * @param event to throw congestion events and to get delayed person and its information.
	 * @param spillBackCausingLink
	 */
	private void processSpillBackDelays(LinkLeaveEvent event, Id<Link> spillBackCausingLink){

		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId().toString());
		Id<Link> personDelayedOnLink = event.getLinkId();

		LinkCongestionInfo spillBackCausingLinkInfo = this.linkId2congestionInfo.get(spillBackCausingLink);

		List<Id<Person>> personsEnteredOnSpillBackCausingLink = new ArrayList<Id<Person>>(spillBackCausingLinkInfo.getEnteringAgents()); 
		Collections.reverse(personsEnteredOnSpillBackCausingLink);

		double delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);

		Iterator<Id<Person>> enteredPersonsListIterator = personsEnteredOnSpillBackCausingLink.iterator();

		while(delayToPayFor > 0  && enteredPersonsListIterator.hasNext()){
			Id<Person> personToBeCharged = enteredPersonsListIterator.next();

			chargeAndThrowEvents(event, personToBeCharged,spillBackCausingLink);

			delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
		}

		if(delayToPayFor>0){
			List<Id<Person>> personsLeftSpillBackCausingLink = new ArrayList<Id<Person>>(spillBackCausingLinkInfo.getLeavingAgents());
			Collections.reverse(personsLeftSpillBackCausingLink);
			Iterator<Id<Person>> prsnLftSpillBakCauinLinkItrtr = personsLeftSpillBackCausingLink.iterator();

			while( delayToPayFor > 0. && prsnLftSpillBakCauinLinkItrtr.hasNext()){ // again charged for flow cap of link
				Id<Person> chargedPersonId = prsnLftSpillBakCauinLinkItrtr.next();

				chargeAndThrowEvents(event, chargedPersonId, spillBackCausingLink);

				delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
			}
		}
	}

	private void chargeAndThrowEvents(LinkLeaveEvent event, Id<Person> causingPerson, Id<Link> causingPersonOnLink){
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId());
		double delayToPayFor = linkInfo.getPersonId2DelaysToPayFor().get(delayedPerson);

		double marginalDelaysPerLeavingVehicle = this.linkId2congestionInfo.get(causingPersonOnLink).getMarginalDelayPerLeavingVehicle_sec();

		// decide how much to charge
		double personChargedFor =0;

		if(delayToPayFor > marginalDelaysPerLeavingVehicle) {
			personChargedFor = marginalDelaysPerLeavingVehicle;
			delayToPayFor = delayToPayFor - marginalDelaysPerLeavingVehicle;
		} else {
			personChargedFor = delayToPayFor;
			delayToPayFor = 0;
		}

		if (delayedPerson.toString().equals(causingPerson.toString())) {
			if(this.sameAffectedCausingAgentWarnCount == 0){
				log.warn("Causing agent and affected agents "+causingPerson.toString()+" are same. Though, the situation is rare but possible. Agent is charging himself.");
				log.warn(Gbl.ONLYONCE);
				this.sameAffectedCausingAgentWarnCount++;
			}
		} 

		this.totalInternalizedDelay = this.totalInternalizedDelay + personChargedFor;
		//throw event --- using the time when the causing agent entered the link as emergence time
		CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "flowStorageCapacity", causingPerson, delayedPerson, personChargedFor, causingPersonOnLink,
				this.linkId2congestionInfo.get(causingPersonOnLink).getPersonId2linkEnterTime().get(causingPerson) );
		this.events.processEvent(congestionEvent);
		linkInfo.getPersonId2DelaysToPayFor().put(delayedPerson, delayToPayFor);
	}

	private Id<Link> getNextLinkInRoute(Id<Person> personId){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();
		Leg leg = TripStructureUtils.getLegs(planElements).get( this.personId2legNr.get( personId ) ) ;
		return ((NetworkRoute) leg.getRoute()).getLinkIds().get( this.personId2linkNr.get( personId ) ) ;
	}

	public void writeCongestionStats(String fileName) {
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write("Total delay [hours];" + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.totalInternalizedDelay / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.delayNotInternalized_roundingErrors / 3600.);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		log.info("Congestion statistics written to " + fileName);		
	}

	public double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

	public double getTotalDelay() {
		return totalDelay;
	}

	public double getDelayNotInternalizedRoundingErrors() {
		return delayNotInternalized_roundingErrors;
	}
}
