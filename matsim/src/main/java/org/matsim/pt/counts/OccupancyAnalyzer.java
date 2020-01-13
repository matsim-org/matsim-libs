/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTrRouteStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.pt.counts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author yChen
 * @author mrieser / senozon
 */
public class OccupancyAnalyzer implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler {

	private static final Logger log = Logger.getLogger(OccupancyAnalyzer.class);

	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	/** Map< stopFacilityId,value[]> */
	private Map<Id<TransitStopFacility>, int[]> boards, alights, occupancies;

	/** Map< vehId,stopFacilityId> */
	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehStops = new HashMap<>();
	/** Map<vehId,passengersNo. in Veh> */
	private final Map<Id<Vehicle>, Integer> vehPassengers = new HashMap<>();
	private StringBuffer occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();

	public OccupancyAnalyzer(final int timeBinSize, final double maxTime) {
		log.setLevel( Level.INFO ) ;

		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.boards = new HashMap<>();
		this.alights = new HashMap<>();
		this.occupancies = new HashMap<>();
	}

	public void setBoards(Map<Id<TransitStopFacility>, int[]> boards) {
		this.boards = boards;
	}

	public void setAlights(Map<Id<TransitStopFacility>, int[]> alights) {
		this.alights = alights;
	}

	public void setOccupancies(Map<Id<TransitStopFacility>, int[]> occupancies) {
		this.occupancies = occupancies;
	}

	public int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	@Override
	public void reset(int iteration) {
		this.boards.clear();
		this.alights.clear();
		this.occupancies.clear();
		this.vehStops.clear();
		this.vehPassengers.clear();
		this.occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
		this.transitDrivers.clear();
		this.transitVehicles.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		
		Id<Vehicle> vehId = event.getVehicleId();
		Id<TransitStopFacility> stopId = this.vehStops.get(vehId);
		double time = event.getTime();
		// --------------------------getOns---------------------------
		int[] getOn = this.boards.get(stopId);
		if (getOn == null) {
			getOn = new int[this.maxSlotIndex + 1];
			this.boards.put(stopId, getOn);
		}
		getOn[getTimeSlotIndex(time)]++;
		// ------------------------veh_passenger---------------------------
		Integer nPassengers = this.vehPassengers.get(vehId);
		this.vehPassengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
		this.occupancyRecord.append("time :\t").append(time).append(" veh :\t").append(vehId).append(" has Passenger\t").append(this.vehPassengers.get(vehId)).append(" \tat stop :\t").append(stopId).append(" ENTERING PERSON :\t").append(event.getPersonId()).append("\n");
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		
		Id<Vehicle> vehId = event.getVehicleId();
		Id<TransitStopFacility> stopId = this.vehStops.get(vehId);
		double time = event.getTime();
		// --------------------------getDowns---------------------------
		int[] getDown = this.alights.get(stopId);
		if (getDown == null) {
			getDown = new int[this.maxSlotIndex + 1];
			this.alights.put(stopId, getDown);
		}
		getDown[getTimeSlotIndex(time)]++;
		// ------------------------veh_passenger---------------------------
		Integer nPassengers = this.vehPassengers.get(vehId);
		if (nPassengers == null) {
			log.error( "tests for `null' but exception says 'negative'???  kai, oct'10 ") ;
			throw new RuntimeException("negative passenger-No. in vehicle?");
		}
		this.vehPassengers.put(vehId, nPassengers - 1);
		if (this.vehPassengers.get(vehId).intValue() == 0) {
			this.vehPassengers.remove(vehId);
		}

		Integer passengers = this.vehPassengers.get(vehId);
		this.occupancyRecord.append("time :\t").append(time).append(" veh :\t").append(vehId).append(" has Passenger\t").append((passengers != null) ? passengers : 0).append("\n");
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id<TransitStopFacility> stopId = event.getFacilityId();
		this.vehStops.put(event.getVehicleId(), stopId);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id<TransitStopFacility> stopId = event.getFacilityId();
		Id<Vehicle> vehId = event.getVehicleId();
		this.vehStops.remove(vehId);
		// -----------------------occupancy--------------------------------
		int[] occupancyAtStop = this.occupancies.get(stopId);

		if (occupancyAtStop == null) {
			// no previous departure from this stop, therefore no occupancy record yet.  Create this:
			occupancyAtStop = new int[this.maxSlotIndex + 1];
			this.occupancies.put(stopId, occupancyAtStop);
		}

		Integer noPassengersInVeh = this.vehPassengers.get(vehId);

		if (noPassengersInVeh != null) {
			occupancyAtStop[this.getTimeSlotIndex(event.getTime())] += noPassengersInVeh;

			this.occupancyRecord.append(event.getTime());
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(vehId);
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(stopId);
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(noPassengersInVeh);
			this.occupancyRecord.append("\n");
		}
	}

