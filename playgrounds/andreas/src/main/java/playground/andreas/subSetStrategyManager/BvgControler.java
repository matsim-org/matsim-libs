/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.andreas.subSetStrategyManager;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripTimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import playground.andreas.bvgScoringFunction.BvgScoringFunctionConfigGroup;
import playground.andreas.bvgScoringFunction.BvgScoringFunctionFactory;

import java.util.Set;


public class BvgControler extends Controler {

	private final static Logger log = Logger.getLogger(BvgControler.class);

	private static String singleTripPersonsFile;

    public BvgControler(final Scenario scenario) {
		super(scenario);
		throw new RuntimeException( Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE ) ;
	}

//	@Override
//	protected void runMobSim() {
//
//		log.info("Overriding runMobSim()");
//
//		QSim simulation = (QSim) QSimUtils.createDefaultQSim(this.getScenario(), this.getEvents());
//
//		simulation.getTransitEngine().setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
////		this.events.addHandler(new LogOutputEventHandler());
//
//		if (simulation instanceof ObservableMobsim) {
//			for (MobsimListener l : this.getMobsimListeners()) {
//				((ObservableMobsim)simulation).addQueueSimulationListeners(l);
//			}
//		}
//		simulation.run();
//	}

	private StrategyManager myLoadStrategyManager() {
		SubSetStrategyManager manager = new SubSetStrategyManager();
		StrategyManagerConfigLoader.load(this, manager); // load defaults

		// load special groups
		{
			Set<Id> ids = ReadSingleTripPersons.readStopNameMap(singleTripPersonsFile);
			StrategyManager mgr = new StrategyManager();

			PlanStrategy strategy1 = new PlanStrategyImpl(new ExpBetaPlanSelector(this.getConfig().planCalcScore()));
			mgr.addStrategyForDefaultSubpopulation(strategy1, 0.9);

			PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
			strategy2.addStrategyModule(new TripTimeAllocationMutator(this.getConfig(),7200, true));
			strategy2.addStrategyModule(new ReRoute(getScenario()));
			mgr.addStrategyForDefaultSubpopulation(strategy2, 0.1);
			mgr.addChangeRequestForDefaultSubpopulation(90,strategy2,0.0);

			manager.addSubset(ids, mgr);
		}
		return manager;
	}

	public static void main(final String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: BvgControler configFile singleTripPersonsFile");
		}
		
		String configFile = args[0];
		singleTripPersonsFile = args[1];
		
		log.info("configFile: "+configFile);
		log.info("singleTripPersonsFile: "+singleTripPersonsFile);
		
		Config config;

		// reading the config file:
		config = ConfigUtils.loadConfig(configFile);

		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		config.planCalcScore().addActivityParams(transitActivityParams);

		// reading the scenario (based on the config):
		Scenario sc = ScenarioUtils.loadScenario(config);

//		TransitRouterConfig routerConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		final BvgControler c = new BvgControler(sc);
        c.setScoringFunctionFactory(new BvgScoringFunctionFactory(config.planCalcScore(), new BvgScoringFunctionConfigGroup(config), c.getScenario().getNetwork()));
        AbstractModule myStrategyManagerModule = new AbstractModule() {

            @Override
            public void install() {
				bind(StrategyManager.class).toInstance(c.myLoadStrategyManager());
			}
        };
        c.addOverridingModule(myStrategyManagerModule);
        c.run();
	}
}
