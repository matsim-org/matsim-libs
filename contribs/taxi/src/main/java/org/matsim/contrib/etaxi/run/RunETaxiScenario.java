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

package org.matsim.contrib.etaxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.etaxi.optimizer.ETaxiOptimizerProvider;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.VariableSpeedCharging;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.dvrp.DvrpAuxConsumptionFactory;
import org.matsim.contrib.ev.dvrp.EvDvrpIntegrationModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunETaxiScenario {
	private static final String CONFIG_FILE = "mielec_2014_02/mielec_etaxi_config.xml";
	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8;// up to 80% SOC
	private static final double TEMPERATURE = 20;// oC

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(ETaxiOptimizerProvider::createParameterSet), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new ETaxiModule());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new EvDvrpIntegrationModule());
		controler.configureQSimComponents(EvDvrpIntegrationModule.activateModes(taxiCfg.getMode()));

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).toInstance(
						charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
								VariableSpeedCharging.createStrategyForNissanLeaf(
										charger.getPower() * CHARGING_SPEED_FACTOR, MAX_RELATIVE_SOC)));

				bind(AuxEnergyConsumption.Factory.class).toInstance(
						new DvrpAuxConsumptionFactory(taxiCfg.getMode(), () -> TEMPERATURE,
								RunETaxiScenario::isTurnedOn));
			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	private static boolean isTurnedOn(DvrpVehicleSpecification vehicle, double time) {
		//FIXME should use actual vehicle to check if schedule is STARTED
		return vehicle.getServiceBeginTime() <= time && time <= vehicle.getServiceEndTime();
	}

	public static void main(String[] args) {
		// String configFile = "./src/main/resources/one_etaxi/one_etaxi_config.xml";
		// String configFile =
		// "../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/mielec_etaxi_config.xml";
		RunETaxiScenario.run(CONFIG_FILE, false);
	}
}
