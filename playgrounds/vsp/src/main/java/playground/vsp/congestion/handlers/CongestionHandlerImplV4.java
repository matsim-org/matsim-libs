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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.opengis.filter.expression.Add;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;


/**
 * This handler calculates delays (caused by the flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) At link Leave event, delay is calculated which is the difference of actual leaving time and the leaving time according to free speed.
 * 2) Persons leaving link are identified and these are charged until delay =0; if there is no leaving agents (=spill back delays), spill back causing link and person are stored
 * 3) Subsequently spill back delays are processed by identifying person in front of queue and charging entering agents (i.e. persons currently on the link) and leaving agents alternatively until delay=0;
 * 
 * @author amit
 * 
 * warnings and structure is kept same as in previous implementation of congestion pricing by ihab.
 *
 */

public class CongestionHandlerImplV4  implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler,
PersonArrivalEventHandler,
PersonStuckEventHandler
{

	final static Logger log = Logger.getLogger(CongestionHandlerImplV4.class);
	final Scenario scenario;
	final EventsManager events;
	final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<>();
	private int roundingErrorWarnCount =0;

	double totalInternalizedDelay = 0.0;
	double totalDelay = 0.0;
	double totalStorageDelay = 0.0;
	double delayNotInternalized_roundingErrors = 0.0;
	
	private List<CongestionEvent> congestionEventsList = new ArrayList<CongestionEvent>();

	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().scenario().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

		if (this.scenario.getNetwork().getLinks().size()==0) {
			throw new RuntimeException("There are no links in scenario thus aborting...");
		}
		
		storeLinkInfo();
	}

	@Override
	public void reset(int iteration) {
		this.ptVehicleIDs.clear();
		this.totalDelay = 0.0;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.linkId2congestionInfo.clear();
		this.congestionEventsList.clear();

		storeLinkInfo();

	}

	private void storeLinkInfo(){
		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

			NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
			linkInfo.setLinkId(link.getId());
			linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));

			double flowCapacity_CapPeriod = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_CapPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
			linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);

			double storageCapacity_cars = (Math.ceil((link.getLength() * link.getNumberOfLanes()) / network.getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
			linkInfo.setStorageCapacityCars(storageCapacity_cars);

			this.linkId2congestionInfo.put(link.getId(), linkInfo);
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
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
		}
	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			this.linkId2congestionInfo.get(event.getLinkId()).getEnteringAgents().remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> enteredPerson = Id.createPersonId(event.getVehicleId());
		if (this.ptVehicleIDs.contains(enteredPerson)){
			log.warn("Public transport mode and mixed traffic is not tested.");
		} else { // car
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getEnteringAgents().add(enteredPerson);
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		}	
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode and mixed traffic is not implemented yet.");
		} else {// car
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			if (linkInfo.getLeavingAgents().size() != 0) {
				// Clear tracking of persons leaving that link previously.
				double lastLeavingFromThatLink = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime());
				double earliestLeaveTime = lastLeavingFromThatLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

				if (event.getTime() > Math.floor(earliestLeaveTime)+1 ){// Flow congestion has disappeared on that link.
					// Deleting the information of agents previously leaving that link.
					linkInfo.getLeavingAgents().clear();
					linkInfo.getPersonId2linkLeaveTime().clear();
				}
			}
			startDelayProcessing(event);

			linkInfo.getEnteringAgents().remove(event.getVehicleId());
			linkInfo.getLeavingAgents().add(Id.createPersonId(event.getVehicleId()));
			linkInfo.getPersonId2linkLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
			linkInfo.setLastLeavingAgent(Id.createPersonId(event.getVehicleId()));
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());
		}
	}

	/**
	 * @param event
	 * This method first charge for flow storage delays  
	 * and then if spill back delays are present, process them.
	 */
	private void startDelayProcessing(LinkLeaveEvent event) {
		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId().toString());
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(delayedPerson);

		if (delayOnThisLink==0) return; 
		else if(delayOnThisLink<0) throw new RuntimeException("Delays can not be negative, do a consistency check.");
		else {
			linkInfo.getPersonId2DelaysToPayFor().put(delayedPerson, delayOnThisLink);
			this.totalDelay = this.totalDelay + delayOnThisLink;

			List<Id<Person>> leavingAgentsList = new ArrayList<Id<Person>>(linkInfo.getLeavingAgents());
			Collections.reverse(leavingAgentsList);

			if(!leavingAgentsList.isEmpty()){ // flow cap delays
				double delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(delayedPerson);
				Iterator< Id<Person>> personIdListIterator = leavingAgentsList.iterator();

				while (personIdListIterator.hasNext() && delayToPayFor>0){
					Id<Person> personToBeChargedId = personIdListIterator.next();

					chargingPersonAndThrowingEvents(event, personToBeChargedId,event.getLinkId());

					delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(event.getVehicleId());
				}
			} else {//spill back delays
				Id<Link> spillBackCausingLink = getNextLinkInRoute(delayedPerson, event.getLinkId(), event.getTime());
				linkInfo.getPersonId2CausingLinkId().put(delayedPerson, spillBackCausingLink);
			}

			double delayToPayFor=this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(delayedPerson);

			if(delayToPayFor == 0) return;
			if(delayToPayFor > 0 && delayToPayFor <=1){
				if(roundingErrorWarnCount<=5){
					roundingErrorWarnCount++;
					log.warn(delayToPayFor + " seconds are not internalized assuming these delays due to rounding errors. \n ");
					if(roundingErrorWarnCount==5) log.warn(Gbl.FUTURE_SUPPRESSED);
				}
				delayToPayFor=0;
				this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().put(delayedPerson, 0.0);
				this.delayNotInternalized_roundingErrors +=delayToPayFor;
				return;
			} else {
				//	Person have spill back delays. Internalizing such delays.
				Id<Person> personInFrontOfQ = Id.createPersonId(event.getVehicleId().toString());
				Id<Link> personOnThisLink = event.getLinkId();

				do {
					Tuple<Id<Person>,Id<Link>> firstPersonOnCausingLink =  processSpillBackDelays(event,personInFrontOfQ,personOnThisLink);
					delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));
					personInFrontOfQ = firstPersonOnCausingLink.getFirst();
					personOnThisLink = firstPersonOnCausingLink.getSecond();
				} while(delayToPayFor > 0.);
			}
		}
	}

	/**
	 * @param event to throw money events and to get delayed person and its information.
	 * @param personInFrontOfQ 
	 * @param personOnThisLink
	 * @return an array of size two; first element is person who is in front of the Q(link) 
	 * and other is spill back causing link
	 */
	private Tuple<Id<Person>,Id<Link>> processSpillBackDelays(LinkLeaveEvent event, Id<Person> personInFrontOfQ, Id<Link> personOnThisLink){

		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId().toString());
		Id<Link> personDelayedOnLink = event.getLinkId();

		//		identify delayed Person
		Id<Link> spillBackCausingLink  = identifySpillBackCausingLink(personOnThisLink, personInFrontOfQ);

		LinkCongestionInfo spillBackCausingLinkInfo = this.linkId2congestionInfo.get(spillBackCausingLink);

		List<Id<Person>> personsEnteredOnSpillBackCausingLink = new ArrayList<Id<Person>>(spillBackCausingLinkInfo.getEnteringAgents()); 
		
		Collections.reverse(personsEnteredOnSpillBackCausingLink);

		double delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);

		Iterator<Id<Person>> enteredPersonsListIterator = personsEnteredOnSpillBackCausingLink.iterator();

		while(enteredPersonsListIterator.hasNext() && delayToPayFor>0){
			Id<Person> personToBeCharged = enteredPersonsListIterator.next();

			chargingPersonAndThrowingEvents(event, personToBeCharged,spillBackCausingLink);

			delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
		}

		List<Id<Person>> personsLeftSpillBackCausingLink = new ArrayList<Id<Person>>();
		if(delayToPayFor>0){
			personsLeftSpillBackCausingLink.addAll(spillBackCausingLinkInfo.getLeavingAgents());
			Collections.reverse(personsLeftSpillBackCausingLink);
			Iterator<Id<Person>> prsnLftSpillBakCauinLinkItrtr = personsLeftSpillBackCausingLink.iterator();

			while(prsnLftSpillBakCauinLinkItrtr.hasNext()&&delayToPayFor>0.){ // again charged for flow cap of link
				Id<Person> chargedPersonId = prsnLftSpillBakCauinLinkItrtr.next();

				chargingPersonAndThrowingEvents(event, chargedPersonId, spillBackCausingLink);

				delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
			}
		}

		delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(delayedPerson);
		
		Tuple<Id<Person>,Id<Link>> firstPersonOnCausingLink = new Tuple<Id<Person>, Id<Link>>(Id.createPersonId("NA"), Id.createLinkId("NONE"));
		if(delayToPayFor == 0) ;
		else if(personsEnteredOnSpillBackCausingLink.size()==0) {
			// only happens if some other event happens in same time step affect the spill back causing links.
				log.warn("There is no agent present on the link "+spillBackCausingLink+" and delay2Pay4 is "+delayToPayFor+". Situation happend during event "+event.toString());
				log.warn("Thus assuming the last left agent as front of the queue. This happens very rare for very short links.");
				firstPersonOnCausingLink = new Tuple<Id<Person>, Id<Link>>(personsLeftSpillBackCausingLink.get(0), spillBackCausingLink);
		} else {
			firstPersonOnCausingLink = new Tuple<Id<Person>, Id<Link>>(personsEnteredOnSpillBackCausingLink.get(personsEnteredOnSpillBackCausingLink.size()-1), spillBackCausingLink);
		}
		return firstPersonOnCausingLink;
	}

	/**
	 * @param personOnThisLink
	 * @return adjacent link, who caused spill back delays.
	 */
	private Id<Link> identifySpillBackCausingLink(Id<Link> personOnThisLink, Id<Person> thisPerson) {
		Id<Link> spillBackCausingLink = Id.createLinkId("NA");
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(personOnThisLink);
		if(linkInfo.getPersonId2CausingLinkId().containsKey(thisPerson)){//leaving agents list was empty
			spillBackCausingLink = linkInfo.getPersonId2CausingLinkId().get(thisPerson);
		} else {
			List<Id<Person>> personLeavingList = new ArrayList<Id<Person>>(linkInfo.getLeavingAgents());
			Collections.reverse(personLeavingList);
			for(Id<Person> id:personLeavingList){
				if(linkInfo.getPersonId2CausingLinkId().containsKey(id)){
					spillBackCausingLink = linkInfo.getPersonId2CausingLinkId().get(id);
					break;
				}
			}
			if (spillBackCausingLink.equals(Id.create("NA",Link.class))) throw new RuntimeException("Spill back causing link is not identified. Somethign went wrong.");
		}
		return spillBackCausingLink;
	}

	private void chargingPersonAndThrowingEvents(LinkLeaveEvent event, Id<Person> causingPerson, Id<Link> causingPersonOnLink){
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
		
		//throw event
		if (delayedPerson.toString().equals(causingPerson.toString())) {
			System.out.println("\n \n \t \t Error \n \n");
			log.error("Causing agent and affected agents "+causingPerson.toString()+" are same. Delays at this point is "+delayToPayFor+" sec.");
			return;
			//throw new RuntimeException("The causing agent and the affected agent are the same (" + personToBeCharged.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
		} else {
			// using the time when the causing agent entered the link
			this.totalInternalizedDelay = this.totalInternalizedDelay + personChargedFor;
			CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "flowStorageCapacity", causingPerson, delayedPerson, personChargedFor, causingPersonOnLink,
					this.linkId2congestionInfo.get(causingPersonOnLink).getPersonId2linkEnterTime().get(causingPerson) );
			this.events.processEvent(congestionEvent);
			this.congestionEventsList.add(congestionEvent);
//			System.out.println(congestionEvent.toString());
		}
		linkInfo.getPersonId2DelaysToPayFor().put(delayedPerson, delayToPayFor);
	}

	/**
	 * @param time if person have same 'next link in route' more than one time, given time is compared with 
	 * activity end time to get the true 'next link in route'.
	 * @return next link in the route of the person, which is currently on given link.
	 */
	private Id<Link> getNextLinkInRoute(Id<Person> personId, Id<Link> linkId, double time){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();

		Map<NetworkRoute, List<Id<Link>>> nRoutesAndLinkIds = new LinkedHashMap<NetworkRoute, List<Id<Link>>>(); // save all routes and links in each route
		List<Double> activityEndTimes = new ArrayList<Double>();
		for(PlanElement pe :planElements){
			
			if(pe instanceof Leg){
			
				NetworkRoute nRoute = ((NetworkRoute)((Leg)pe).getRoute()); 
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				linkIds.add(nRoute.getStartLinkId());
				linkIds.addAll(nRoute.getLinkIds());  
				linkIds.add(nRoute.getEndLinkId());
				nRoutesAndLinkIds.put(nRoute, linkIds);
			
			} else if(pe instanceof Activity){
				
				double actEndTime = ((Activity)pe).getEndTime();
				activityEndTimes.add(actEndTime);
				
			}
		}

		Id<Link> nextLinkInRoute =  Id.create("NA",Link.class);
		List<Id<Link>> nextLinksInRoutes = new ArrayList<Id<Link>>();

		for(NetworkRoute nr:nRoutesAndLinkIds.keySet()){
			List<Id<Link>>linkIds = nRoutesAndLinkIds.get(nr);
			Iterator<Id<Link>> it = linkIds.iterator();
			do{
				if(it.next().equals(linkId)&&it.hasNext()){
					nextLinksInRoutes.add(it.next());
					break;
				}
			}while(it.hasNext());
		}

		if(nextLinksInRoutes.size()==0) throw new RuntimeException("There is no next link in the route of person "+personId+". Check!!!");
		else if(nextLinksInRoutes.size()==1) nextLinkInRoute = nextLinksInRoutes.get(0);
		else {
			for(int i=0; i < (activityEndTimes.size()-1);){
				if(activityEndTimes.get(i)<time && activityEndTimes.get(i+1)>0){
					nextLinkInRoute =  nextLinksInRoutes.get(i);
					break;
				} else {
					throw new RuntimeException("There are more than one links which come next to link"+linkId+" for perosn "+personId+
							". To check activity end times are used but condition is not satisfied. Aborting... ");
				}
			}
		}
		return nextLinkInRoute;
	}

	double getLastLeavingTime(Map<Id<Person>, Double> personId2LinkLeaveTime) {
		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id<Person> id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
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
	
	public List<CongestionEvent> getCongestionEventsAsList(){
		return this.congestionEventsList;
	}
}
