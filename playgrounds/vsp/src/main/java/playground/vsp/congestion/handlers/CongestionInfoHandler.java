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
import java.util.HashMap;
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
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.LinkCongestionInfo;

/**
 * This handler provides the basic information which is required to compute and internalize external congestion effects.
 * The basic functionality is to keep track of each link's queue, notice when a queue has dissolved, and so on.
 * 
 * @author ikaddoura
 *
 */
public final class CongestionInfoHandler implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler {

	private final static Logger log = Logger.getLogger(CongestionInfoHandler.class);

	private final Scenario scenario;
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();

	CongestionInfoHandler(Scenario scenario) {
		this.scenario = scenario;

		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			log.warn("Capacity period is other than 3600.");
		}
		
		if (this.scenario.getConfig().parallelEventHandling().getNumberOfThreads() != null) {
			throw new RuntimeException("Parallel event handling is not tested and may not work properly. Please set the number of threads to 'null'. Aborting...");
		}

		if (this.scenario.getConfig().transit().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

	}

	@Override
	public final void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
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
	public final void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				createLinkInfo(event.getLinkId());
			}

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else {
			// car!
			if (this.linkId2congestionInfo.get(event.getLinkId()) == null){
				// no one entered or left this link before
				createLinkInfo(event.getLinkId());
			}

			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());	
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		}	
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {		
		throw new RuntimeException("Not implemented. Please use a delegate instead. Aborting...");
	}
	
	public final void updateFlowQueue(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());

		if (linkInfo.getLeavingAgents().isEmpty() ) {
			// No agent is being tracked for that link.

		} else {

			double earliestLeaveTime = getLastLeavingTime(linkInfo.getPersonId2linkLeaveTime()) + linkInfo.getMarginalDelayPerLeavingVehicle_sec();

			if (event.getTime() > earliestLeaveTime + 1.){
				// Flow congestion has disappeared on that link.

				// Deleting the information of agents previously leaving that link.
				linkInfo.getLeavingAgents().clear();
				linkInfo.getPersonId2linkLeaveTime().clear();

				// yy looks to me link getLeavingAgents is not needed; getPersonId2linkLeaveTime().keySet() would return the
				// same result (and make the code faster).  kai, aug'15
			}
		}
	}
	
	private static double getLastLeavingTime(Map<Id<Person>, Double> personId2LinkLeaveTime) {

		double lastLeavingFromThatLink = Double.NEGATIVE_INFINITY;
		for (Id<Person> id : personId2LinkLeaveTime.keySet()){
			if (personId2LinkLeaveTime.get(id) > lastLeavingFromThatLink) {
				lastLeavingFromThatLink = personId2LinkLeaveTime.get(id);
			}
		}
		return lastLeavingFromThatLink;
	}
	
	public final void addAgentToFlowQueue(LinkLeaveEvent event) {
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		// Start tracking delays caused by that agent leaving the link.

		linkInfo.getPersonId2freeSpeedLeaveTime().remove(event.getVehicleId());

		linkInfo.getLeavingAgents().add(Id.createPersonId(event.getVehicleId()));
		linkInfo.getPersonId2linkLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());
		linkInfo.setLastLeavingAgent(Id.createPersonId(event.getVehicleId()));
	}

	public final void createLinkInfo(Id<Link> linkId) {
		LinkCongestionInfo linkInfo = new LinkCongestionInfo();	

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
	}
	
	final Map<Id<Link>, LinkCongestionInfo> getLinkId2congestionInfo() {
		return linkId2congestionInfo;
	}

	public List<Id<Vehicle>> getPtVehicleIDs() {
		return ptVehicleIDs;
	}
	
}
