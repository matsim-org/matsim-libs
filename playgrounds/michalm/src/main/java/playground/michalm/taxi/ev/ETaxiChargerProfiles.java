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

package playground.michalm.taxi.ev;

import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.taxi.util.stats.TimeProfiles;

import playground.michalm.ev.data.*;

public class ETaxiChargerProfiles {
	public static ProfileCalculator createChargerOccupancyCalculator(final EvData evData) {
		String[] header = { "plugged", "queued", "assigned" };
		return new TimeProfiles.MultiValueProfileCalculator(header) {
			@Override
			public Integer[] calcValues() {
				int plugged = 0;
				int queued = 0;
				int assigned = 0;
				for (Charger c : evData.getChargers().values()) {
					ETaxiChargingLogic logic = (ETaxiChargingLogic)c.getLogic();
					plugged += logic.getPluggedCount();
					queued += logic.getQueuedCount();
					assigned += logic.getAssignedCount();
				}

				return new Integer[] { plugged, queued, assigned };
			}
		};
	}
}
