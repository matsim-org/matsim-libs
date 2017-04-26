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

package playground.michalm.taxi.utli.stats;

import org.matsim.contrib.util.*;

public class ETaxiStats {
	public enum ETaxiState {
		QUEUED, PLUGGED;
	}

	public final String id;

	public final EnumAdder<ETaxiState, Long> stateTimeSumsByState = new LongEnumAdder<>(ETaxiState.class);

	public ETaxiStats(String id) {
		this.id = id;
	}

	public double getFleetQueuedTimeRatio() {
		double queued = stateTimeSumsByState.get(ETaxiState.QUEUED);
		double plugged = stateTimeSumsByState.get(ETaxiState.PLUGGED);
		return queued / (queued + plugged);
	}
}