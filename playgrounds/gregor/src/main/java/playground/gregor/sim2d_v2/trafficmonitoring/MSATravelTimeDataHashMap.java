/* *********************************************************************** *
 * project: org.matsim.*
 * MSATravelTimeDataHashMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.trafficmonitoring;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMap;
import org.matsim.core.utils.misc.IntegerCache;

public class MSATravelTimeDataHashMap extends TravelTimeDataHashMap {

	private final int binSize;
	private final Map<Integer,Double> msaTravelTimes;
	private int msaIt = 0;

	public MSATravelTimeDataHashMap(Link link, int binSize, Map<Integer,Double> msaTravelTimes) {
		super(link);
		this.binSize = binSize;
		this.msaTravelTimes = msaTravelTimes;
	}


	@Override
	public void resetTravelTimes() {
		double oldCoef = this.msaIt/(1.+this.msaIt);
		double newCoef = 1./(1.+this.msaIt);
		for (Entry<Integer, Double> e : this.msaTravelTimes.entrySet()) {
			double time = getTimeFromSlotIdx(e.getKey());
			double newTime = Math.min(super.link.getLength()/0.01,super.getTravelTime(e.getKey(), time));
			double oldTime = e.getValue();
			if (newTime != oldTime) {
				int iii =0;
				iii++;
			}
			e.setValue(oldCoef * oldTime + newCoef * newTime);
		}
		this.msaIt++;
		super.resetTravelTimes();
	}

	@Override
	public double getTravelTime(final int timeSlice, final double now) {
		Double ret = this.msaTravelTimes.get(IntegerCache.getInteger(timeSlice));
		if (ret == null) {
			ret = super.getTravelTime(timeSlice, now);
			this.msaTravelTimes.put(timeSlice, ret);
		}
		return ret;
	}

	private double getTimeFromSlotIdx(int timeSlice) {
		return timeSlice * this.binSize;
	}

}
