/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.data;

import java.util.HashMap;
import java.util.Map;

public class DynamicODMatrix {

	private final Map<Integer, Map<String, Map<String, Integer>>> odms = new HashMap<Integer, Map<String, Map<String, Integer>>>();
	private final int binSize;
	private final double maxTime;
	
	public DynamicODMatrix(final int binSize, final double maxTime) {
		this.binSize = binSize;
		this.maxTime = maxTime;
	}
	
	public int getBinSize() {
		return this.binSize;
	}
	
	public int getNOfBins() {
		return (int) this.maxTime / this.binSize;
	}
	
	public Map<String, Map<String, Integer>> getMatrixForTimeBin(final int timeBinIndex) {
		return this.odms.get(timeBinIndex);
	}

	public void addTrip(final double time, final String fromZoneId, final String toZoneId) {
		addTrip(time, fromZoneId, toZoneId, 1);
	}

	public void addTrip(final double time, final String fromZoneId, final String toZoneId, final int nOfTrips) {
		int slot = getTimeSlot(time);
		Map<String, Map<String, Integer>> odm = this.odms.get(slot);
		if (odm == null) {
			odm = new HashMap<String, Map<String, Integer>>();
			this.odms.put(slot, odm);
		}
		Map<String, Integer> toValues = odm.get(fromZoneId);
		if (toValues == null) {
			toValues = new HashMap<String, Integer>();
			odm.put(fromZoneId, toValues);
			toValues.put(toZoneId, nOfTrips);
		} else {
			Integer oldValue = toValues.get(toZoneId);
			if (oldValue == null) {
				toValues.put(toZoneId, nOfTrips);
			} else {
				toValues.put(toZoneId, oldValue.intValue() + nOfTrips);
			}
		}
	}
	
	private int getTimeSlot(final double time) {
		if (time > maxTime) {
			return (int) this.maxTime / binSize;
		}
		if (time < 0) {
			return 0;
		}
		return (int) time / this.binSize;
	}
}
