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

package playground.mrieser.svi.data.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;

/**
 * @author mrieser
 */
public class DynamicTravelTimeMatrix {

	private final Map<Integer, Map<String, Map<String, Tuple<Double, Integer>>>> odms = new HashMap<Integer, Map<String, Map<String, Tuple<Double, Integer>>>>();
	private final int binSize;
	private final double maxTime;

	public DynamicTravelTimeMatrix(final int binSize, final double maxTime) {
		this.binSize = binSize;
		this.maxTime = maxTime;
	}

	public int getBinSize() {
		return this.binSize;
	}

	public int getNOfBins() {
		return (int) this.maxTime / this.binSize;
	}
	
	public void clear() {
		this.odms.clear();
	}

	public void addTravelTime(final double depTime, final double travelTime, final String fromZoneId, final String toZoneId) {
		int slot = getTimeSlot(depTime);
		Map<String, Map<String, Tuple<Double, Integer>>> odm = this.odms.get(slot);
		if (odm == null) {
			odm = new HashMap<String, Map<String, Tuple<Double, Integer>>>();
			this.odms.put(slot, odm);
		}
		Map<String, Tuple<Double, Integer>> toValues = odm.get(fromZoneId);
		if (toValues == null) {
			toValues = new HashMap<String, Tuple<Double, Integer>>();
			odm.put(fromZoneId, toValues);
			toValues.put(toZoneId, new Tuple<Double, Integer>(travelTime, 1));
		} else {
			Tuple<Double, Integer> oldValue = toValues.get(toZoneId);
			if (oldValue == null) {
				toValues.put(toZoneId, new Tuple<Double, Integer>(travelTime, 1));
			} else {
				int nOfValues = oldValue.getSecond();
				double avgTravTime = (oldValue.getFirst() * nOfValues + travelTime) / (nOfValues + 1);
				toValues.put(toZoneId, new Tuple<Double, Integer>(avgTravTime, nOfValues + 1));
			}
		}
	}

	/**
	 * @param depTime
	 * @param fromZoneId
	 * @param toZoneId
	 * @return averaged travel time from one zone to another, or <code>Double.NaN</code> if no data is available for this relation
	 */
	public double getAverageTravelTime(final double depTime, final String fromZoneId, final String toZoneId) {
		int slot = getTimeSlot(depTime);
		double tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
		if (!Double.isNaN(tt)) {
			return tt;
		}
		while (slot > 0) {
			slot--;
			tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
			if (!Double.isNaN(tt)) {
				return tt;
			}
		}
		slot = getTimeSlot(depTime);
		while (slot*this.binSize < this.maxTime) {
			slot++;
			tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
			if (!Double.isNaN(tt)) {
				return tt;
			}
		}
		return 15.0*60; // well, then... we have to return something, otherwise the scores will be all NaN
	}

	public double getAverageTravelTimeWithUnknown(final double depTime, final String fromZoneId, final String toZoneId) {
		int slot = getTimeSlot(depTime);
		double tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
		if (!Double.isNaN(tt)) {
			return tt;
		}
		while (slot > 0) {
			slot--;
			tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
			if (!Double.isNaN(tt)) {
				return tt;
			}
		}
		slot = getTimeSlot(depTime);
		while (slot*this.binSize < this.maxTime) {
			slot++;
			tt = getAverageTravelTime(slot, fromZoneId, toZoneId);
			if (!Double.isNaN(tt)) {
				return tt;
			}
		}
		return Double.NaN;
	}
	
	private double getAverageTravelTime(final int timeSlot, final String fromZoneId, final String toZoneId) {
		Map<String, Map<String, Tuple<Double, Integer>>> odm = this.odms.get(timeSlot);
		if (odm == null) {
			odm = new HashMap<String, Map<String, Tuple<Double, Integer>>>();
			this.odms.put(timeSlot, odm);
		}
		Map<String, Tuple<Double, Integer>> toValues = odm.get(fromZoneId);
		if (toValues == null) {
			return Double.NaN;
		}
		Tuple<Double, Integer> oldValue = toValues.get(toZoneId);
		if (oldValue == null) {
			return Double.NaN;
		}
		return oldValue.getFirst();
	}

	private int getTimeSlot(final double time) {
		if (time > this.maxTime) {
			return (int) this.maxTime / this.binSize;
		}
		if (time < 0) {
			return 0;
		}
		return (int) time / this.binSize;
	}

	/*package*/ void dump() {
		for (Map.Entry<Integer, Map<String, Map<String, Tuple<Double, Integer>>>> e : this.odms.entrySet()) {
			System.out.println("Time Slot: " + e.getKey());
			Map<String, Map<String, Tuple<Double, Integer>>> odm = e.getValue();

			for (Map.Entry<String, Map<String, Tuple<Double, Integer>>> e2 : odm.entrySet()) {
				for (Map.Entry<String, Tuple<Double, Integer>> e3 : e2.getValue().entrySet()) {
					System.out.println(e2.getKey() + '\t' + e3.getKey() + '\t' + e3.getValue().getFirst() + '\t' + e3.getValue().getSecond());
				}
			}
		}
	}
}
