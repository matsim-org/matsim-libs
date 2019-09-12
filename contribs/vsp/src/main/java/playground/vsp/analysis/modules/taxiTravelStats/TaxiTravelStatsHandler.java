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

package playground.vsp.analysis.modules.taxiTravelStats;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.vehicles.Vehicle;


/**
 * Counts number of vehicles, number of passengers, and capacity of transit vehicles per link. In addition provides Paxkm and capacitykm per link.
 * 
 * @authors aneumann
 *
 */

public class TaxiTravelStatsHandler implements ActivityEndEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private static final Logger log = Logger.getLogger(TaxiTravelStatsHandler.class);
	
	private static int STANDARD_TAXI_CAPACITY = 1;
	
	private final MutableScenario scenario;
	private final Double interval;
	
	private final Map<String, Integer> taxiId2CurrentPaxCountMap;
	
	private final Counts<Link> countsVehicles;
	private final Counts<Link> countsCapacity;
	private final Counts<Link> countsCapacity_m;
	private final Counts<Link> countsPax;
	private final Counts<Link> countsPax_m;
	
	private Integer maxSlice = 0;
	
	public TaxiTravelStatsHandler(Scenario scenario, Double interval) {
		this.scenario = (MutableScenario) scenario;
		this.interval = interval;

		this.taxiId2CurrentPaxCountMap = new HashMap<>();
		
		this.countsVehicles = new Counts<>();
		this.countsCapacity = new Counts<>();
		this.countsCapacity_m = new Counts<>();
		this.countsPax = new Counts<>();
		this.countsPax_m = new Counts<>();
		
		log.info("Using fixed taxi vehicle capacity of " + STANDARD_TAXI_CAPACITY);
	}
	
	@Override
	public void reset(int iteration) {

	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equalsIgnoreCase("BeforeVrpSchedule")) {
			this.taxiId2CurrentPaxCountMap.put(event.getPersonId().toString(), new Integer(0));
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// handle only taxi cabs
		if(this.taxiId2CurrentPaxCountMap.containsKey(event.getVehicleId().toString())){
			
			//create the counts if none exist
			Count<Link> countVehicles = this.countsVehicles.createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countCapacity = this.countsCapacity.createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countCapacity_m = this.countsCapacity_m.createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countPax = this.countsPax.createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			Count<Link> countPax_m = this.countsPax_m.createAndAddCount(event.getLinkId(), event.getLinkId().toString());
			
			if(countVehicles == null){
				//get existing counts
				countVehicles = this.countsVehicles.getCount(event.getLinkId());
				countCapacity = this.countsCapacity.getCount(event.getLinkId());
				countCapacity_m = this.countsCapacity_m.getCount(event.getLinkId());
				countPax = this.countsPax.getCount(event.getLinkId());
				countPax_m = this.countsPax_m.getCount(event.getLinkId());
			} else {
				//we always want to start with hour one
				countVehicles.createVolume(1, 0.);
				countCapacity.createVolume(1, 0.);
				countCapacity_m.createVolume(1, 0.);
				countPax.createVolume(1, 0.);
				countPax_m.createVolume(1, 0.);
			}

			this.increaseCount(countVehicles, event.getTime(), 1);
			
			double vehCapacity = getCapacityForVehicle(event.getVehicleId());
			this.increaseCount(countCapacity, event.getTime(), vehCapacity);
			
			double vehCapacity_m = vehCapacity * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countCapacity_m, event.getTime(), vehCapacity_m);
			
			double pax = this.taxiId2CurrentPaxCountMap.get(event.getVehicleId().toString());
			this.increaseCount(countPax, event.getTime(), pax);
			
			double pax_m = pax * this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			this.increaseCount(countPax_m, event.getTime(), pax_m);
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non taxi and every driver
		if(this.taxiId2CurrentPaxCountMap.keySet().contains(event.getVehicleId().toString())){
			// taxi
			if(!event.getVehicleId().toString().equalsIgnoreCase(event.getPersonId().toString())){
				// taxi cab but not the driver - increase by one
				this.taxiId2CurrentPaxCountMap.put(event.getVehicleId().toString(), this.taxiId2CurrentPaxCountMap.get(event.getVehicleId().toString()) + 1);
			}
		}	
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// subtract a passenger to the vehicle counts data, but ignore every non taxi and every driver
		if(this.taxiId2CurrentPaxCountMap.keySet().contains(event.getVehicleId().toString())){
			// taxi
			if(!event.getVehicleId().toString().equalsIgnoreCase(event.getPersonId().toString())){
				// taxi cab but not the driver - decrease by one
				this.taxiId2CurrentPaxCountMap.put(event.getVehicleId().toString(), this.taxiId2CurrentPaxCountMap.get(event.getVehicleId().toString()) - 1);
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
	
	private double getCapacityForVehicle(Id<Vehicle> vehicleId) {
		return STANDARD_TAXI_CAPACITY;
	}

	protected int getMaxTimeSlice() {
		return this.maxSlice;
	}
	
	protected Counts<Link> getCountsVehicles() {
		return this.countsVehicles;
	}

	protected Counts<Link> getCountsCapacity() {
		return this.countsCapacity;
	}

	protected Counts<Link> getCountsCapacity_m() {
		return this.countsCapacity_m;
	}

	protected Counts<Link> getCountsPax() {
		return this.countsPax;
	}

	protected Counts<Link> getCountsPax_m() {
		return this.countsPax_m;
	}
}
