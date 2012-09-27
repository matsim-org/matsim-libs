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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.counts.SimpleWriter;

/**
 * Collects occupancy data of transit-line stations
 */
public class PtBseOccupancyAnalyzer implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	private Map<Id, int[]> occupancies; // Map< stopFacilityId,value[]>
	private final Map<Id, Id> veh_stops = new HashMap<Id, Id>(); // Map< vehId,stopFacilityId>
	private final Map<Id, Integer> veh_passengers = new HashMap<Id, Integer>(); // Map<vehId,passengersNo.in Veh>
	private StringBuffer occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
	private final Map<Id, Id> vehToRouteId = new HashMap<Id, Id>();
	private final Set<Id> transitDrivers = new HashSet<Id>();
	private final Set<Id> transitVehicles = new HashSet<Id>();
	
	//String constants
	private final static String STR_M44 = "M44";
	private final static String STR_TIME = "time: \t";
	private final static String STR_VEH =	" veh: \t";
	private final static String STR_PASSENGER =	" has Passenger \t";
	private final static String STR_STOP =	" \tat stop: \t";
	private final static String STR_ENTERING =	" ENTERING PERSON :\t";
	private final static String STR_NL =	"\n";
	private final static String STR_TB =	"\t";
	
	public PtBseOccupancyAnalyzer() {
		this.timeBinSize = 3600;
		this.maxTime = 24 * 3600 - 1;
		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.occupancies = new HashMap<Id, int[]>();
	}

	@Override
	public void reset(final int iteration) {
		this.occupancies.clear();
		this.veh_stops.clear();
		this.occupancyRecord = new StringBuffer("time\tvehId\tStopId\tno.ofPassengersInVeh\n");
		this.vehToRouteId.clear();    
		this.transitDrivers.clear();   //clear stored data Manuel apr2012
		this.transitVehicles.clear();   //clear stored data Manuel apr2012
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		this.vehToRouteId.put(event.getVehicleId(), event.getTransitRouteId());
		this.transitDrivers.add(event.getDriverId()); //store transit drivers as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		this.transitVehicles.add(event.getVehicleId()); //store transit vehicles as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles, as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		}
		
		// only specific transit line
		Id transitLineId = this.vehToRouteId.get(event.getVehicleId());
		if (!transitLineId.toString().contains(STR_M44)) {
			return;
		}

		// ------------------veh_passenger- (for occupancy)-----------------
		Id vehId = event.getVehicleId(), stopId = this.veh_stops.get(vehId);
		double time = event.getTime();
		Integer nPassengers = this.veh_passengers.get(vehId);
		this.veh_passengers.put(vehId, (nPassengers != null) ? (nPassengers + 1) : 1);
		this.occupancyRecord.append(STR_TIME + time + STR_VEH + vehId 	+ STR_PASSENGER + this.veh_passengers.get(vehId) + STR_STOP + stopId + STR_ENTERING	+ event.getPersonId() + STR_NL);
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles, as in org.matsim.pt.counts.OccupancyAnalyzer Manuel apr2012
		}
		
		// only specific transit line
		Id transitLineId = this.vehToRouteId.get(event.getVehicleId());
		if (!transitLineId.toString().contains(STR_M44)) {
			return;
		}
		// ----------------veh_passenger-(for occupancy)--------------------------
		Id vehId = event.getVehicleId();
		double time = event.getTime();
		Integer nPassengers = this.veh_passengers.get(vehId);
		if (nPassengers == null) {
			throw new RuntimeException("null passenger-No. in vehicle ?");
		}
		this.veh_passengers.put(vehId, nPassengers - 1);
		if (this.veh_passengers.get(vehId).intValue() == 0) {
			this.veh_passengers.remove(vehId);
		}
		Integer passengers = this.veh_passengers.get(vehId);
		this.occupancyRecord.append(STR_TIME + time + STR_VEH + vehId 	+ STR_PASSENGER + ((passengers != null) ? passengers : 0) + STR_NL);
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		Id facId = event.getFacilityId();

		// -----------------------occupancy--------------------------------
		this.veh_stops.remove(vehId);
		int[] occupancyAtStop = this.occupancies.get(facId);
		if (occupancyAtStop == null) { // no previous departure from this stop, therefore no occupancy
																		// record yet. Create this:
			occupancyAtStop = new int[this.maxSlotIndex + 1];
			this.occupancies.put(facId, occupancyAtStop);
		}

		Integer noPassengersInVeh = this.veh_passengers.get(vehId);

		if (noPassengersInVeh != null) {
			occupancyAtStop[this.getTimeSlotIndex(event.getTime())] += noPassengersInVeh;
			this.occupancyRecord.append(event.getTime());
			this.occupancyRecord.append(STR_TB);
			this.occupancyRecord.append(vehId);
			this.occupancyRecord.append(STR_TB);
			this.occupancyRecord.append(facId);
			this.occupancyRecord.append(STR_TB);
			this.occupancyRecord.append(noPassengersInVeh);
			this.occupancyRecord.append(STR_NL);
		}
	}

	@Override
	public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
		Id stopId = event.getFacilityId();

		this.veh_stops.put(event.getVehicleId(), stopId);
		// (constructing a table with vehId as key, and stopId as value; constructed when veh arrives at
		// stop; necessary
		// since personEnters/LeavesVehicle does not carry stop id)
	}

	// occupancy methods
	public void setOccupancies(final Map<Id, int[]> occupancies) {
		this.occupancies = occupancies;
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
	public int[] getOccupancyVolumesForStop(final Id stopId) {
		return this.occupancies.get(stopId);
	}

	public Set<Id> getOccupancyStopIds() {
		return this.occupancies.keySet();
	}

	public void writeResultsForSelectedStopIds(final String filename, final Counts occupCounts, final List<Id> m44StopIds) {
		SimpleWriter writer = new SimpleWriter(filename);

		// write header
		final String oc = "oc";
		final String sim = "scalSim";
		final String sep = "-";
		final char CHR_HT = '\t';
		final char CHR_NL = '\n';
		StringBuffer countValBuff = new StringBuffer("stopId\t");
		StringBuffer simValBuff = new StringBuffer();
		for (int i = 0; i < 24; i++) {
			countValBuff.append(oc + i + sep + (i + 1) + CHR_HT);
			simValBuff.append(sim + i + sep + (i + 1) + CHR_HT);
		}
		countValBuff.append(simValBuff.toString() + "coordinate\tcsId\n");
		writer.write(countValBuff.toString());

		// write content
		// for (Id stopId : getOccupancyStopIds()) { this is for all stations
		for (Id stopId : m44StopIds) {
			// get count data
			Count count = occupCounts.getCounts().get(stopId);
			if (!occupCounts.getCounts().containsKey(stopId)) {
				continue;
			}

			// get sim-Values
			int[] ocuppancy = this.occupancies.get(stopId);
//			if (ocuppancy == null) {
//				 log.debug("stopId:\t" + stopId + "\tthere aren't passengers in Bus after the transfer!");
//			}
			countValBuff = new StringBuffer(stopId.toString() + CHR_HT);
			simValBuff = new StringBuffer();
			for (int i = 0; i < 24; i++) {
				countValBuff.append(count.getVolume(i + 1).getValue() + CHR_HT); // all volumes from 1 to 24 must be given in counts file, even with 0 as value.
				simValBuff.append((ocuppancy != null ? ocuppancy[i] : 0) + CHR_HT);
			}
			countValBuff.append(simValBuff.toString() + count.getCoord().toString() + CHR_HT + count.getCsId() + CHR_NL);
			writer.write(countValBuff.toString());

			countValBuff = null;
			simValBuff = null;
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

	public static void main(final String[] args) {
		String eventFile = null;
		if (args.length == 1) {
			eventFile = args[0];
		} else {
			eventFile = "../playgrounds/mmoyo/output/routeAnalysis/scoreOff/afterBugRepair/it500/500.events.xml.gz";
		}
		new PtBseOccupancyAnalyzer().run(eventFile);
	}
}
