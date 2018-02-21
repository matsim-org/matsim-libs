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

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

/**
 * @author michalm
 */
public class DvrpTravelTimeModule extends AbstractModule {
	public static final String DVRP_INITIAL = "dvrp_initial";
	public static final String DVRP_OBSERVED = "dvrp_observed";
	public static final String DVRP_ESTIMATED = "dvrp_estimated";

	public void install() {
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).toInstance(new FreeSpeedTravelTime());
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_OBSERVED).to(networkTravelTime());
		addTravelTimeBinding(DVRP_ESTIMATED).to(DvrpTravelTimeEstimator.class);

		bind(DvrpOfflineTravelTimeEstimator.class).asEagerSingleton();
		addMobsimListenerBinding().to(DvrpOfflineTravelTimeEstimator.class);

		if (DvrpConfigGroup.get(getConfig()).getTravelTimeEstimationBeta() > 0) {// online estimation
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
