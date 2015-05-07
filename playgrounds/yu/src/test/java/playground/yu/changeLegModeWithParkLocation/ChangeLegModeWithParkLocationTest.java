/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.changeLegModeWithParkLocation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import playground.yu.tests.ChangeLegModeWithParkLocation;

import javax.inject.Provider;

/**
 * @author yu
 *
 */
public class ChangeLegModeWithParkLocationTest extends MatsimTestCase {
	private static class LegChainModesListener1 implements
			IterationEndsListener {
		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			Controler ctl = event.getControler();
			int itr = event.getIteration();
			String criterion = "";
			switch (itr) {
			case 0:
				criterion = "|walk|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 1:
				criterion = "|pt|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 2:
				criterion = "|car|walk|walk|walk|walk|walk|car|car|";
				break;
			case 3:
				criterion = "|car|walk|walk|walk|car|car|car|car|";
				break;
			case 4:
				criterion = "|car|walk|walk|walk|walk|walk|car|car|";
				break;
			case 5:
				criterion = "|walk|walk|walk|walk|walk|walk|walk|walk|";
				break;
			case 6:
				criterion = "|car|walk|walk|walk|walk|walk|car|car|";
				break;
			case 7:
				criterion = "|pt|walk|walk|walk|walk|walk|walk|pt|";
				break;
			case 8:
				criterion = "|walk|pt|pt|pt|pt|walk|walk|walk|";
				break;
			case 9:
				criterion = "|car|walk|pt|walk|car|car|car|car|";
				break;
			case 10:
				criterion = "|car|walk|pt|walk|walk|pt|car|car|";
				break;
			}
			System.out.println("----->currentIteration=" + itr);

            Plan plan = ctl.getScenario().getPopulation().getPersons().values().iterator()
					.next().getSelectedPlan();
			StringBuilder legChainModes = new StringBuilder("|");
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					legChainModes.append(((Leg) pe).getMode() + "|");
				}
			}

			assertEquals("different legChainModes?", criterion, legChainModes
					.toString());
		}
	}

	private static class LegChainModesListener2 implements
			IterationEndsListener {
		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			Controler ctl = event.getControler();
			int itr = event.getIteration();
			String criterion = "";
			switch (itr) {
			case 0:
				criterion = "|car|car|car|car|car|car|car|car|";
				break;
			case 1:
				criterion = "|car|car|car|car|pt|walk|car|car|";
				break;
			case 2:
				criterion = "|car|pt|pt|walk|pt|walk|car|car|";
				break;
			case 3:
				criterion = "|car|pt|pt|walk|car|car|car|car|";
				break;
			case 4:
				criterion = "|walk|walk|pt|walk|pt|pt|walk|walk|";
				break;
			case 5:
				criterion = "|car|pt|pt|walk|pt|walk|car|car|";
				break;
			case 6:
				criterion = "|car|pt|pt|walk|car|car|car|car|";
				break;
			case 7:
				criterion = "|car|car|car|car|walk|walk|car|car|";
				break;
			case 8:
				criterion = "|walk|pt|pt|pt|pt|walk|walk|walk|";
				break;
			case 9:
				criterion = "|car|pt|walk|walk|car|car|car|car|";
				break;
			case 10:
				criterion = "|car|pt|walk|walk|walk|pt|car|car|";
				break;
			}
			System.out.println("----->currentIteration=" + itr);

            Plan plan = ctl.getScenario().getPopulation().getPersons().values().iterator()
					.next().getSelectedPlan();
			StringBuilder legChainModes = new StringBuilder("|");
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					legChainModes.append(((Leg) pe).getMode() + "|");
				}
			}

			assertEquals("different legChainModes?", criterion, legChainModes
					.toString());
		}
	}

	private static class LegChainModesListener3 implements
			IterationEndsListener {
		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			Controler ctl = event.getControler();
			int itr = event.getIteration();
			String criterion = "";
			switch (itr) {
			case 0:
				criterion = "|pt|pt|pt|pt|pt|pt|pt|pt|";
				break;
			case 1:
				criterion = "|walk|pt|pt|pt|pt|pt|pt|pt|";
				break;
			case 2:
				criterion = "|walk|pt|pt|walk|pt|pt|pt|pt|";
				break;
			case 3:
				criterion = "|walk|pt|pt|walk|pt|walk|pt|pt|";
				break;
			case 4:
				criterion = "|car|pt|pt|pt|pt|pt|car|car|";
				break;
			case 5:
				criterion = "|pt|pt|pt|pt|pt|pt|pt|pt|";
				break;
			case 6:
				criterion = "|pt|pt|pt|pt|pt|walk|pt|pt|";
				break;
			case 7:
				criterion = "|pt|pt|pt|pt|pt|pt|pt|walk|";
				break;
			case 8:
				criterion = "|walk|pt|pt|pt|pt|walk|pt|pt|";
				break;
			case 9:
				criterion = "|pt|pt|walk|pt|pt|pt|pt|pt|";
				break;
			case 10:
				criterion = "|car|pt|walk|pt|pt|pt|car|car|";
				break;
			}
			System.out.println("----->currentIteration=" + itr);

            Plan plan = ctl.getScenario().getPopulation().getPersons().values().iterator()
					.next().getSelectedPlan();
			StringBuilder legChainModes = new StringBuilder("|");
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					legChainModes.append(((Leg) pe).getMode() + "|");
				}
			}

			System.err.println("legChainModes: " + legChainModes ) ;
			assertEquals("different legChainModes?", criterion, legChainModes
					.toString());
		}
	}

	public void testLegChainModes1() {
		// the agent in the initial plan has only "walk" legs.
		Config config = loadConfig(getInputDirectory() + "config1.xml");
		config.controler().setWritePlansInterval(0);
		Controler ctl = new ChangeLegModeWithParkLocationControler(config);
		ctl.addControlerListener(new LegChainModesListener1());
        ctl.getConfig().controler().setCreateGraphs(false);
        ctl.getConfig().controler().setWriteEventsInterval(0);
		ctl.setDumpDataAtEnd(false);

		// !!!NO longer necessary, man can set constanceWalk in
		// "planCalcScore"-Config Group

		// ctl
		// .setScoringFunctionFactory(new
		// CharyparNagelScoringFunctionFactoryWithWalk(
		// config.planCalcScore(), Double
		// .parseDouble(config.findParam(
		// "changeLegModeWithParkLocation", "constantWalk"))));
		ctl.run();
	}

	public void testLegChainModes2() {
		// the agent in the initial plan has only "car" legs.
		Config config = loadConfig(getInputDirectory() + "config2.xml");
		config.controler().setWritePlansInterval(0);
		Controler ctl = new ChangeLegModeWithParkLocationControler(config);
		ctl.addControlerListener(new LegChainModesListener2());
        ctl.getConfig().controler().setCreateGraphs(false);
        ctl.getConfig().controler().setWriteEventsInterval(0);
		ctl.setDumpDataAtEnd(false);
		// !!!NO longer necessary, man can set constanceWalk in
		// "planCalcScore"-Config Group

		// ctl
		// .setScoringFunctionFactory(new
		// CharyparNagelScoringFunctionFactoryWithWalk(
		// config.planCalcScore(), Double.parseDouble(config
		// .findParam("changeLegModeWithParkLocation",
		// "constantWalk"))));
		ctl.run();
	}

	public void testLegChainModes3() {
		// the agent in the initial plan has only "pt" legs.
		final Config config = loadConfig(getInputDirectory() + "config3.xml");
		config.controler().setWritePlansInterval(0);
		
		config.strategy().setMaxAgentPlanMemorySize(5);
		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.toString() ) ;
			stratSets.setWeight(0.0);
			config.strategy().addStrategySettings(stratSets);
		}{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName("abc");
			stratSets.setWeight(0.5);
			config.strategy().addStrategySettings(stratSets);
		}{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.toString() );
			stratSets.setWeight(0.0);
			config.strategy().addStrategySettings(stratSets);
		}
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler ctl = new Controler(config);

		ctl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("abc").toProvider(new Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
						final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new ChangeLegModeWithParkLocation(config, scenario.getNetwork()));
						builder.addStrategyModule(new ReRoute(scenario));
						return builder.build();
					}
				});
			}
		});

		ctl.addControlerListener(new LegChainModesListener3());
        ctl.getConfig().controler().setCreateGraphs(false);
        ctl.getConfig().controler().setWriteEventsInterval(0);
		ctl.setDumpDataAtEnd(false);

		// !!!NO longer necessary, man can set constanceWalk in
		// "planCalcScore"-Config Group

		// ctl
		// .setScoringFunctionFactory(new
		// CharyparNagelScoringFunctionFactoryWithWalk(
		// config.planCalcScore(), Double.parseDouble(config
		// .findParam("changeLegModeWithParkLocation",
		// "constantWalk"))));
		ctl.run();
	}

	private static class ChangeLegModeWithParkLocationControler extends
			Controler {
		public ChangeLegModeWithParkLocationControler(final Config config) {
			super(config);
			final Scenario scenario = this.getScenario() ;

			final Provider<PlanStrategy> planStrategyFactory = new javax.inject.Provider<PlanStrategy>(){
				@Override public PlanStrategy get() {
					PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder( new RandomPlanSelector<Plan,Person>() ) ;
					builder.addStrategyModule(new ChangeLegModeWithParkLocation(config, scenario.getNetwork()));
					builder.addStrategyModule(new ReRoute(getScenario()));
					return builder.build();
				}
			} ;
			this.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addPlanStrategyBinding("abc").toProvider(planStrategyFactory);
                }
            });

			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName("abc") ;
			stratSets.setWeight(0.5);
			config.strategy().addStrategySettings(stratSets);

//			throw new RuntimeException("overriding loadStrategyManager is deprecated; for that reason, this class does not work any more. kai, oct'14") ;
		}

//		@Override
//		protected StrategyManager loadStrategyManager() {
//			StrategyManager manager = new StrategyManager();
//			StrategyManagerConfigLoader.load(this, manager);
//			manager.setMaxPlansPerAgent(5);
//			//
//			// PlanStrategy strategy1 = new PlanStrategy(new
//			// ExpBetaPlanChanger());
//			// manager.addStrategy(strategy1, 0.1);
//
//			PlanStrategyImpl strategy2 = new PlanStrategyImpl(
//					new RandomPlanSelector());
//			strategy2.addStrategyModule(new ChangeLegModeWithParkLocation(
//					config, network));
//			strategy2.addStrategyModule(new ReRoute(getScenario()));
//			manager.addStrategyForDefaultSubpopulation(strategy2, 0.5);
//
//			// PlanStrategy strategy3 = new PlanStrategy(new
//			// RandomPlanSelector());
//			// strategy3.addStrategyModule(new ReRoute(this));
//			// manager.addStrategy(strategy3, 0.4);
//
//			return manager;
//		}
	}
}
