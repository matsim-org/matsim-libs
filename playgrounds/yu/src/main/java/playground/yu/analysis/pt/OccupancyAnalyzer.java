/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTrRouteStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.analysis.pt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class OccupancyAnalyzer implements PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler {
	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	/** Map< stopFacilityId,value[]> */
	private Map<Id, int[]> boards, alights, occupancies;

	/** Map< vehId,stopFacilityId> */
	private final Map<Id, Id> veh_stops = new HashMap<Id, Id>();
	/** Map<vehId,passengersNo. in Veh> */
	private final Map<Id, Integer> veh_passengers = new HashMap<Id, Integer>();
	private StringBuffer occupancyRecord;

	// TODO Version 1. vehId<->n_passengers[not
	// array];vehId<->Line+H/R,[handle(Impl
	// enter/leave)] (refresh with
	// AgentArrivalEvnet??? maybe later);handle(VehDepartsAtFa...) < accumulate
	// all enRoute agentsNr. in this veh/bus in the according Line+H/R, where
	// the stop belongs, [map<stopId,line+H/R, need scheduleFile] in this time

	// TODO Version 2 [[better???]]. vehId<->n_passengers[not
	// array];handle(VehDepartsAtFa...) < accumulate all enRoute agentsNr. in
	// this veh/bus just at this stop
	public OccupancyAnalyzer(final int timeBinSize, final double maxTime) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.boards = new HashMap<Id, int[]>();
		this.alights = new HashMap<Id, int[]>();
		this.occupancies = new HashMap<Id, int[]>();
	}

	public void setBoards(Map<Id, int[]> boards) {
		this.boards = boards;
	}

	public void setAlights(Map<Id, int[]> alights) {
		this.alights = alights;
	}

	public void setOccupancies(Map<Id, int[]> occupancies) {
		this.occupancies = occupancies;
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	public void reset(int iteration) {
		this.boards.clear();
		this.alights.clear();
		this.occupancies.clear();
		this.veh_stops.clear();
		this.occupancyRecord = new StringBuffer(
				"time\tvehId\tStopId\tno.ofPassengersInVeh\n");
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vehId = event.getVehicleId(), stopId = this.veh_stops.get(vehId);
		double time = event.getTime();
		// --------------------------getOns---------------------------
		int[] getOn = this.boards.get(stopId);
		if (getOn == null) {
			getOn = new int[this.maxSlotIndex + 1];
			this.boards.put(stopId, getOn);
		}
		getOn[getTimeSlotIndex(time)]++;
		// ------------------------veh_passenger---------------------------
		Integer nPassengers = this.veh_passengers.get(vehId);
		this.veh_passengers.put(vehId,
				(nPassengers != null) ? (nPassengers + 1) : 1);
		this.occupancyRecord.append("time :\t" + time + " veh :\t" + vehId
				+ " has Passenger\t" + this.veh_passengers.get(vehId)
				+ " \tat stop :\t" + stopId + " ENTERING PERSON :\t"
				+ event.getPersonId() + "\n");
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id vehId = event.getVehicleId();
		Id stopId = this.veh_stops.get(vehId);
		double time = event.getTime();
		// --------------------------getDowns---------------------------
		int[] getDown = this.alights.get(stopId);
		if (getDown == null) {
			getDown = new int[this.maxSlotIndex + 1];
			this.alights.put(stopId, getDown);
		}
		getDown[getTimeSlotIndex(time)]++;
		// ------------------------veh_passenger---------------------------
		Integer nPassengers = this.veh_passengers.get(vehId);
		if (nPassengers == null)
			throw new RuntimeException("negative passenger-No. in vehicle?");
		this.veh_passengers.put(vehId, nPassengers - 1);
		if (this.veh_passengers.get(vehId).intValue() == 0)
			this.veh_passengers.remove(vehId);

		Integer passengers = this.veh_passengers.get(vehId);
		this.occupancyRecord.append("time :\t" + time + " veh :\t" + vehId
				+ " has Passenger\t" + ((passengers != null) ? passengers : 0)
				+ "\n");
	}

	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id stopId = event.getFacilityId();
		this.veh_stops.put(event.getVehicleId(), stopId);
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id stopId = event.getFacilityId();
		Id vehId = event.getVehicleId();
		this.veh_stops.remove(vehId);
		// -----------------------occupancy--------------------------------
		int[] occupancyAtStop = this.occupancies.get(stopId);
		if (occupancyAtStop == null) {
			occupancyAtStop = new int[this.maxSlotIndex + 1];
			this.occupancies.put(stopId, occupancyAtStop);
		}
		Integer noPassengersInVeh = this.veh_passengers.get(vehId);
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
	public int[] getBoardVolumesForStop(final Id stopId) {
		return this.boards.get(stopId);
	}

	/**
	 * @param stopId
	 * @return Array containing the number of agents alighting at the stop
	 *         {@code stopId} per time bin, starting with time bin 0 from 0
	 *         seconds to (timeBinSize-1)seconds.
	 */
	public int[] getAlightVolumesForStop(final Id stopId) {
		return this.alights.get(stopId);
	}

	/**
	 * @param stopId
	 * @return Array containing the number of passengers in bus after the
	 *         transfer at the stop {@code stopId} per time bin, starting with
	 *         time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getOccupancyVolumesForStop(final Id stopId) {
		return this.occupancies.get(stopId);
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents
	 *         boarded, for which counting-values are available.
	 */
	public Set<Id> getBoardStopIds() {
		return this.boards.keySet();
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents
	 *         alit, for which counting-values are available.
	 */
	public Set<Id> getAlightStopIds() {
		return this.alights.keySet();
	}

	public Set<Id> getOccupancyStopIds() {
		return this.occupancies.keySet();
	}

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents alit
	 *         or/and boarded, for which counting-values are available.
	 */
	public Set<Id> getAllStopIds() {
		Set<Id> allStopIds = new TreeSet<Id>();
		allStopIds.addAll(getBoardStopIds());
		allStopIds.addAll(getAlightStopIds());
		allStopIds.addAll(getOccupancyStopIds());
		return allStopIds;
	}

	public void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		// write filehead
		writer.write("stopId\t");
		for (int i = 0; i < 24; i++)
			writer.write("bo" + i + "-" + (i + 1) + "\t");
		for (int i = 0; i < 24; i++)
			writer.write("al" + i + "-" + (i + 1) + "\t");
		for (int i = 0; i < 24; i++)
			writer.write("oc" + i + "-" + (i + 1) + "\t");
		writer.writeln();
		// write content
		for (Id stopId : getAllStopIds()) {
			writer.write(stopId + "\t");

			int[] board = this.boards.get(stopId);
			if (board == null)
				System.out.println("stopId:\t" + stopId + "\thas null boards!");
			for (int i = 0; i < 24; i++) {
				writer.write((board != null ? board[i] : 0) + "\t");
			}

			int[] alight = this.alights.get(stopId);
			if (alight == null)
				System.out
						.println("stopId:\t" + stopId + "\thas null alights!");
			for (int i = 0; i < 24; i++) {
				writer.write((alight != null ? alight[i] : 0) + "\t");
			}

			int[] ocuppancy = this.occupancies.get(stopId);
			if (ocuppancy == null)
				System.out
						.println("stopId:\t"
								+ stopId
								+ "\tthere aren't passengers in Bus after the transfer!");
			for (int i = 0; i < 24; i++) {
				writer.write((ocuppancy != null ? ocuppancy[i] : 0) + "\t");
			}
			writer.writeln();
		}
		writer.write(this.occupancyRecord.toString());
		writer.close();
	}

	public static void main(String[] args) {
		String netFilename = "";
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/ITERS/it.1000/1000.events.xml.gz";
		String transitScheduleFilename = "";

		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().scenario().setUseTransit(true);

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		TransitSchedule schedule = new TransitScheduleFactoryImpl()
				.createTransitSchedule();
		try {
			new TransitScheduleReader(new ScenarioImpl())
					.readFile(transitScheduleFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		EventsManager em = new EventsManagerImpl();
		OccupancyAnalyzer oa = new OccupancyAnalyzer(3600, 24 * 3600 - 1);
		em.addHandler(oa);

		new MatsimEventsReader(em).readFile(eventsFilename);

		oa
				.write("../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/m2_out_100a_opt/ITERS/it.1000/m2_out_100a_opt.1000.oa.txt");
	}
}
