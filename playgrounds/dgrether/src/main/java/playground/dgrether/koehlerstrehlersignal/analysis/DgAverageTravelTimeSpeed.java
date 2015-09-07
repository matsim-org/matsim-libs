/* *********************************************************************** *
 * project: org.matsim.*
 * DgAverageTravelTimeSpeed
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.Link2WaitEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class DgAverageTravelTimeSpeed implements LinkEnterEventHandler, LinkLeaveEventHandler, Link2WaitEventHandler, 
		VehicleAbortEventHandler, Wait2LinkEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler{

	private static final Logger log = Logger.getLogger(DgAverageTravelTimeSpeed.class);
	
	private Network network;
	private Map<Id<Vehicle>, Double> networkEnterTimesByVehicleId;
	private Set<Id<Person>> seenPersonIds;
	private double sumTravelTime;
	private double sumDistance;
	private double numberOfTrips;
	private Map<Id<Vehicle>, Set<Id<Person>>> vehId2SetOfPersonIdsMap;

	public DgAverageTravelTimeSpeed(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.networkEnterTimesByVehicleId = new HashMap<>();
		this.seenPersonIds = new HashSet<>();
		this.sumTravelTime = 0.0;
		this.sumDistance = 0.0;
		this.numberOfTrips = 0.0;
		this.vehId2SetOfPersonIdsMap = new HashMap<>();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!vehId2SetOfPersonIdsMap.containsKey(event.getPersonId())){
			vehId2SetOfPersonIdsMap.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		vehId2SetOfPersonIdsMap.get(event.getVehicleId()).add(event.getPersonId());		
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			handleVehicleSeen(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			handleVehicleSeen(event.getVehicleId(), event.getTime());
		}
	}

	private void handleVehicleSeen(Id<Vehicle> vehId, double time) {
		this.networkEnterTimesByVehicleId.put(vehId, time);
		this.seenPersonIds.addAll(vehId2SetOfPersonIdsMap.get(vehId));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			Double linkEnterTime = this.networkEnterTimesByVehicleId.remove(event.getVehicleId());
			if (linkEnterTime != null) {
				this.sumTravelTime += event.getTime() - linkEnterTime;
				this.sumDistance += network.getLinks().get(event.getLinkId()).getLength();
			}
		}
		else{
			log.error("Link wasn't found.");
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (network.getLinks().containsKey(event.getLinkId())) {
			Double vehEnterTime = this.networkEnterTimesByVehicleId.remove(event.getVehicleId());
			if (vehEnterTime != null) {
				this.sumTravelTime += event.getTime() - vehEnterTime;
				this.sumDistance += network.getLinks().get(event.getLinkId()).getLength();
			}
		}		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.seenPersonIds.contains(event.getPersonId())){
			this.numberOfTrips++;
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.networkEnterTimesByVehicleId.remove(event.getVehicleId());
	}

	
	public double getTravelTime() {
		return sumTravelTime;
	}
	
	public double getNumberOfVehicles(){
		return this.seenPersonIds.size();
	}
	
	public Set<Id<Person>> getSeenPersonIds(){
		return this.seenPersonIds;
	}
	
	public double getDistanceMeter(){
		return this.sumDistance;
	}
	
	public double getNumberOfTrips(){
		return this.numberOfTrips;
	}


}
