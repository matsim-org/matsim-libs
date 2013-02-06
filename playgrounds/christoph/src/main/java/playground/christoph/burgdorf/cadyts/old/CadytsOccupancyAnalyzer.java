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

package playground.christoph.burgdorf.cadyts.old;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.counts.SimpleWriter;

/**
 * Collects occupancy data of transit-line stations
 */
public class CadytsOccupancyAnalyzer implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	private Map<Id, int[]> occupancies; // Map< stopFacilityId,value[]>
	private final Map<Id, Id> vehStops = new HashMap<Id, Id>(); // Map< vehId,stopFacilityId>
	private final Map<Id, Integer> vehPassengers = new HashMap<Id, Integer>(); // Map<vehId,passengersNo.in Veh>
	private StringBuffer occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
	private final Set<Id> transitDrivers = new HashSet<Id>();
	private final Set<Id> transitVehicles = new HashSet<Id>();
	private final Set<Id> calibratedLines;

//	public CadytsPtOccupancyAnalyzer(final Set<Id> calibratedLines) {
//		this(calibratedLines,3600) ;
//	}
	public CadytsOccupancyAnalyzer(final Set<Id> calibratedLines, int timeBinSize_s ) {
		this.calibratedLines = calibratedLines;
		this.timeBinSize = timeBinSize_s ;

		this.maxTime = Time.MIDNIGHT-1; //24 * 3600 - 1;
		// (yy not completely clear if it might be better to use 24*this.timeBimSize, but it is overall not so great
		// to have this hardcoded.  kai/manuel, jul'12)

		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.occupancies = new HashMap<Id, int[]>();
	}

	@Override
	public void reset(final int iteration) {
		this.occupancies.clear();
		this.vehStops.clear();
		this.vehPassengers.clear();
		this.occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
		this.transitDrivers.clear();
		this.transitVehicles.clear();
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.calibratedLines.contains(event.getTransitLineId())) {
			this.transitDrivers.add(event.getDriverId());
			this.transitVehicles.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}

		// ------------------veh_passenger- (for occupancy)-----------------
		Id vehId = event.getVehicleId(), stopId = this.vehStops.get(vehId);
		double time = event.getTime();
		Integer nPassengers = this.vehPassengers.get(vehId);
		this.vehPassengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
		this.occupancyRecord.append("time :\t" + time + " veh :\t" + vehId + " has Passenger\t" + this.vehPassengers.get(vehId)
				+ " \tat stop :\t" + stopId + " ENTERING PERSON :\t" + event.getPersonId() + "\n");
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-(analyzed-)transit vehicles
		}

		// ----------------veh_passenger-(for occupancy)--------------------------
		Id vehId = event.getVehicleId();
		double time = event.getTime();
		Integer nPassengers = this.vehPassengers.get(vehId);
		if (nPassengers == null) {
			throw new RuntimeException("null passenger-No. in vehicle ?");
		}
		this.vehPassengers.put(vehId, nPassengers - 1);
		if (this.vehPassengers.get(vehId).intValue() == 0) {
			this.vehPassengers.remove(vehId);
		}
		Integer passengers = this.vehPassengers.get(vehId);
		this.occupancyRecord.append("time :\t" + time + " veh :\t" + vehId + " has Passenger\t"
				+ ((passengers != null) ? passengers : 0) + "\n");
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		Id facId = event.getFacilityId();

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
		Id stopId = event.getFacilityId();

		this.vehStops.put(event.getVehicleId(), stopId);
		// (constructing a table with vehId as key, and stopId as value; constructed when veh arrives at
		// stop; necessary
		// since personEnters/LeavesVehicle does not carry stop id)
	}

	// occupancy methods
	public void setOccupancies(final Map<Id, int[]> occupancies) {
		this.occupancies = occupancies;
	}

	@Deprecated // try to use request that also contains time instead
	int getTimeSlotIndex(final double time) {
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
	@Deprecated // try to use request that also contains time instead
	public int[] getOccupancyVolumesForStop(final Id stopId) {
		return this.occupancies.get(stopId);
	}
	 public int getOccupancyVolumeForStopAndTime(final Id stopId, final int time_s ) {
		 int timeBinIndex = getTimeSlotIndex( time_s ) ;
		 return this.occupancies.get(stopId)[timeBinIndex] ;
	 }

	public Set<Id> getOccupancyStopIds() {
		return this.occupancies.keySet();
	}

	public void writeResultsForSelectedStopIds(final String filename, final Counts occupCounts, final Collection<Id> stopIds) {
		SimpleWriter writer = new SimpleWriter(filename);

		final char TAB = '\t';
		final char NL = '\n';

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
		for (Id stopId : stopIds) {
			// get count data
			Count count = occupCounts.getCounts().get(stopId);
			if (!occupCounts.getCounts().containsKey(stopId)) {
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
			for (int i = 0; i < ocuppancy.length; i++) {
				writer.write((ocuppancy != null ? ocuppancy[i] : 0) + TAB);
			}
			writer.write(count.getCoord().toString() + TAB + count.getCsId() + NL);
		}
		writer.write(this.occupancyRecord.toString());
		writer.close();
	}

	private void readEvents(final String eventFileName) {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventFileName);
	}

	public void run(final String eventFileName) {
		this.readEvents(eventFileName);
	}

}
