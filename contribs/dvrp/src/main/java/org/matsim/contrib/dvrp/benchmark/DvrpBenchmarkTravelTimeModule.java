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

import java.net.URL;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpOfflineTravelTimes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpBenchmarkTravelTimeModule extends AbstractModule {
	@Inject
	private DvrpConfigGroup dvrpCfg;

	public void install() {
		if (dvrpCfg.initialTravelTimesFile != null) {
			addTravelTimeBinding(DvrpTravelTimeModule.DVRP_ESTIMATED).toProvider(() -> {
				URL url = ConfigGroup.getInputFileURL(getConfig().getContext(), dvrpCfg.initialTravelTimesFile);
				var timeDiscretizer = new TimeDiscretizer(getConfig().travelTimeCalculator());
				var linkTravelTimes = DvrpOfflineTravelTimes.loadLinkTravelTimes(timeDiscretizer, url,
						getConfig().global().getDefaultDelimiter());
				return DvrpOfflineTravelTimes.asTravelTime(timeDiscretizer, linkTravelTimes);
			}).asEagerSingleton();
		} else {
			addTravelTimeBinding(DvrpTravelTimeModule.DVRP_ESTIMATED).to(QSimFreeSpeedTravelTime.class)
					.asEagerSingleton();
		}

		// Because TravelTimeCalculatorModule is not installed for benchmarking, we need to add a binding
		// for the car mode
		addTravelTimeBinding(TransportMode.car).to(
				Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
	}
}
