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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
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

public class CongestionHandlerImplV4  implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
PersonDepartureEventHandler,
PersonArrivalEventHandler,
PersonStuckEventHandler,
ActivityEndEventHandler
{

	final static Logger log = Logger.getLogger(CongestionHandlerImplV4.class);
	final Scenario scenario;
	final EventsManager events;
	final List<Id<Vehicle>> nonCarVehicleIDs = new ArrayList<Id<Vehicle>>();
	final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<>();
	private Map<Id<Person>, List<Tuple<String, Double>>> personId2ActType2ActEntTime = new HashMap<>();
	private int roundingErrorWarnCount =0;

	double totalInternalizedDelay = 0.0;
	double totalDelay = 0.0;
	double totalStorageDelay = 0.0;
	double delayNotInternalized_roundingErrors = 0.0;

	public CongestionHandlerImplV4(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

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
		this.nonCarVehicleIDs.clear();
		this.totalDelay = 0.0;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.linkId2congestionInfo.clear();
		this.personId2ActType2ActEntTime.clear();

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
	public void handleEvent(ActivityEndEvent event) {
		//require for the multiple next link in route of the same person
		if(personId2ActType2ActEntTime.containsKey(event.getPersonId())){
			List<Tuple<String, Double>> listSoFar = personId2ActType2ActEntTime.get(event.getPersonId());
			listSoFar.add(new Tuple<String, Double>(event.getActType(), event.getTime()));
		} else {
			List<Tuple<String, Double>> listNow = new ArrayList<Tuple<String,Double>>();
			listNow.add(new Tuple<String, Double>(event.getActType(), event.getTime()));
			personId2ActType2ActEntTime.put(event.getPersonId(), listNow);
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
		if (this.nonCarVehicleIDs.contains(event.getVehicleId())){
			log.warn("Modes other than car is not implemented yet.");
		} else { // car
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getEnteringAgents().add(enteredPerson);
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		}	
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!this.nonCarVehicleIDs.contains(event.getVehicleId())){
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			if (linkInfo.getLeavingAgents().size() != 0) {
				// Clear tracking of persons leaving that link previously.

				double lastLeavingFromCurrentLink = linkInfo.getLastLeaveTime();
				double earliestLeaveTime = lastLeavingFromCurrentLink + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

				if (event.getTime() > Math.floor(earliestLeaveTime)+1 ){// Flow congestion has disappeared on that link.
					// Deleting the information of agents previously leaving that link.
					linkInfo.getLeavingAgents().clear();
				}
			}
			startProcessingDelay(event);

			linkInfo.getEnteringAgents().remove(event.getVehicleId());
			linkInfo.getLeavingAgents().add(Id.createPersonId(event.getVehicleId()));
			linkInfo.setLastLeavingAgent(Id.createPersonId(event.getVehicleId()));
			linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());
			linkInfo.setLastLeaveTime(event.getTime());
		}
	}

	/**
	 * @param event
	 * This method first charge for flow storage delays  
	 * and then if spill back delays are present, process them.
	 */
	private void startProcessingDelay(LinkLeaveEvent event) {

		Id<Person> delayedPerson = Id.createPersonId(event.getVehicleId().toString());

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(delayedPerson);

		if (delayOnThisLink==0) return; 
		else if(delayOnThisLink<0) throw new RuntimeException("Delays can not be negative, do a consistency check. Aborting ...");
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
				// remove previous occurance of this spillback causing link if any.
				if(linkInfo.getSpillBackCausingLinks().contains(spillBackCausingLink)) linkInfo.getSpillBackCausingLinks().remove(spillBackCausingLink);	
				linkInfo.getSpillBackCausingLinks().add(spillBackCausingLink);  
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
				identifyAndProcessSpillBackCausingLink(event, linkInfo);
			}
		}

		double delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));
		if(delayToPayFor > 0) {
			this.delayNotInternalized_roundingErrors += delayToPayFor;
			this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().put(delayedPerson, 0.0);
			log.warn("Delay to pay for is "+delayToPayFor+". Including them in non Internalizing delays. This happened during event "+event.toString());
		}
	}

	private void identifyAndProcessSpillBackCausingLink(LinkLeaveEvent event, LinkCongestionInfo linkCongestionInfo){

		List<Id<Link>> spillBackCausingLinks = linkCongestionInfo.getSpillBackCausingLinks();

		Collections.reverse(spillBackCausingLinks);

		if(spillBackCausingLinks.isEmpty()) return;

		Iterator<Id<Link>> spillBackLinkIterator = spillBackCausingLinks.iterator();

		double delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));

		while(spillBackLinkIterator.hasNext() && delayToPayFor > 0) {

			Id<Link> spillBackCausingLink = spillBackLinkIterator.next();
			processSpillBackDelays(event, spillBackCausingLink);

			delayToPayFor = this.linkId2congestionInfo.get(event.getLinkId()).getPersonId2DelaysToPayFor().get(Id.createPersonId(event.getVehicleId()));

			if(delayToPayFor == 0) ;
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

		while(enteredPersonsListIterator.hasNext() && delayToPayFor > 0){
			Id<Person> personToBeCharged = enteredPersonsListIterator.next();

			chargingPersonAndThrowingEvents(event, personToBeCharged,spillBackCausingLink);

			delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
		}

		List<Id<Person>> personsLeftSpillBackCausingLink = new ArrayList<Id<Person>>();
		if(delayToPayFor>0){
			personsLeftSpillBackCausingLink.addAll(spillBackCausingLinkInfo.getLeavingAgents());
			Collections.reverse(personsLeftSpillBackCausingLink);
			Iterator<Id<Person>> prsnLftSpillBakCauinLinkItrtr = personsLeftSpillBackCausingLink.iterator();

			while(prsnLftSpillBakCauinLinkItrtr.hasNext() && delayToPayFor > 0.){ // again charged for flow cap of link
				Id<Person> chargedPersonId = prsnLftSpillBakCauinLinkItrtr.next();

				chargingPersonAndThrowingEvents(event, chargedPersonId, spillBackCausingLink);

				delayToPayFor = this.linkId2congestionInfo.get(personDelayedOnLink).getPersonId2DelaysToPayFor().get(delayedPerson);
			}
		}
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
			log.error("Causing agent and affected agents "+causingPerson.toString()+" are same. Delays at this point is "+personChargedFor+" sec.");
			return;
			//throw new RuntimeException("The causing agent and the affected agent are the same (" + personToBeCharged.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
		} else {
			// using the time when the causing agent entered the link
			this.totalInternalizedDelay = this.totalInternalizedDelay + personChargedFor;
			CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), "flowStorageCapacity", causingPerson, delayedPerson, personChargedFor, causingPersonOnLink,
					this.linkId2congestionInfo.get(causingPersonOnLink).getPersonId2linkEnterTime().get(causingPerson) );
			this.events.processEvent(congestionEvent);
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

		List<Tuple<String, Double>> personActInfos = personId2ActType2ActEntTime.get(personId);
		int numberOfActEnded = personActInfos.size();

		String currentAct = personId2ActType2ActEntTime.get(personId).get(numberOfActEnded-1).getFirst();
		int noOfOccuranceOfCurrentAct = 0;

		SortedSet<Double> actEndTimes = new TreeSet<Double>();

		for(int i =0;i<numberOfActEnded;i++){ // last stored act is currentAct
			Tuple<String, Double> actInfo = personId2ActType2ActEntTime.get(personId).get(i);
			if(currentAct.equals(actInfo.getFirst())) {
				actEndTimes.add(actInfo.getSecond());
				noOfOccuranceOfCurrentAct++;
			}
		}

		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();

		int actIndex = 0;

		for(PlanElement pe :planElements){
			if(pe instanceof Activity && actIndex < noOfOccuranceOfCurrentAct){
				if(((Activity)pe).getType().equals(currentAct)) actIndex ++;	
			}

			if(pe instanceof Leg && actIndex == noOfOccuranceOfCurrentAct){
				//				The following is necessary where a person makes several trips with different modes and thus non car trips are not instance of NetworkRoute.
				if(!((Leg)pe).getMode().equals(TransportMode.car)) continue; 

				NetworkRoute nRoute = ((NetworkRoute)((Leg)pe).getRoute()); 
				routeLinks.add(nRoute.getStartLinkId());
				routeLinks.addAll(nRoute.getLinkIds());  
				routeLinks.add(nRoute.getEndLinkId());
				break;
			}
		}

		Id<Link> nextLinkInRoute =  Id.create("NA",Link.class);
		Iterator<Id<Link>> it = routeLinks.iterator();
		do{
			if(it.next().equals(linkId) && it.hasNext()){
				nextLinkInRoute = it.next();
				break;
			}
		} while(it.hasNext());

		if (nextLinkInRoute.equals(Id.create("NA",Link.class))){ 
			throw new RuntimeException("Next link in the route of person "+personId+" is not found. At time "+time+" person is on the link "+linkId+". Aborting ...");
		} else return nextLinkInRoute;

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
