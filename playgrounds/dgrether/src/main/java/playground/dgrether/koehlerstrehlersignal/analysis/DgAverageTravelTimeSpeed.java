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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class DgAverageTravelTimeSpeed implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, 
		VehicleAbortsEventHandler, VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler, PersonLeavesVehicleEventHandler{
	
	private static final Logger LOG = Logger.getLogger(DgAverageTravelTimeSpeed.class);
	
	private Network network;
	private Map<Id<Vehicle>, Double> vehEnterTime;
	private Set<Id<Person>> seenPersonIds;
	// TODO only for debugging
	private Set<Id<Vehicle>> seenVehicleIds;
	private double sumTravelTime;
	private double sumDistance;
	private double numberOfTrips;
	private Map<Id<Vehicle>, Set<Id<Person>>> personsInsideVeh;
	private boolean considerStuckAbortTravelTime = false;
	private Set<Id<Vehicle>> stuckedVeh;

	public DgAverageTravelTimeSpeed(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.vehEnterTime = new HashMap<>();
		this.seenPersonIds = new HashSet<>();
		this.seenVehicleIds = new HashSet<>();
		this.sumTravelTime = 0.0;
		this.sumDistance = 0.0;
		this.numberOfTrips = 0.0;
		this.personsInsideVeh = new HashMap<>();
		this.stuckedVeh = new HashSet<>();
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!personsInsideVeh.containsKey(event.getVehicleId())){
			personsInsideVeh.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		personsInsideVeh.get(event.getVehicleId()).add(event.getPersonId());		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		personsInsideVeh.get(event.getVehicleId()).remove(event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		handleVehicleSeen(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		handleVehicleSeen(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	private void handleVehicleSeen(Id<Vehicle> vehId, Id<Link> linkId, double time) {
		if (network.getLinks().containsKey(linkId)) {
			this.vehEnterTime.put(vehId, time);
			this.seenPersonIds.addAll(personsInsideVeh.get(vehId));
			this.seenVehicleIds.add(vehId);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		handleVehicleLeft(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		handleVehicleLeft(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	private void handleVehicleLeft(Id<Vehicle> vehId, Id<Link> linkId, double time) {
		if (network.getLinks().containsKey(linkId)) {
			Double vehEnterTime = this.vehEnterTime.remove(vehId);
			if (vehEnterTime != null) {
				this.sumTravelTime += time - vehEnterTime;
				this.sumDistance += network.getLinks().get(linkId).getLength();
			}
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		// TODO for debugging
//		if (event.getTime() == 86400.){
//			LOG.error("vehicle " + event.getVehicleId() + " aborts at sim end time during its first trip on link " + event.getLinkId());
//		}
		stuckedVeh.add(event.getVehicleId());
		if (this.considerStuckAbortTravelTime ){
			LOG.warn("Travel time and speed of vehicle " + event.getVehicleId() + " is considered although it aborts on link " + event.getLinkId());
			handleVehicleLeft(event.getVehicleId(), event.getLinkId(), event.getTime());
		} else {
			this.vehEnterTime.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// counts each person every time it arrives as one trip
		if (this.seenPersonIds.contains(event.getPersonId())){
			this.numberOfTrips++;
		}
	}

	public double getTravelTime() {
		return sumTravelTime;
	}
	
	// TODO test whether getNumberOfVehicles, getNumberOfPersons and getNumberOfTrips is the same with the FirstTripPerPersonFilter
	public double getNumberOfVehicles(){
		return this.seenVehicleIds.size();
	}
	
	public double getNumberOfPersons(){
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

	public void considerStuckAndAbortTravelTimesAndSpeeds(){
		this.considerStuckAbortTravelTime = true;
	}
	
	public int getNumberOfStuckedVeh(){
		return this.stuckedVeh.size();
	}
	
	public Set<Id<Vehicle>> getStuckedVeh(){
		return this.stuckedVeh;
	}
}
