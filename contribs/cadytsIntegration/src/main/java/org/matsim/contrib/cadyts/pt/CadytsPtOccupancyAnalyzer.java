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

package org.matsim.contrib.cadyts.pt;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.facilities.Facility;
import org.matsim.pt.counts.SimpleWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Collects occupancy data of transit-line stations
 * <p></p>
 * This is probably similar to code elsewhere.  However, it makes some sense to keep this here since the correct workings of cadyts 
 * (obviously) depends on the fact that the counts are actually what it thinks, and so it makes sense to decouple this from the upstream
 * counting method and leave it here. kai, sep'13 
 */
public class CadytsPtOccupancyAnalyzer implements CadytsPtOccupancyAnalyzerI {

	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	private Map<Id<Facility>, int[]> occupancies; // Map< stopFacilityId,value[]>
	private final Map<Id<Vehicle>, Id<Facility>> vehStops = new HashMap<>(); // Map< vehId,stopFacilityId>
	private final Map<Id<Vehicle>, Integer> vehPassengers = new HashMap<>(); // Map<vehId,passengersNo.in Veh>
	private StringBuffer occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
	private final Set<Id> analyzedTransitDrivers = new HashSet<>();
	private final Set<Id> analyzedTransitVehicles = new HashSet<>();
	private final Set<Id<TransitLine>> calibratedLines;

	@Inject
	public CadytsPtOccupancyAnalyzer( Config config ) {
		CadytsConfigGroup ccc = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class ) ;

		this.calibratedLines = toTransitLineIdSet( ccc.getCalibratedItems() ) ;
		this.timeBinSize = ccc.getTimeBinSize() ;

