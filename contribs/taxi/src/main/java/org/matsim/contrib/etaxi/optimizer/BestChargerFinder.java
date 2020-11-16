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

package org.matsim.contrib.etaxi.optimizer;

import java.util.stream.Stream;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;

/**
 * @author michalm
 */
public class BestChargerFinder {
	private final BestDispatchFinder dispatchFinder;

	public BestChargerFinder(BestDispatchFinder dispatchFinder) {
		this.dispatchFinder = dispatchFinder;
	}

	public Dispatch<Charger> findBestChargerForVehicle(DvrpVehicle veh, Stream<Charger> chargers) {
		return dispatchFinder.findBestDestination(veh, chargers, Charger::getLink);
	}
}
