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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * This handler calculates delays (caused by the flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. The marginal congestion event is thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 
 * @author ikaddoura
 *
 */
abstract class AbstractCongestionHandler implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
Wait2LinkEventHandler {

	private final static Logger log = Logger.getLogger(AbstractCongestionHandler.class);

	// If the following parameter is false, a Runtime Exception is thrown in case an agent is delayed by the storage capacity.
	private final boolean allowingForStorageCapacityConstraint = true;

	// If the following parameter is false, the delays resulting from the storage capacity are not internalized.
	private final boolean calculatingStorageCapacityConstraints = true;
	private double delayNotInternalized_storageCapacity = 0.0;

	private final Scenario scenario;
	private final EventsManager events;
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();

	private double totalInternalizedDelay = 0.0;
	private double totalDelay = 0.0;
	private double delayNotInternalized_roundingErrors = 0.0;
	private double delayNotInternalized_spillbackNoCausingAgent = 0.0;

	Map<Id<Person>,Integer> personId2legNr = new HashMap<>() ;
	Map<Id<Person>,Integer> personId2linkNr = new HashMap<>() ;

	Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>() ;

	AbstractCongestionHandler(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			log.warn("Capacity period is other than 3600.");
		}

		// TODO: Runtime exception if parallel events handling.

		if (this.scenario.getConfig().qsim().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().transit().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

	}

	@Override
	public final void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
		this.delayNotInternalized_storageCapacity = 0.0;
		this.totalDelay = 0.0 ;
		this.totalInternalizedDelay = 0.0;
		this.delayNotInternalized_roundingErrors = 0.0;
		this.delayNotInternalized_spillbackNoCausingAgent = 0.0;
		
		//--
		this.personId2legNr.clear();
		this.personId2linkNr.clear();
	}

	@Override
	public final void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}

	@Override
	public final void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects: " + event.toString());
	}

	@Override
	public final void handleEvent( Wait2LinkEvent event ) {
		this.vehicleId2personId.put( event.getVehicleId(), event.getPersonId() ) ;
	}
	
	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){ // car!
			LinkCongestionInfo linkInfo = getOrCreateLinkInfo( event.getLinkId() ) ;
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
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
	public final void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		} else { // car! 
			LinkCongestionInfo linkInfo = getOrCreateLinkInfo( event.getLinkId() ) ;
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		}
		// ---
		int linkNr = this.personId2linkNr.get( event.getPersonId() ) ;
		this.personId2linkNr.put( event.getPersonId(), linkNr + 1 ) ;
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		} else { // car!
			LinkCongestionInfo linkInfo = getOrCreateLinkInfo(event.getLinkId());
			
			Id<Person> personId = this.vehicleId2personId.get( event.getVehicleId() ) ;
			updateFlowAndDelayQueues(event.getTime(), personId, linkInfo );
			calculateCongestion(event);
			addAgentToFlowAndDelayQueues(event, event.getTime(), personId, linkInfo );
			
			linkInfo.memorizeLastLinkLeaveEvent( event );
		}
		
	}


	// ############################################################################################################################################################

	private final static void updateFlowAndDelayQueues(double time, Id<Person> personId, LinkCongestionInfo linkInfo) {
		if ( linkInfo.getDelayQueue().isEmpty() ) {
			// queue is already empty; nothing to do
		} else {
			double delay = time - linkInfo.getPersonId2freeSpeedLeaveTime().get( personId ) - 1 ;
			if ( delay < 0 ) {
				linkInfo.getDelayQueue().clear() ;
			}
		}

		if (linkInfo.getFlowQueue().isEmpty() ) {
			// queue is already empty; nothing to do
		} else {
			double earliestLeaveTime = getLastLeavingTime(linkInfo) + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			if ( time > earliestLeaveTime + 1.){
				// bottleneck no longer active; remove data:
				linkInfo.getFlowQueue().clear();
			}
		}
	}

	final double computeFlowCongestionAndReturnStorageDelay(double now, Id<Link> linkId, Id<Vehicle> affectedVehId, double agentDelay) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get( linkId);
		
		for ( Iterator<Id<Person>> it = linkInfo.getFlowQueue().descendingIterator() ; it.hasNext() ; ) {
			Id<Person> causingPersonId = it.next() ;
			double delayAllocatedToThisCausingPerson = Math.min( linkInfo.getMarginalDelayPerLeavingVehicle_sec(), agentDelay ) ;
			// (marginalDelay... is based on flow capacity of link, not time headway of vehicle)

			if(delayAllocatedToThisCausingPerson==0.) {
				return 0.; // no reason to throw a congestion event for zero delay. (AA sep'15)
			}
			
			if (affectedVehId.toString().equals(causingPersonId.toString())) {
				//					log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
			} else {
				// using the time when the causing agent entered the link
				CongestionEvent congestionEvent = new CongestionEvent(now, "flowStorageCapacity", causingPersonId, 
						Id.createPersonId(affectedVehId), delayAllocatedToThisCausingPerson, linkId, linkInfo.getPersonId2linkEnterTime().get(causingPersonId));
				this.events.processEvent(congestionEvent);
				this.totalInternalizedDelay += delayAllocatedToThisCausingPerson ;
			}
			agentDelay = agentDelay - delayAllocatedToThisCausingPerson ; 
		}

		if (agentDelay <= 1.) {
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += agentDelay;
			agentDelay = 0.;
		}

		return agentDelay;
	}

	private final static void addAgentToFlowAndDelayQueues(LinkLeaveEvent event, double now, Id<Person> personId, LinkCongestionInfo linkInfo) {

		linkInfo.getFlowQueue().add( personId );
		linkInfo.getDelayQueue().add( personId ) ;
		
		linkInfo.getPersonId2linkLeaveTime().put( personId, now );
	}

	private final LinkCongestionInfo getOrCreateLinkInfo(Id<Link> linkId) {
		
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get( linkId ) ;
		if (linkInfo != null){ 
			return linkInfo ;
		}
		linkInfo = new LinkCongestionInfo();	

		Network network = this.scenario.getNetwork();
		Link link = network.getLinks().get(linkId);
		linkInfo.setLinkId(link.getId());
		linkInfo.setFreeTravelTime(Math.floor(link.getLength() / link.getFreespeed()));

		double flowCapacity_capPeriod = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
		double marginalDelay_sec = ((1 / (flowCapacity_capPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
		linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);

		double storageCapacity_cars = (int) (Math.ceil((link.getLength() * link.getNumberOfLanes()) 
				/ ((NetworkImpl)network).getEffectiveCellSize()) * this.scenario.getConfig().qsim().getStorageCapFactor() );
		linkInfo.setStorageCapacityCars(storageCapacity_cars);

		this.linkId2congestionInfo.put(link.getId(), linkInfo);
		
		return linkInfo ;
	}

	private static double getLastLeavingTime(LinkCongestionInfo linkInfo ) {
		Map<Id<Person>, Double> personId2LinkLeaveTime = linkInfo.getPersonId2linkLeaveTime() ;

		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id<Person> id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
	}

	public final void writeCongestionStats(String fileName) {
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Total delay [hours];" + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.totalInternalizedDelay / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.delayNotInternalized_roundingErrors / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (spill-back related delays without identifiying the causing agent) [hours];" + this.delayNotInternalized_spillbackNoCausingAgent / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (in case delays resulting from the storage capacity are ignored) [hours];" + this.delayNotInternalized_storageCapacity / 3600.);
			bw.newLine();

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Congestion statistics written to " + fileName);		
	}

	public final double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

	public final double getTotalDelay() {
		return totalDelay;
	}

	public final double getDelayNotInternalizedRoundingErrors() {
		return delayNotInternalized_roundingErrors;
	}

	public final double getDelayNotInternalizedSpillbackNoCausingAgent() {
		return delayNotInternalized_spillbackNoCausingAgent;
	}

	abstract void calculateCongestion(LinkLeaveEvent event);

	final boolean isAllowingForStorageCapacityConstraint() {
		return allowingForStorageCapacityConstraint;
	}

	final boolean isCalculatingStorageCapacityConstraints() {
		return calculatingStorageCapacityConstraints;
	}

	final void addToTotalDelay(double val) {
		this.totalDelay += val;
	}

	final Map<Id<Link>, LinkCongestionInfo> getLinkId2congestionInfo() {
		return linkId2congestionInfo;
	}

	final void addToDelayNotInternalized_storageCapacity( double val ) {
		this.delayNotInternalized_storageCapacity += val ;
	}
	final double getDelayNotInternalized_storageCapacity() {
		return this.delayNotInternalized_storageCapacity ;
	}
	final void addToDelayNotInternalized_spillbackNoCausingAgent(double val) {
		this.delayNotInternalized_spillbackNoCausingAgent += val ;
	}
	final void addToTotalInternalizedDelay(double val){
		this.totalInternalizedDelay += val;
	}

	public void addToDelayNotInternalized_roundingErrors(double val) {
		this.delayNotInternalized_roundingErrors += val;
	}

	final EventsManager getEventsManager(){
		return this.events;
	}
}
