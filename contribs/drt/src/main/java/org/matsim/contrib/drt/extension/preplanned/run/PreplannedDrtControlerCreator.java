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

/**
 *
 */
package org.matsim.contrib.drt.extension.preplanned.run;

import static org.matsim.contrib.drt.run.DrtControlerCreator.createScenarioWithDrtRouteFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.ImmutableSet;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class PreplannedDrtControlerCreator {
	private static final Logger log = Logger.getLogger(PreplannedDrtControlerCreator.class);

	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		Scenario scenario = createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModePreplannedDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		// make sure rebalancing is OFF as this would interfere with simulation of pre-calculated vehicle schedules
		MultiModeDrtConfigGroup.get(config).getModalElements().forEach(drtConfigGroup -> {
			if (drtConfigGroup.getRebalancingParams().isPresent()) {
				log.warn("The rebalancing parameter set is defined for drt mode: "
						+ drtConfigGroup.getMode()
						+ ". It will be ignored. No rebalancing will happen.");
				controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtConfigGroup.getMode()) {
					@Override
					protected void configureQSim() {
						bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
					}
				});
			}
			controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.getMode()) {
				@Override
				public void install() {
					bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
									getter -> new VehicleOccupancyProfileCalculator(getMode(),
											getter.getModal(FleetSpecification.class), 300, getter.get(QSimConfigGroup.class),
											ImmutableSet.of(DrtDriveTask.TYPE, DefaultDrtStopTask.TYPE, WaitForStopTask.TYPE))))
							.asEagerSingleton();
				}
			});
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
