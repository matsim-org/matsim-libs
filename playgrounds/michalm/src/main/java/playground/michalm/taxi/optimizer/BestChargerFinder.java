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

package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.util.LinkProvider;

import playground.michalm.ev.data.Charger;

/**
 * @author michalm
 */
public class BestChargerFinder {
	private static final LinkProvider<Charger> CHARGER_TO_LINK = (charger) -> charger.getLink();

	private final BestDispatchFinder dispatchFinder;

	public BestChargerFinder(BestDispatchFinder dispatchFinder) {
		this.dispatchFinder = dispatchFinder;
	}

	public Dispatch<Charger> findBestChargerForVehicle(Vehicle veh, Iterable<Charger> chargers) {
		return dispatchFinder.findBestDestination(veh, chargers, CHARGER_TO_LINK);
	}
}
