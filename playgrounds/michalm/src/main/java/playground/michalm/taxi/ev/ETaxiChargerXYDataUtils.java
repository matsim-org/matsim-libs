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

import org.matsim.api.core.v01.Coord;

import playground.michalm.ev.data.*;
import playground.michalm.util.XYDataCollector.XYDataCalculator;

public class ETaxiChargerXYDataUtils {
	public static XYDataCalculator<Charger> createChargerOccupancyCalculator(final EvData evData, boolean relative) {
		String[] header = relative ? //
				new String[] { "plugs", "plugged_rel", "queued_rel", "assigned_rel" } //
				: new String[] { "plugs", "plugged", "queued", "assigned" };

		return new XYDataCalculator<Charger>() {
			@Override
			public String[] getHeader() {
				return header;
			}

			@Override
			public Coord getCoord(Charger charger) {
				return charger.getCoord();
			}

			@Override
			public String[] calculate(Charger charger) {
				ETaxiChargingLogic logic = (ETaxiChargingLogic)charger.getLogic();
				int plugs = charger.getPlugs();
				return new String[] { charger.getPlugs() + "", //
						getValue(logic.getPluggedCount(), plugs, relative), //
						getValue(logic.getQueuedCount(), plugs, relative), //
						getValue(logic.getAssignedCount(), plugs, relative) };
			}

			private String getValue(int count, int plugs, boolean relative) {
				return relative ? ((double)count / plugs) + "" : count + "";
			}
		};
	}
}
