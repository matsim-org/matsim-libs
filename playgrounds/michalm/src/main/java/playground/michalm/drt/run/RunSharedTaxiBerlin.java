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

package playground.michalm.drt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Provider;

import playground.michalm.drt.analysis.DRTAnalysisModule;
import playground.michalm.drt.optimizer.*;
import playground.michalm.drt.passenger.NDrtRequestCreator;
import playground.michalm.drt.vrpagent.NDrtActionCreator;

public class RunSharedTaxiBerlin {
	public static void main(String[] args) {
		String configFile = "../../../shared-svn/projects/bvg_sharedTaxi/input/config.xml";
		RunSharedTaxiBerlin.run(configFile, false);
	}

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareConfigGroup());
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigGroup taxiCfg = DrtConfigGroup.get(config);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FleetImpl fleet = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleet).parse(taxiCfg.getVehiclesFileUrl(config.getContext()));

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule(fleet,
				createModuleForQSimPlugin(DefaultDrtOptimizerProvider.class), DrtOptimizer.class));
		controler.addOverridingModule(new DRTAnalysisModule());
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	private static com.google.inject.AbstractModule createModuleForQSimPlugin(
			final Class<? extends Provider<? extends DrtOptimizer>> providerClass) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(DrtOptimizer.class).toProvider(providerClass).asEagerSingleton();
				bind(VrpOptimizer.class).to(DrtOptimizer.class);
				bind(DynActionCreator.class).to(NDrtActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(NDrtRequestCreator.class).asEagerSingleton();
			}
		};
	}
}
