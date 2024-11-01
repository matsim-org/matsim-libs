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

package org.matsim.contrib.dvrp.trafficmonitoring;

import java.net.URL;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpTravelTimeModule extends AbstractModule {
	public static final String DVRP_INITIAL = "dvrp_initial";
	public static final String DVRP_OBSERVED = "dvrp_observed";
	public static final String DVRP_ESTIMATED = "dvrp_estimated";

	@Inject
	private DvrpConfigGroup dvrpCfg;

	public void install() {
		if (dvrpCfg.initialTravelTimesFile != null) {
			addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).toProvider(() -> {
				URL url = ConfigGroup.getInputFileURL(getConfig().getContext(), dvrpCfg.initialTravelTimesFile);
				var timeDiscretizer = new TimeDiscretizer(getConfig().travelTimeCalculator());
				var linkTravelTimes = DvrpOfflineTravelTimes.loadLinkTravelTimes(timeDiscretizer, url,
						getConfig().global().getDefaultDelimiter());
				return DvrpOfflineTravelTimes.asTravelTime(timeDiscretizer, linkTravelTimes);
			}).asEagerSingleton();
		} else {
			addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).to(QSimFreeSpeedTravelTime.class)
					.asEagerSingleton();
		}
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_OBSERVED).to(
				Key.get(TravelTime.class, Names.named(dvrpCfg.mobsimMode)));
		addTravelTimeBinding(DVRP_ESTIMATED).to(DvrpTravelTimeEstimator.class);

		bind(DvrpOfflineTravelTimeEstimator.class).asEagerSingleton();
		addMobsimListenerBinding().to(DvrpOfflineTravelTimeEstimator.class);
		addControlerListenerBinding().to(DvrpOfflineTravelTimeEstimator.class);

		if (dvrpCfg.travelTimeEstimationBeta > 0 && dvrpCfg.travelTimeEstimationAlpha > 0) {// online estimation
			bind(DvrpOnlineTravelTimeEstimator.class).asEagerSingleton();
			addMobsimListenerBinding().to(DvrpOnlineTravelTimeEstimator.class);
			bind(DvrpTravelTimeEstimator.class).to(DvrpOnlineTravelTimeEstimator.class);

			bind(WithinDayTravelTime.class).asEagerSingleton();
			addEventHandlerBinding().to(WithinDayTravelTime.class);
			addMobsimListenerBinding().to(WithinDayTravelTime.class);

		} else { // offline estimation
			bind(DvrpTravelTimeEstimator.class).to(DvrpOfflineTravelTimeEstimator.class);
		}
	}
}
