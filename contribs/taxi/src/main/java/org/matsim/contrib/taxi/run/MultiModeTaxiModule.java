/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.taxi.run;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeTaxiModule extends AbstractModule {

	@Inject
	private MultiModeTaxiConfigGroup multiModeTaxiCfg;

	@Override
	public void install() {
		List<DvrpModeQSimModule> dvrpModeQSimModules = new ArrayList<>();
		for (TaxiConfigGroup taxiCfg : multiModeTaxiCfg.getTaxiConfigGroups()) {
			dvrpModeQSimModules.add(new DvrpModeQSimModule(taxiCfg.getMode()));
			install(new TaxiModeModule(taxiCfg));
		}

		install(new DvrpModule(dvrpModeQSimModules.stream().toArray(DvrpModeQSimModule[]::new)));

		bind(TravelDisutilityFactory.class).annotatedWith(Taxi.class)
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Taxi.class);
			}
		});
	}
}
