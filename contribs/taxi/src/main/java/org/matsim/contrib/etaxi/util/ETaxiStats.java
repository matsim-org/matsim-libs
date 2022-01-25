/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.etaxi.util;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public class ETaxiStats {
	public enum ETaxiState {
		QUEUED, PLUGGED
	}

	public final String id;

	public final Map<ETaxiState, Double> stateDurations = new HashMap<>();

	public ETaxiStats(String id) {
		this.id = id;
	}

	public OptionalDouble getFleetQueuedTimeRatio() {
		double queued = stateDurations.getOrDefault(ETaxiState.QUEUED, 0.);
		double plugged = stateDurations.getOrDefault(ETaxiState.PLUGGED, 0.);
		return (queued != 0 && plugged != 0) ? OptionalDouble.of(queued / (queued + plugged)) : OptionalDouble.empty();
	}
}
