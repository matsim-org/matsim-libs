/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class PricingControler {
	
	private static final boolean usingMunich = true;

	public static void main(String[] args) {

		String configFile = args[0];
		String outputDir = args[1];
		String congestionPricing = args[2];
		
		Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
//		String configFile = "../../../repos/runs-svn/siouxFalls/run204/baseCase/config_congestionPricing_baseCaseCtd.xml";//args[0];
//		String outputDir = "../../../repos/runs-svn/siouxFalls/run204/policies/v4/";//args[1];
//		String congestionPricing = "implV4";//args[2];
//		String networkFile = "../../../repos/runs-svn/siouxFalls/run204/baseCase/output_network.xml.gz";
//		String plansFIle = "../../../repos/runs-svn/siouxFalls/run204/baseCase/output_plans.xml.gz";
//		
//		Scenario sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(plansFIle, networkFile, configFile);
		
		sc.getConfig().controler().setOutputDirectory(outputDir);
		sc.getConfig().controler().setWriteEventsInterval(100);
		sc.getConfig().controler().setWritePlansInterval(100);
		
		final Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
        controler.setDumpDataAtEnd(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		TollHandler tollHandler = new TollHandler(sc);
		TollDisutilityCalculatorFactory fact = new TollDisutilityCalculatorFactory(tollHandler);
		
		switch (congestionPricing) {
		case "implV3":
		{
			controler.setTravelDisutilityFactory(fact);
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 3.");
		}
		break;
		case "implV4":
			{
				controler.setTravelDisutilityFactory(fact);
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV4(controler.getEvents(), sc)));
				Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 4.");
			}
			break;
		
		case "implV5":
		{
			controler.setTravelDisutilityFactory(fact);
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new MarginalCongestionHandlerImplV5(controler.getEvents(), sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 5.");
		}
		break;
		
		case "implV6":
		{
			controler.setTravelDisutilityFactory(fact);
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV6(controler.getEvents(), sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 6.");
		}
		break;
		
		case "none":
		default:
			Logger.getLogger(PricingControler.class).info("Congestion pricing implementation does not match. No pricing implementation is introduced.");
		}
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		
		if(usingMunich){
			controler.addPlanStrategyFactory("SubtourModeChoice_".concat("COMMUTER_REV_COMMUTER"), new PlanStrategyFactory() {
				String [] availableModes = {"car","pt_COMMUTER_REV_COMMUTER"};
				String [] chainBasedModes = {"car","bike"};
				@Override
				public PlanStrategy get() {
					PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
					builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
					builder.addStrategyModule(new ReRoute(controler.getScenario()));
					return builder.build();
				}
			});

			// following is must since, feb15, it is not possible to add two replanning strategies together, even for different sub pop except "ChangeExpBeta"
			controler.addPlanStrategyFactory("ReRoute_".concat("COMMUTER_REV_COMMUTER"), new PlanStrategyFactory() {

				@Override
				public PlanStrategy get() {
					PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
					builder.addStrategyModule(new ReRoute(controler.getScenario()));
					return builder.build();
				}
			});
		}
		
		controler.run();
		
	}

}
