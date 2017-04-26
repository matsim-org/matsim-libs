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

/**
 * 
 */
package playground.jbischoff.csberlin.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ChangeSingleLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.jbischoff.analysis.TripHistogram;
import playground.jbischoff.analysis.TripHistogramListener;
import playground.jbischoff.csberlin.replanning.BerlinCSPermissibleModesCalculator;
import playground.jbischoff.csberlin.replanning.ChangeSingleLegModeWithPermissibleModes;
import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.ffcs.sim.SetupFreefloatingParking;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunCSBerlinBasecaseWithParkingFreefloating {
	public static void main(String[] args) {
		String runId;
		String configfile;
		String outputdir;
		if (args.length<2){
		configfile = "../../../shared-svn/projects/bmw_carsharing/data/scenario/configBCParkingFreeFloat14.xml";
		runId = "bc17_test_nocars";
		outputdir = "D:/runs-svn/bmw_carsharing/basecase/"+runId;
		}
		else {
			configfile = args[0];
			runId = args[1];
			outputdir = args[2];
		}
		Config config = ConfigUtils.loadConfig(configfile, new FFCSConfigGroup(), new DvrpConfigGroup());
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputdir);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Controler controler = new Controler(config);
		SetupFreefloatingParking.installFreefloatingParkingModules(controler, (FFCSConfigGroup) config.getModule("freefloating"));
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addControlerListenerBinding().to(TripHistogramListener.class).asEagerSingleton();
				bind(TripHistogram.class).asEagerSingleton();
				bind(BerlinCSPermissibleModesCalculator.class).asEagerSingleton();
			
				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.toString()).toProvider(new Provider<PlanStrategy>() {
					@Inject Scenario scenario;
					@Inject Provider<TripRouter> tripRouterProvider;
					@Inject BerlinCSPermissibleModesCalculator berlinCSAvailableModeSelector;
					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, scenario.getConfig().global()));
						builder.addStrategyModule(new ChangeSingleLegModeWithPermissibleModes(scenario.getConfig().global().getNumberOfThreads(), berlinCSAvailableModeSelector));
						builder.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
						return builder.build();


					}
				});
			}
		});
		
		
		controler.run();
		
		
	}
}