	/**
	 * @param stopId
	 * @return Array containing the number of agents boarding at the stop
	 *         <code>stopId</code> per time bin, starting with time bin 0 from 0
	 *         seconds to (timeBinSize-1)seconds.
	 */
	public int[] getBoardVolumesForStop(final Id<TransitStopFacility> stopId) {
		int[] values = this.boards.get(stopId);
		if (values == null) {
			return new int[this.maxSlotIndex + 1];
		}
		return values;
	}

	/**
	 * @param stopId
	 * @return Array containing the number of agents alighting at the stop
	 *         {@code stopId} per time bin, starting with time bin 0 from 0
	 *         seconds to (timeBinSize-1)seconds.
	 */
	public int[] getAlightVolumesForStop(final Id<TransitStopFacility> stopId) {
		int[] values = this.alights.get(stopId);
		if (values == null) {
			return new int[this.maxSlotIndex + 1];
		}
		return values;
	}

	/**
	 * @param stopId
	 * @return Array containing the number of passengers in bus after the
	 *         transfer at the stop {@code stopId} per time bin, starting with
	 *         time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getOccupancyVolumesForStop(final Id<TransitStopFacility> stopId) {
		int[] values = this.occupancies.get(stopId);
		if (values == null) {
			return new int[this.maxSlotIndex + 1];
		}
		return values;
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents
	 *         boarded, for which counting-values are available.
	 */
	public Set<Id<TransitStopFacility>> getBoardStopIds() {
		return this.boards.keySet();
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents
	 *         alit, for which counting-values are available.
	 */
	public Set<Id<TransitStopFacility>> getAlightStopIds() {
		return this.alights.keySet();
	}

	public Set<Id<TransitStopFacility>> getOccupancyStopIds() {
		return this.occupancies.keySet();
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents alit
	 *         or/and boarded, for which counting-values are available.
	 */
	public Set<Id<TransitStopFacility>> getAllStopIds() {
		Set<Id<TransitStopFacility>> allStopIds = new TreeSet<>();
		allStopIds.addAll(getBoardStopIds());
		allStopIds.addAll(getAlightStopIds());
		allStopIds.addAll(getOccupancyStopIds());
		return allStopIds;
	}

	public void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		// write filehead
		writer.write("stopId\t");
		for (int i = 0; i < 24; i++) {
			writer.write("bo" + i + "-" + (i + 1) + "\t");
		}
		for (int i = 0; i < 24; i++) {
			writer.write("al" + i + "-" + (i + 1) + "\t");
		}
		for (int i = 0; i < 24; i++) {
			writer.write("oc" + i + "-" + (i + 1) + "\t");
		}
		writer.writeln();
		// write content
		for (Id<TransitStopFacility> stopId : getAllStopIds()) {
			writer.write(stopId + "\t");

			int[] board = this.boards.get(stopId);
			if (board == null){
				log.debug("stopId:\t" + stopId + "\thas null boards!");
			}
			for (int i = 0; i < 24; i++) {
				writer.write((board != null ? board[i] : 0) + "\t");
			}

			int[] alight = this.alights.get(stopId);
			if (alight == null) {
				log.debug("stopId:\t" + stopId + "\thas null alights!");
			}
			for (int i = 0; i < 24; i++) {
				writer.write((alight != null ? alight[i] : 0) + "\t");
			}

			int[] ocuppancy = this.occupancies.get(stopId);
			if (ocuppancy == null) {
				log.debug("stopId:\t" + stopId + "\tthere aren't passengers in Bus after the transfer!");
			}
			for (int i = 0; i < 24; i++) {
				writer.write((ocuppancy != null ? ocuppancy[i] : 0) + "\t");
			}
			writer.writeln();
		}
		writer.write(this.occupancyRecord.toString());
		writer.close();
	}
}
