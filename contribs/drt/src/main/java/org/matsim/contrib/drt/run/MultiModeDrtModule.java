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

package org.matsim.contrib.drt.run;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class MultiModeDrtModule extends AbstractModule {

	@Inject
	private MultiModeDrtConfigGroup multiModeDrtCfg;

	@Override
	public void install() {
		List<String> modes = new ArrayList<>();
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getDrtConfigGroups()) {
			modes.add(drtCfg.getMode());
			install(new DrtModeModule(drtCfg));
			install(new DrtModeAnalysisModule(drtCfg));
		}

		install(new DvrpModule(modes.stream().toArray(String[]::new)));

		bind(TravelDisutilityFactory.class).annotatedWith(Drt.class)
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
		bind(MainModeIdentifier.class).toInstance(new MultiModeDrtMainModeIdentifier(multiModeDrtCfg));

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);
			}
		});
	}
}
