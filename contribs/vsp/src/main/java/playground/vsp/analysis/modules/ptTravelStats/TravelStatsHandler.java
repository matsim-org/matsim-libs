/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.ptTravelStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;


/**
 * Counts number of vehicles, number of passengers, and capacity of transit vehicles per mode and link. In addition provides Paxkm and capacitykm per mode and link.
 * 
 * @authors aneumann, fuerbas, droeder
 *
 */

public class TravelStatsHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private static final Logger log = Logger.getLogger(TravelStatsHandler.class);
	
	private final MutableScenario scenario;
	private final Double interval;
	
	private final Map<Id<Vehicle>, String> transitVehicleId2transportModeMap;
	private final Set<Id<Person>> transitVehicleDriverIds;
	private final Map<Id<Vehicle>, Integer> transitVehicleId2CurrentPaxCountMap;
	
	private final HashMap<String, Counts<Link>> mode2CountsVehicles;
	private final HashMap<String, Counts<Link>> mode2CountsCapacity;
	private final HashMap<String, Counts<Link>> mode2CountsCapacity_m;
	private final HashMap<String, Counts<Link>> mode2CountsPax;
	private final HashMap<String, Counts<Link>> mode2CountsPax_m;
	
	private Integer maxSlice = 0;
	
	public TravelStatsHandler(Scenario scenario, Double interval) {
		this.scenario = (MutableScenario) scenario;
		this.interval = interval;

		this.transitVehicleId2transportModeMap = new HashMap<>();
		this.transitVehicleDriverIds = new TreeSet<>();
		this.transitVehicleId2CurrentPaxCountMap = new HashMap<>();
		
		this.mode2CountsVehicles = new HashMap<>();
		this.mode2CountsCapacity = new HashMap<>();
		this.mode2CountsCapacity_m = new HashMap<>();
		this.mode2CountsPax = new HashMap<>();
		this.mode2CountsPax_m = new HashMap<>();
	}
	
	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitVehicleDriverIds.add(event.getDriverId());
		this.transitVehicleId2CurrentPaxCountMap.put(event.getVehicleId(), new Integer(0));
		
		if(!this.scenario.getTransitSchedule().getTransitLines().containsKey(event.getTransitLineId())) {
			log.debug("The transit line " + event.getTransitLineId() + " does not exist in the transit schedule of the provided scenario.");
			return;
		}
		
		TransitLine line = this.scenario.getTransitSchedule().getTransitLines().get(event.getTransitLineId());
		if(line == null ){
			log.debug(event.getTransitLineId());
		}
		
		TransitRoute route = line.getRoutes().get(event.getTransitRouteId());
		if(route == null) {
			log.debug("The route " + event.getTransitRouteId() + " does not exist for transit line " + event.getTransitLineId()); 
			return;
		}
		
		String mode = route.getTransportMode();
		this.transitVehicleId2transportModeMap.put(event.getVehicleId(), mode);
		
		if(!this.mode2CountsVehicles.containsKey(mode)){
			this.mode2CountsVehicles.put(mode, new Counts<Link>());
		}		
		if(!this.mode2CountsCapacity.containsKey(mode)){
			this.mode2CountsCapacity.put(mode, new Counts<Link>());
		}
		if(!this.mode2CountsCapacity_m.containsKey(mode)){
			this.mode2CountsCapacity_m.put(mode, new Counts<Link>());
		}
		if(!this.mode2CountsPax.containsKey(mode)){
			this.mode2CountsPax.put(mode, new Counts<Link>());
		}
		if(!this.mode2CountsPax_m.containsKey(mode)){
			this.mode2CountsPax_m.put(mode, new Counts<Link>());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// handle only pt-Vehicles!
		if(this.transitVehicleId2CurrentPaxCountMap.containsKey(event.getVehicleId())){
			
			String mode = this.transitVehicleId2transportModeMap.get(event.getVehicleId());
			
			//create the counts if none exist
			Count<Link> countVehicles = this.mode2CountsVehicles.get(mode).createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countCapacity = this.mode2CountsCapacity.get(mode).createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countCapacity_m = this.mode2CountsCapacity_m.get(mode).createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countPax = this.mode2CountsPax.get(mode).createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countPax_m = this.mode2CountsPax_m.get(mode).createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			
			if(countVehicles == null){
				//get existing counts
				countVehicles = this.mode2CountsVehicles.get(mode).getCount(event.getLinkId());
				countCapacity = this.mode2CountsCapacity.get(mode).getCount(event.getLinkId());
				countCapacity_m = this.mode2CountsCapacity_m.get(mode).getCount(event.getLinkId());
				countPax = this.mode2CountsPax.get(mode).getCount(event.getLinkId());
				countPax_m = this.mode2CountsPax_m.get(mode).getCount(event.getLinkId());
			} else {
				//we always want to start with hour one
				countVehicles.createVolume(1, 0.);
				countCapacity.createVolume(1, 0.);
				countCapacity_m.createVolume(1, 0.);
				countPax.createVolume(1, 0.);
				countPax_m.createVolume(1, 0.);
			}

			this.increaseCount(countVehicles, event.getTime(), 1);
			
			double vehCapacity = getCapacityForPtVehicle(event.getVehicleId());
			this.increaseCount(countCapacity, event.getTime(), vehCapacity);
			
			double vehCapacity_m = vehCapacity * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countCapacity_m, event.getTime(), vehCapacity_m);
			
			double pax = this.transitVehicleId2CurrentPaxCountMap.get(event.getVehicleId());
			this.increaseCount(countPax, event.getTime(), pax);
			
			double pax_m = pax * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countPax_m, event.getTime(), pax_m);
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non pt-vehicle and every driver
		if(this.transitVehicleId2CurrentPaxCountMap.keySet().contains(event.getVehicleId())){
			if(!this.transitVehicleDriverIds.contains(event.getPersonId())){
				// transit vehicle, but not the driver - increase by one
				this.transitVehicleId2CurrentPaxCountMap.put(event.getVehicleId(), this.transitVehicleId2CurrentPaxCountMap.get(event.getVehicleId()) + 1);
			}
		}	
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// subtract a passenger to the vehicle counts data, but ignore every non pt-vehicle and every driver
		if(this.transitVehicleId2CurrentPaxCountMap.keySet().contains(event.getVehicleId())){
			if(!this.transitVehicleDriverIds.contains(event.getPersonId())){
				// transit vehicle, but not the driver - decrease by one
				this.transitVehicleId2CurrentPaxCountMap.put(event.getVehicleId(), this.transitVehicleId2CurrentPaxCountMap.get(event.getVehicleId()) - 1);
			}
		}	
	}

	private void increaseCount(Count<Link> count, double time, double amount) {
		Integer slice = (int) (time / this.interval) + 1;
		if(slice > this.maxSlice){
			this.maxSlice = slice;
		}
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + amount);
	}
	
	private double getCapacityForPtVehicle(Id<Vehicle> vehicleId) {
		int vehSeats = this.scenario.getTransitVehicles().getVehicles().get(vehicleId).getType().getCapacity().getSeats();
		int vehStand = this.scenario.getTransitVehicles().getVehicles().get(vehicleId).getType().getCapacity().getStandingRoom();
		return vehSeats + vehStand;
	}

	protected int getMaxTimeSlice() {
		return this.maxSlice;
	}
	
	protected HashMap<String, Counts<Link>> getMode2CountsVehicles() {
		return this.mode2CountsVehicles;
	}

	protected HashMap<String, Counts<Link>> getMode2CountsCapacity() {
		return this.mode2CountsCapacity;
	}

	protected HashMap<String, Counts<Link>> getMode2CountsCapacity_m() {
		return this.mode2CountsCapacity_m;
	}

	protected HashMap<String, Counts<Link>> getMode2CountsPax() {
		return this.mode2CountsPax;
	}

	protected HashMap<String, Counts<Link>> getMode2CountsPax_m() {
		return this.mode2CountsPax_m;
	}
}
