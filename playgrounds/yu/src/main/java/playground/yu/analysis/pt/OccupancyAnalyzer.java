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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
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
	private final Map<Id, int[]> boards, alights;

	/** Map< vehId,stopFacilityId> */
	private final Map<Id, Id> veh_stops = new HashMap<Id, Id>();

	public OccupancyAnalyzer(final int timeBinSize, final double maxTime) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		this.boards = new HashMap<Id, int[]>();
		this.alights = new HashMap<Id, int[]>();
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
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id stopId = this.veh_stops.get(event.getVehicleId());
		double time = event.getTime();
		// --------------------------getDowns---------------------------
		int[] getDown = this.alights.get(stopId);
		if (getDown == null) {
			getDown = new int[this.maxSlotIndex + 1];
			this.alights.put(stopId, getDown);
		}
		getDown[getTimeSlotIndex(time)]++;
	}

	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id stopId = event.getFacilityId();
		// double time = event.getTime();

		this.veh_stops.put(event.getVehicleId(), stopId);

		// int[] counter = this.vehCounter.get(stopId);
		// if (counter == null) {
		// counter = new int[this.maxSlotIndex + 1];
		// this.vehCounter.put(stopId, counter);
		// }
		// counter[getTimeSlotIndex(time)]++;
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.veh_stops.remove(event.getVehicleId());
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

	/**
	 * @return Set of {@code Id}s containing all stop ids, where the agents alit
	 *         or/and boarded, for which counting-values are available.
	 */
	public Set<Id> getAllStopIds() {
		Set<Id> allStopIds = new TreeSet<Id>();
		allStopIds.addAll(getBoardStopIds());
		allStopIds.addAll(getAlightStopIds());
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

			writer.writeln();
		}
		writer.close();
	}

	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/ITERS/it.1000/1000.events.xml.gz";

		EventsManager em = new EventsManagerImpl();
		OccupancyAnalyzer oa = new OccupancyAnalyzer(3600, 24 * 3600 - 1);
		em.addHandler(oa);

		new MatsimEventsReader(em).readFile(eventsFilename);

		oa
				.write("../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/m2_out_100a_opt/ITERS/it.1000/m2_out_100a_opt.1000.oa.txt");
	}
}
