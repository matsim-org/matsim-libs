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

package playground.dziemke.cemdapMatsimCadyts.mmoyo.randomizerPtRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.contrib.cadyts.pt.CadytsPtModule;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizingTransitRouterTravelTimeAndDisutility;
import org.matsim.contrib.common.randomizedtransitrouter.RandomizingTransitRouterTravelTimeAndDisutility.DataCollection;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;

import javax.inject.Inject;
import javax.inject.Provider;

//		import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
//		import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility;
//		import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterTravelTimeAndDisutility.DataCollection;

public class RndPtRouterLauncher {

	private static Provider<TransitRouter> createRandomizedTransitRouterFactory (final TransitSchedule schedule, final TransitRouterConfig trConfig, final TransitRouterNetwork routerNetwork){
		return
				() -> {
					RandomizingTransitRouterTravelTimeAndDisutility ttCalculator =
							new RandomizingTransitRouterTravelTimeAndDisutility(trConfig);
					ttCalculator.setDataCollection(DataCollection.randomizedParameters, false) ;
					ttCalculator.setDataCollection(DataCollection.additionalInformation, false) ;
					return new TransitRouterImpl(trConfig, new PreparedTransitSchedule(schedule), routerNetwork, ttCalculator, ttCalculator);
				};
	}

	public static void main(final String[] args) {

		int lastIteration = 10;

		String configFile ;
		final double cadytsWeight;
		if(args.length==3){
			configFile = args[0];
			lastIteration = Integer.parseInt(args[1]);
			cadytsWeight = Double.parseDouble(args[2]);
		}else {
			cadytsWeight = 30.0;
			configFile = "../../../shared-svn/projects/ptManuel/calibration/moyo_1.xml";
		}

		Config config = ConfigUtils.loadConfig(configFile) ;

		config.controler().setLastIteration(lastIteration);
		config.controler().setWriteEventsInterval(1);
		config.vspExperimental().setAbleToOverwritePtInteractionParams(true);


		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		config.addModule(ccc) ;

		int lastStrategyIdx = config.strategy().getStrategySettings().size();
		if ( lastStrategyIdx >= 1 ) {
			throw new RuntimeException("remove all strategy settings from config; should be done here") ;
		}

		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName("ChangeExpBeta");
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}

		{    //////!!!!!!!!!!!!!!!!!!!!!///////
			StrategyConfigGroup.StrategySettings stratSets2 = new StrategyConfigGroup.StrategySettings();
			stratSets2.setStrategyName("ReRoute"); // test that this does work.  Otherwise define this strategy in config file
			stratSets2.setWeight(0.1);
			stratSets2.setDisableAfter(400) ;
			config.strategy().addStrategySettings(stratSets2);
		}

		//load data
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final TransitRouterConfig trConfig = new TransitRouterConfig( config ) ;
		final TransitSchedule trSchedule =  scn.getTransitSchedule();
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(trSchedule, trConfig.getBeelineWalkConnectionDistance());

		//set the controler
		final Controler controler = new Controler(scn);
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		//add cadytsContext as ctrListener
		final double beta = 30. ;

//		controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new CadytsPtModule());

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			//			@Inject private CadytsContext cadytsContext;
			@Inject private CadytsPtContext cadytsContext;
			@Inject
			ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

//				final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cadytsContext);
				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				final double cadytsScoringWeight = cadytsWeight * beta;//config.planCalcScore().getBrainExpBeta();
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});

//		create the factory for rndizedRouter
		final Provider<TransitRouter> randomizedTransitRouterFactory = createRandomizedTransitRouterFactory (trSchedule, trConfig, routerNetwork);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(randomizedTransitRouterFactory);
			}
		});

		//add analyzer for specific bus line and stop Zone conversion
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(false);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);


		controler.run();
	}

}
