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

package org.matsim.contrib.edrt.run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunEDrtScenario {
	private static final String CONFIG_FILE = "mielec_2014_02/mielec_edrt_config.xml";
	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8;// charge up to 80% SOC
	private static final double MIN_RELATIVE_SOC = 0.2;// send to chargers vehicles below 20% SOC
	private static final double TEMPERATURE = 20;// oC

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		Controler controler = EDrtControlerCreator.createControler(config, otfvis);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EDrtVehicleDataEntryFactoryProvider.class).toInstance(
						new EDrtVehicleDataEntryFactoryProvider(MIN_RELATIVE_SOC));

				bind(ChargingLogic.Factory.class).toInstance(
						charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
								new FixedSpeedChargingStrategy(charger.getPower() * CHARGING_SPEED_FACTOR,
										MAX_RELATIVE_SOC)));

				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);
			}
		});

		return controler;
	}

	public static void main(String[] args) {
		RunEDrtScenario.run(CONFIG_FILE, false);
	}
}
