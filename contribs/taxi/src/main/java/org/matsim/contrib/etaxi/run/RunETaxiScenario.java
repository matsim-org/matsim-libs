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

import static org.matsim.contrib.drt.run.DrtControlerCreator.createScenarioWithDrtRouteFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.discharging.IdleDischargingHandler;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.contrib.evrp.EvDvrpFleetQSimModule;
import org.matsim.contrib.evrp.OperatingVehicleProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Key;

public class RunETaxiScenario {
	private static final double CHARGING_SPEED_FACTOR = 1.5; // > 1 in this example
	private static final double MAX_SOC = 0.8; // charge up to 80% SOC
	private static final double TEMPERATURE = 20; // oC

	public static void run(String [] args, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(args,
				new MultiModeTaxiConfigGroup(ETaxiConfigGroups::createWithCustomETaxiOptimizerParams),
				new DvrpConfigGroup(), new OTFVisConfigGroup(), new EvConfigGroup());

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		MultiModeTaxiConfigGroup multiModeTaxiConfig = MultiModeTaxiConfigGroup.get(config);

		Scenario scenario = createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new MultiModeETaxiModule());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EvModule());

		for (TaxiConfigGroup taxiCfg : multiModeTaxiConfig.getModalElements()) {
			controler.addOverridingQSimModule(new EvDvrpFleetQSimModule(taxiCfg.getMode()));
		}

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(IdleDischargingHandler.VehicleProvider.class).to(OperatingVehicleProvider.class);
			}
		});

//		controler.configureQSimComponents(DvrpQSimComponents.activateModes(List.of(EvModule.EV_COMPONENT),
//				multiModeTaxiConfig.modes().collect(toList())));
		controler.configureQSimComponents( DvrpQSimComponents.activateModes( multiModeTaxiConfig.modes().toArray(String[]::new ) ) );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).to(ChargingWithQueueingAndAssignmentLogic.Factory.class);
				//TODO switch to VariableSpeedCharging for Nissan
				bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, CHARGING_SPEED_FACTOR));
				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);

				for (TaxiConfigGroup taxiCfg : multiModeTaxiConfig.getModalElements()) {
					bind(Key.get(ChargingStrategy.Factory.class, DvrpModes.mode(taxiCfg.getMode()))).toInstance(new ChargeUpToMaxSocStrategy.Factory(MAX_SOC));
				}
			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}
}
