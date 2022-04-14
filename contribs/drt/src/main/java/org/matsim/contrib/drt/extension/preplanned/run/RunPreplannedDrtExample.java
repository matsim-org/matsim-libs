/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.preplanned.run;

import static org.matsim.contrib.drt.extension.preplanned.optimizer.PreplannedDrtOptimizer.PreplannedSchedules;

import java.net.URL;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michal.mac
 */
public class RunPreplannedDrtExample {
	private static final Logger log = Logger.getLogger(RunPreplannedDrtExample.class);

	public static void run(URL configUrl, boolean otfvis, int lastIteration,
			Map<String, PreplannedSchedules> preplannedSchedulesByMode) {
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			if (drtCfg.getRebalancingParams().isPresent()) {
				log.warn("The rebalancing parameter set is defined for drt mode: "
						+ drtCfg.getMode()
						+ ". It will be ignored. No rebalancing will happen.");
				drtCfg.removeParameterSet(drtCfg.getRebalancingParams().get());
			}
		}

		Controler controler = PreplannedDrtControlerCreator.createControler(config, otfvis);

		MultiModeDrtConfigGroup.get(config)
				.getModalElements()
				.stream()
				.map(DrtConfigGroup::getMode)
				.forEach(mode -> controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(mode) {
					@Override
					protected void configureQSim() {
						bindModal(PreplannedSchedules.class).toInstance(preplannedSchedulesByMode.get(mode));
					}
				}));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}
}
