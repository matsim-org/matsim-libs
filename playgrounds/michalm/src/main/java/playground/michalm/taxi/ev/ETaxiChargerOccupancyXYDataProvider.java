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

import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.*;

import playground.michalm.ev.data.*;
import playground.michalm.util.XYDataCollector;
import playground.michalm.util.XYDataCollector.XYDataCalculator;

public class ETaxiChargerOccupancyXYDataProvider implements Provider<MobsimListener> {
	private final EvData evData;
	private final MatsimServices matsimServices;

	@Inject
	public ETaxiChargerOccupancyXYDataProvider(EvData evData, MatsimServices matsimServices) {
		this.evData = evData;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		XYDataCalculator<Charger> calc = ETaxiChargerXYDataUtils.createChargerOccupancyCalculator(evData, false);
		return new XYDataCollector<>(evData.getChargers().values(), calc, 300, "charger_occupancy_absolute",
				matsimServices);
	}
}
