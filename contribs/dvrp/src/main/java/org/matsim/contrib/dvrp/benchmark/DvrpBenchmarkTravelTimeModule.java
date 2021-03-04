/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.benchmark;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.controler.AbstractModule;

/**
 * @author michalm
 */
public class DvrpBenchmarkTravelTimeModule extends AbstractModule {
	public void install() {
		// Because TravelTimeCalculatorModule is not installed for benchmarking, we need to add a binding
		// for the car mode
		bind(QSimFreeSpeedTravelTime.class).asEagerSingleton();
		addTravelTimeBinding(TransportMode.car).to(QSimFreeSpeedTravelTime.class);
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_ESTIMATED).to(QSimFreeSpeedTravelTime.class);
	}
}