		this.maxTime = Time.MIDNIGHT-1; //24 * 3600 - 1;
		// (yy not completely clear if it might be better to use 24*this.timeBimSize, but it is overall not so great
		// to have this hardcoded.  kai/manuel, jul'12)

		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.occupancies = new HashMap<>();
	}

	@Override
	public void reset(final int iteration) {
		this.occupancies.clear();
		this.vehStops.clear();
		this.vehPassengers.clear();
		this.occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
		this.analyzedTransitDrivers.clear();
		this.analyzedTransitVehicles.clear();
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.calibratedLines.contains(event.getTransitLineId())) {
			this.analyzedTransitDrivers.add(event.getDriverId());
			this.analyzedTransitVehicles.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.analyzedTransitDrivers.contains(event.getPersonId()) || !this.analyzedTransitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}

		// ------------------veh_passenger- (for occupancy)-----------------
		Id<Vehicle> vehId = event.getVehicleId();
		Id<Facility> stopId = this.vehStops.get(vehId);
		double time = event.getTime();
		Integer nPassengers = this.vehPassengers.get(vehId);
		this.vehPassengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
		this.occupancyRecord.append("time :\t").append(time).append(" veh :\t").append(vehId).append(" has Passenger\t").append(this.vehPassengers.get(vehId)).append(" \tat stop :\t").append(stopId).append(" ENTERING PERSON :\t").append(event.getPersonId()).append("\n");
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.analyzedTransitDrivers.contains(event.getPersonId()) || !this.analyzedTransitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}

		// ----------------veh_passenger-(for occupancy)--------------------------
		Id<Vehicle> vehId = event.getVehicleId();
		double time = event.getTime();
		Integer nPassengers = this.vehPassengers.get(vehId);
		if (nPassengers == null) {
			throw new RuntimeException("null passenger-No. in vehicle ?");
		}
		this.vehPassengers.put(vehId, nPassengers - 1);
		if (this.vehPassengers.get(vehId) == 0) {
			this.vehPassengers.remove(vehId);
		}
		Integer passengers = this.vehPassengers.get(vehId);
		this.occupancyRecord.append("time :\t").append(time).append(" veh :\t").append(vehId).append(" has Passenger\t").append((passengers != null) ? passengers : 0).append("\n");
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		Id<Vehicle> vehId = event.getVehicleId();
		Id<Facility> facId = event.getFacilityId();

		// -----------------------occupancy--------------------------------
		this.vehStops.remove(vehId);
		int[] occupancyAtStop = this.occupancies.get(facId);
		if (occupancyAtStop == null) { // no previous departure from this stop, therefore no occupancy
																		// record yet. Create this:
			occupancyAtStop = new int[this.maxSlotIndex + 1];
			this.occupancies.put(facId, occupancyAtStop);
		}

		Integer noPassengersInVeh = this.vehPassengers.get(vehId);

		if (noPassengersInVeh != null) {
			occupancyAtStop[this.getTimeSlotIndex(event.getTime())] += noPassengersInVeh;
			this.occupancyRecord.append(event.getTime());
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(vehId);
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(facId);
			this.occupancyRecord.append("\t");
			this.occupancyRecord.append(noPassengersInVeh);
			this.occupancyRecord.append("\n");
		}
	}

	@Override
	public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
		Id<Facility> stopId = event.getFacilityId();

		this.vehStops.put(event.getVehicleId(), stopId);
		// (constructing a table with vehId as key, and stopId as value; constructed when veh arrives at
		// stop; necessary
		// since personEnters/LeavesVehicle does not carry stop id)
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	/**
	 * @param stopId
	 * @return Array containing the number of passengers in bus after the transfer at the stop
	 *         {@code stopId} per time bin, starting with time bin 0 from 0 seconds to
	 *         (timeBinSize-1)seconds.
	 */
	@Override
	public int[] getOccupancyVolumesForStop(final Id<Facility> stopId) {
		return this.occupancies.get(stopId);
	}
	 /* (non-Javadoc)
	 * @see org.matsim.contrib.cadyts.pt.CadytsPtOccupancyAnalyzerI#getOccupancyVolumeForStopAndTime(org.matsim.api.core.v01.Id, int)
	 */
	@Override
	public int getOccupancyVolumeForStopAndTime(final Id<Facility> stopId, final int time_s ) {
		 if ( this.occupancies.get(stopId) != null ) {
			 int timeBinIndex = getTimeSlotIndex( time_s ) ;
			 return this.occupancies.get(stopId)[timeBinIndex] ;
		 } else {
			 return 0 ;
		 }
	 }

	public Set<Id<Facility>> getOccupancyStopIds() {
		return this.occupancies.keySet();
	}

	@Override
	public void writeResultsForSelectedStopIds(final String filename, final Counts<Link> occupCounts, final Collection<Id<Facility>> stopIds) {
		SimpleWriter writer = new SimpleWriter(filename);

		final String TAB = "\t";
		final String NL = "\n";

		// write header
		writer.write("stopId\t");
		for (int i = 0; i < 24; i++) {
			writer.write("oc" + i + "-" + (i + 1) + TAB);
		}
		for (int i = 0; i < 24; i++) {
			writer.write("scalSim" + i + "-" + (i + 1) + TAB);
		}
		writer.write("coordinate\tcsId\n");

		// write content
		for (Id<Facility> stopId : stopIds) {
			// get count data
			Count count = occupCounts.getCounts().get(Id.create(stopId, Link.class));
			if (!occupCounts.getCounts().containsKey(Id.create(stopId, Link.class))) {
				continue;
			}

			// get sim-Values
			int[] ocuppancy = this.occupancies.get(stopId);
			writer.write(stopId.toString() + TAB);
			for (int i = 0; i < ocuppancy.length; i++) {
				Volume v = count.getVolume(i + 1);
				if (v != null) {
					writer.write(v.getValue() + TAB);
				} else {
					writer.write("n/a" + TAB);
				}
			}
			for (int anOcuppancy : ocuppancy) {
				writer.write((anOcuppancy) + TAB);
			}
			writer.write(count.getCoord().toString() + TAB + count.getCsLabel() + NL);
		}
		writer.write(this.occupancyRecord.toString());
		writer.close();
	}

	@Override
	public String toString() {
		final StringBuilder stringBuffer2 = new StringBuilder();
		final String STOPID = "stopId: ";
		final String VALUES = "; values:";
		final char TAB = '\t';
		final char RETURN = '\n';

		for (Id<Facility> stopId : this.getOccupancyStopIds()) { // Only occupancy!
			StringBuilder stringBuffer = new StringBuilder();
			stringBuffer.append(STOPID);
			stringBuffer.append(stopId);
			stringBuffer.append(VALUES);

			boolean hasValues = false; // only prints stops with volumes > 0
			int[] values = this.getOccupancyVolumesForStop(stopId);

			for (int value : values) {
				hasValues = hasValues || (value > 0);

				stringBuffer.append(TAB);
				stringBuffer.append(value);
			}
			stringBuffer.append(RETURN);
			if (hasValues)
				stringBuffer2.append(stringBuffer.toString());

		}
		return stringBuffer2.toString();
	}

	public static Set<Id<TransitLine>> toTransitLineIdSet(Set<String> list) {
		Set<Id<TransitLine>> converted = new LinkedHashSet<>();
		
		for ( String id : list) {
			converted.add(Id.create(id, TransitLine.class));
		}
		
		return converted;
	}


}
