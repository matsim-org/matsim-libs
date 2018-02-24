/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.core.replanning;

import com.google.inject.*;
import org.junit.Assert;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioByConfigModule;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collections;

/**
 * @author nagel
 *
 */
public class InnovationSwitchOffTest {
	private static final Logger log = Logger.getLogger(InnovationSwitchOffTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Integration test for testing if switching off of innovative strategies works.
	 */
	@Test
	public void testInnovationSwitchOff() {
		Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("equil"));
		config.controler().setOutputDirectory(this.utils.getOutputDirectory());

		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans2.xml");

		{
			StrategySettings settings = new StrategySettings(Id.create(1, StrategySettings.class));
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.BestScore.toString());
			settings.setWeight(0.5);
			config.strategy().addStrategySettings(settings);
		}
		{
			StrategySettings settings = new StrategySettings(Id.create(2, StrategySettings.class));
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			settings.setWeight(0.5);
			config.strategy().addStrategySettings(settings);
		}
		{
			StrategySettings settings = new StrategySettings(Id.create(3, StrategySettings.class));
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
			settings.setWeight(0.1);
			config.strategy().addStrategySettings(settings);
		}
		{
			StrategySettings settings = new StrategySettings(Id.create(4, StrategySettings.class));
			settings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
			settings.setWeight(0.1);
			settings.setDisableAfter(11);
			config.strategy().addStrategySettings(settings);
		}

		config.strategy().setFractionOfIterationsToDisableInnovation(0.66);

		config.controler().setFirstIteration(10);
		config.controler().setLastIteration(16);

		{
			ActivityParams params = new ActivityParams("h");
			params.setTypicalDuration(12. * 3600.);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("w");
			params.setTypicalDuration(8. * 3600.);
			config.planCalcScore().addActivityParams(params);
		}
		config.controler().setCreateGraphs(false);


		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(AbstractModule.override(Collections.singleton(new ControlerDefaultsModule()), new AbstractModule() {
					@Override
					public void install() {
						bindMobsim().toInstance(new Mobsim() {
							@Override
							public void run() {
							}
						});
					}
				}));
				install(new ScenarioByConfigModule());
				final Provider<StrategyManager> strategyManagerProvider = binder().getProvider(StrategyManager.class);
				addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
					@Override
					public void notifyBeforeMobsim(BeforeMobsimEvent event) {
						System.out.flush();
						log.warn(" Iteration: " + event.getIteration());
						final StrategyManager sm = strategyManagerProvider.get(); // move into controler package if access to sm is a problem. kai, jun'13
						for (int ii = 0; ii < sm.getStrategiesOfDefaultSubpopulation().size(); ii++) {
							log.warn("strategy " + sm.getStrategiesOfDefaultSubpopulation().get(ii) + " has weight " + sm.getWeightsOfDefaultSubpopulation().get(ii));
							if (event.getIteration() == 11 && sm.getStrategiesOfDefaultSubpopulation().get(ii).toString().contains(ReRoute.class.getSimpleName())) {
								Assert.assertEquals(0.1, sm.getWeightsOfDefaultSubpopulation().get(ii), 0.000001);
							}
							if (event.getIteration() == 12 && sm.getStrategiesOfDefaultSubpopulation().get(ii).toString().contains(ReRoute.class.getSimpleName())) {
								Assert.assertEquals(0., sm.getWeightsOfDefaultSubpopulation().get(ii), 0.000001);
							}
							if (event.getIteration() == 13 && sm.getStrategiesOfDefaultSubpopulation().get(ii).toString().contains(TimeAllocationMutator.class.getSimpleName())) {
								Assert.assertEquals(0.1, sm.getWeightsOfDefaultSubpopulation().get(ii), 0.000001);
							}
							if (event.getIteration() == 14 && sm.getStrategiesOfDefaultSubpopulation().get(ii).toString().contains(TimeAllocationMutator.class.getSimpleName())) {
								Assert.assertEquals(0.0, sm.getWeightsOfDefaultSubpopulation().get(ii), 0.000001);
							}
						}
						System.err.flush();
					}
				});
			}
		});
		ControlerI ctrl = injector.getInstance(ControlerI.class);
		ctrl.run();

	}
}

