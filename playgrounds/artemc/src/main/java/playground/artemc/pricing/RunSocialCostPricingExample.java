/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.artemc.pricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.analysis.AnalysisControlerListener;
import playground.artemc.heterogeneity.scoring.CharyparNagelScoringFunctionForAnalysisFactory;
import playground.artemc.heterogeneity.scoring.DisaggregatedScoreAnalyzer;
import playground.artemc.socialCost.MeanTravelTimeCalculator;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic "script" to use roadpricing.
 * 
 * @author nagel
 *
 */
public final class RunSocialCostPricingExample {
	// to not change class name: referenced from book.  kai, dec'14


	private static String input;
	private static String output;

	public static void main(String[] args) {


		input = args[0];
		output = args[1];

		
		// load the scenario:
		Scenario scenario = initSampleScenario();

		// instantiate the controler:
		Controler controler = new Controler(scenario) ;

		// use the road pricing module.
        // (loads the road pricing scheme, uses custom travel disutility including tolls, etc.)

		controler.setModules(new ControlerDefaultsModule(), new RoadPricingWithoutTravelDisutilityModule(), new LinkOccupancyAnalyzerModule(), new UpdateSocialCostPricingSchemeWithSpillOverModule());
//		controler.addOverridingModule( new AbstractModule() {
//			@Override
//			public void install() {
//				bindToProvider(TravelDisutilityFactory.class, TravelDisutilityTollAndIncomeHeterogeneityProviderWrapper.TravelDisutilityWithPricingAndHeterogeneityProvider.class);
//			}});

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);

		controler.setScoringFunctionFactory(new CharyparNagelScoringFunctionForAnalysisFactory(controler.getConfig().planCalcScore(), controler.getScenario().getNetwork()));
//		controler.addControlerListener(new SimpleAnnealer());
		// Additional analysis
		AnalysisControlerListener analysisControlerListener = new AnalysisControlerListener((MutableScenario) controler.getScenario());
		controler.addControlerListener(analysisControlerListener);
		controler.addControlerListener(new DisaggregatedScoreAnalyzer((MutableScenario) controler.getScenario(),analysisControlerListener.getTripAnalysisHandler()));

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		// run the controler:
		controler.run() ;
	}

	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {

			Controler controler = event.getControler();
			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			transportModes.add(TransportMode.pt);
			transportModes.add(TransportMode.walk);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}

	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig(input+"configRP.xml", new RoadPricingConfigGroup());

		config.network().setInputFile(input + "network.xml");
		boolean isPopulationZipped = new File(input+"population.xml.gz").isFile();
		if(isPopulationZipped){
			config.plans().setInputFile(input+"population.xml.gz");
		}else{
			config.plans().setInputFile(input+"population.xml");
		}

		config.transit().setTransitScheduleFile(input + "transitSchedule.xml");
		config.transit().setVehiclesFile(input + "vehicles.xml");

		if(output!=null){
			config.controler().setOutputDirectory(output);
		}

		//Roadpricing module config
		ConfigUtils.addOrGetModule(config,
		                           RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(input+"roadpricing.xml");


		//config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}

}
