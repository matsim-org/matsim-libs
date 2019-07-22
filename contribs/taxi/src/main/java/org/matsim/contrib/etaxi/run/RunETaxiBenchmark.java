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

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkConfigConsistencyChecker;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkControlerModule;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkModule;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListenerModule;
import org.matsim.contrib.etaxi.optimizer.ETaxiOptimizerProvider;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.ev.dvrp.EvDvrpFleetQSimModule;
import org.matsim.contrib.ev.dvrp.EvDvrpIntegrationModule;
import org.matsim.contrib.ev.dvrp.OperatingVehicleProvider;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.common.collect.ImmutableSet;

/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that link travel times are
 * deterministic. To simulate this property, we remove (1) all other traffic, and (2) link capacity constraints (e.g. by
 * increasing the capacities by 100+ times), as a result all vehicles move with the free-flow speed (which is the
 * effective speed).
 * <p>
 * </p>
 * To model the impact of traffic, we can use a time-variant network, where we specify different free-flow speeds for
 * each link over time. The default approach is to specify free-flow speeds in each time interval (usually 15 minutes).
 */
public class RunETaxiBenchmark {
	public static void run(URL configUrl, int runs) {
		Config config = ConfigUtils.loadConfig(configUrl,
				new TaxiConfigGroup(ETaxiOptimizerProvider::createParameterSet), new DvrpConfigGroup(),
				new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		createControler(config, runs).run();
	}

	public static Controler createControler(Config config, int runs) {
		config.controler().setLastIteration(runs - 1);
		config.controler().setDumpDataAtEnd(false);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);

		DvrpConfigGroup.get(config).setNetworkModes(ImmutableSet.of());// to switch off network filtering
		config.addConfigConsistencyChecker(new DvrpBenchmarkConfigConsistencyChecker());

		String mode = TaxiConfigGroup.get(config).getMode();
		Scenario scenario = RunTaxiBenchmark.loadBenchmarkScenario(config, 15 * 60, 30 * 3600);

		Controler controler = new Controler(scenario);
		controler.setModules(new DvrpBenchmarkControlerModule());
		controler.addOverridingModule(new DvrpBenchmarkModule());
		controler.configureQSimComponents(EvDvrpIntegrationModule.activateModes(mode));

		controler.addOverridingModule(new ETaxiModule());
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new EvDvrpIntegrationModule());
		controler.addOverridingQSimModule(new EvDvrpFleetQSimModule(mode));

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(AuxDischargingHandler.VehicleProvider.class).to(OperatingVehicleProvider.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).toProvider(new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
						charger -> new ChargeUpToMaxSocStrategy(charger, 0.8)));
				//TODO switch to VariableSpeedCharging for Nissan
				bind(ChargingPower.Factory.class).toInstance(ev -> new FixedSpeedCharging(ev, 2.0));
				bind(TemperatureService.class).toInstance(linkId -> 20);
			}
		});

		controler.addOverridingModule(QSimScopeObjectListenerModule.builder(ETaxiBenchmarkStats.class).mode(mode)
				.objectClass(Fleet.class)
				.listenerCreator(getter -> new ETaxiBenchmarkStats(getter.get(OutputDirectoryHierarchy.class)))
				.build());

		return controler;
	}
}
