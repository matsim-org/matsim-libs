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

import org.matsim.api.basic.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * @author yu
 * 
 */
public class OccupancyAnalyzer implements PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
	private final int timeBinSize, maxSlotIndex;
	private final double maxTime;
	/**
	 * Map<TransitRouteId,Map<VehId,value[]>>
	 */
	private final Map<Id, Map<Id, int[]>> occupancies, getOns, getDowns;

	/**
	 * 
	 */
	public OccupancyAnalyzer(final int timeBinSize, final double maxTime) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = ((int) this.maxTime) / this.timeBinSize + 1;
		occupancies = new HashMap<Id, Map<Id, int[]>>();
		getOns = new HashMap<Id, Map<Id, int[]>>();
		getDowns = new HashMap<Id, Map<Id, int[]>>();
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int) time / this.timeBinSize);
	}

	public void reset(int iteration) {
		this.occupancies.clear();
		this.getOns.clear();
		this.getDowns.clear();
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id trId = ((PersonEntersVehicleEventImpl) event).getTransitRouteId();
		Id vehId = event.getVehicleId();
		double time = event.getTime();
		// --------------------------getOns---------------------------
		Map<Id, int[]> veh_getOn = this.getOns.get(trId);
		if (veh_getOn == null) {
			veh_getOn = new HashMap<Id, int[]>();
			this.getOns.put(trId, veh_getOn);
		}

		int[] getOn = veh_getOn.get(vehId);
		if (getOn == null) {
			getOn = new int[this.maxSlotIndex + 1];
			veh_getOn.put(vehId, getOn);
		}

		getOn[getTimeSlotIndex(time)]++;
		// ------------------------occupancy-----------------------
		Map<Id, int[]> veh_occup = this.occupancies.get(trId);
		if (veh_occup == null) {
			veh_occup = new HashMap<Id, int[]>();
			this.occupancies.put(trId, veh_occup);
		}

		int[] occup = veh_occup.get(vehId);
		if (occup == null) {
			occup = new int[this.maxSlotIndex + 1];
			veh_occup.put(vehId, occup);
		}

		occup[getTimeSlotIndex(time)]++;
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {

		Id trId = ((PersonLeavesVehicleEventImpl) event).getTransitRouteId();
		Id vehId = event.getVehicleId();
		double time = event.getTime();
		// --------------------------getDowns---------------------------
		Map<Id, int[]> veh_getDown = this.getDowns.get(trId);
		if (veh_getDown == null) {
			veh_getDown = new HashMap<Id, int[]>();
			this.getDowns.put(trId, veh_getDown);
		}

		int[] getDown = veh_getDown.get(vehId);
		if (getDown == null) {
			getDown = new int[this.maxSlotIndex + 1];
			veh_getDown.put(vehId, getDown);
		}

		getDown[getTimeSlotIndex(time)]++;
		// ------------------------occupancy-----------------------
		Map<Id, int[]> veh_occup = this.occupancies.get(trId);
		try {
			int[] occup = veh_occup.get(vehId);

			occup[getTimeSlotIndex(time)]--;
		} catch (NullPointerException e) {
			System.err.println(e
					+ "\nthere is not occupancy-data about TransitRoute:\t"
					+ trId + "\tor TransitVehicle:\t" + vehId);
			System.exit(138);
		}
	}

	// private void stratify() {//this should be made by
	// "PtCountsComparisonAlgorithm"
	//
	// }
}
