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
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Provider;

import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

class PricingControler {
	
	private static boolean usingMunich = false;

	public static void main(String[] args) {

		String configFile = args[0];
		String outputDir = args[1];
		String congestionPricing = args[2];
		
		usingMunich = Boolean.valueOf(args[3]);
		
		Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
//		String configFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/output_config.xml.gz";//args[0];
//		String outputDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run11/";//args[1];
//		String congestionPricing = "implV4";//args[2];
//		String networkFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/output_network.xml.gz";
//		String plansFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/output_plans.xml.gz";
//		String personAttributeFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/output_personAttributes.xml.gz";
//		
//		Config config = LoadMyScenarios.getConfigFromPlansNetworkAndConfigFiles(plansFile, networkFile, configFile);
//		config.plans().setInputPersonAttributeFile(personAttributeFile);
//		Scenario sc = ScenarioUtils.loadScenario(config);
//		sc.getConfig().counts().setCountsFileName("../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml");

		sc.getConfig().controler().setOutputDirectory(outputDir);
		sc.getConfig().controler().setWriteEventsInterval(100);
		sc.getConfig().controler().setWritePlansInterval(100);
		
		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		
		TollHandler tollHandler = new TollHandler(sc);
		final TollDisutilityCalculatorFactory fact = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		
		switch (congestionPricing) {
		case "implV3":
		{
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(fact);
				}
			});
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) sc)));
			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 3.");
		}
		break;
		case "implV4":
			{
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(fact);
					}
				});
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV4(controler.getEvents(), sc)));
				Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 4.");
			}
		break;
		case "implV6":
//		{
//			controler.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					bindCarTravelDisutilityFactory().toInstance(fact);
//				}
//			});
//			services.addControlerListener(new MarginalCongestionPricingContolerListener(sc, tollHandler, new CongestionHandlerImplV6(services.getEvents(), sc)));
//			Logger.getLogger(PricingControler.class).info("Using congestion pricing implementation version 6.");
//		}
		break;
		case "none":
		default:
			Logger.getLogger(PricingControler.class).info("Congestion pricing implementation does not match. No pricing implementation is introduced.");
		}
		
		if(usingMunich){
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding("SubtourModeChoice_".concat("COMMUTER_REV_COMMUTER")).toProvider(new javax.inject.Provider<PlanStrategy>() {
						String[] availableModes = {"car", "pt_COMMUTER_REV_COMMUTER"};
						String[] chainBasedModes = {"car", "bike"};

						@Override
						public PlanStrategy get() {
							final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
							builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
							builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
							return builder.build();
						}
					});
				}
			});

			// following is must since, feb15, it is not possible to add two replanning strategies together, even for different sub pop except "ChangeExpBeta"
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
					addPlanStrategyBinding("ReRoute_".concat("COMMUTER_REV_COMMUTER")).toProvider(new javax.inject.Provider<PlanStrategy>() {

						@Override
						public PlanStrategy get() {
							final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
							builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
							return builder.build();
						}
					});
				}
			});
		}
		controler.run();
	}
}
