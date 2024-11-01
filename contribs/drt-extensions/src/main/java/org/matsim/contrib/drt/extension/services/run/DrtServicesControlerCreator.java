/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesModeModule;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesQSimModule;
import org.matsim.contrib.drt.extension.services.analysis.ServiceAnalysisModule;
import org.matsim.contrib.drt.extension.services.optimizer.DrtServiceOptimizerQSimModule;
import org.matsim.contrib.drt.extension.services.optimizer.DrtServiceQSimModule;
import org.matsim.contrib.drt.extension.services.services.ServiceExecutionModule;
import org.matsim.contrib.drt.extension.services.services.tracker.ServiceExecutionTrackingModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

/**
 * @author steffenaxer
 */
public class DrtServicesControlerCreator {
	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		Controler controler = DrtControlerCreator.createControler(config, otfvis);
		return prepareController(config, controler);
	}

	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param scenario
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, Scenario scenario, boolean otfvis) {
		Controler controler = DrtControlerCreator.createControler(config, scenario, otfvis);
		return prepareController(config, controler);
	}

	private static Controler prepareController(Config config, Controler controler) {
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new DrtServiceQSimModule(drtCfg));
			controler.addOverridingModule(new ServiceExecutionTrackingModule(drtCfg));
			controler.addOverridingModule(new OperationFacilitiesModeModule((DrtWithExtensionsConfigGroup) drtCfg));
			controler.addOverridingModule(new ServiceExecutionModule(drtCfg));
			controler.addOverridingModule(new ServiceAnalysisModule(drtCfg));
			controler.addOverridingQSimModule(new OperationFacilitiesQSimModule(drtCfg));
			controler.addOverridingQSimModule(new DrtServiceOptimizerQSimModule(drtCfg));
		}

		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));
		return controler;
	}
}
