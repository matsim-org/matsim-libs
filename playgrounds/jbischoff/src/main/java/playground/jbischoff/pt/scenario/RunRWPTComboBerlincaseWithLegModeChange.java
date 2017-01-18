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
package playground.jbischoff.pt.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Names;

import playground.jbischoff.pt.router.VariableAccessTransit;
import playground.jbischoff.pt.router.VariableAccessTransitRouterImplFactory;
import playground.jbischoff.pt.router.config.VariableAccessConfigGroup;
import playground.jbischoff.pt.router.config.VariableAccessModeConfigGroup;
import playground.jbischoff.pt.strategy.ChangeSingleLegModeWithPredefinedFromModes;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunRWPTComboBerlincaseWithLegModeChange {
	public static void main(String[] args) {

//		 if (args.length!=1){
//		 throw new RuntimeException("Wrong arguments");
//		 }
		String configfile = "C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/modechoice/config_modechoice.xml";

		Config config = ConfigUtils.loadConfig(configfile, new TaxiConfigGroup(), new TaxiFareConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// special, second transit schedule for our special pt mode, i.e. only
		// railway lines
		String scrappedScheduleFile = config.transit().getTransitScheduleFileURL(config.getContext()).getFile().split(".xml")[0]+"_va.xml"; 
		new TransitScheduleReader(scenario2).readFile(scrappedScheduleFile);
		final TransitSchedule schedule = scenario2.getTransitSchedule();

		VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
		walk.setDistance(1000);
		walk.setTeleported(true);
		walk.setMode("walk");

		VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
		taxi.setDistance(20000);
		taxi.setTeleported(false);
		taxi.setMode("taxi");
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		vacfg.setAccessModeGroup(taxi);
		vacfg.setAccessModeGroup(walk);

		config.addModule(vacfg);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		TaxiData taxiData = new TaxiData();
		new VehicleReader(scenario.getNetwork(), taxiData)
				.readFile(taxiCfg.getTaxisFileUrl(config.getContext()).getFile());
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiModule(taxiData));
		double expAveragingAlpha = 0.05;// from the AV flow paper

		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(expAveragingAlpha));
		controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();

				bind(TransitSchedule.class).annotatedWith(Names.named("variableAccess")).toInstance(schedule);
				addRoutingModuleBinding("avpt").toProvider(VariableAccessTransit.class);

				bind(TransitRouter.class).annotatedWith(Names.named("variableAccess")).toProvider(VariableAccessTransitRouterImplFactory.class);
				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode.toString())
						.toProvider(new Provider<PlanStrategy>() {
							@Inject
							Provider<TripRouter> tripRouterProvider;

							@Override
							public PlanStrategy get() {
								final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
								builder.addStrategyModule(
										new TripsToLegsModule(tripRouterProvider, scenario.getConfig().global()));
								builder.addStrategyModule(new ChangeSingleLegModeWithPredefinedFromModes(
										scenario.getConfig().global(), scenario.getConfig().changeMode()));
								return builder.build();
							}
						});

			}
		});

		controler.run();

	}
}
