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
package playground.ikaddoura.noise;

import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;

/**
 * @author lkroeger
 *
 */

public class NoiseInternalizationControler {
//	private static final Logger log = Logger.getLogger(NoiseInternalizationControler.class);
	
	static String configFile = null;
	double annualCostRate = (85.0/(1.95583))*(Math.pow(1.02, (2014-1995)));

	public static void main(String[] args) throws IOException {
		
		configFile = "/Users/Lars/Desktop/VERSUCH/configB.xml";
		
		NoiseInternalizationControler noiseInternalizationControler = new NoiseInternalizationControler();
		
//		noiseInternalizationControler.runBaseCase(configFile);
		noiseInternalizationControler.runInternalization(configFile);
	}

//	private void runBaseCase(String configFile) {
//		Controler controler = new Controler(configFile);
//// 		adapt the WelfareAnalysisControlerListener for the noise damage
//		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
//		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
//		controler.setOverwriteFiles(true);
//		controler.run();
//		
//	}

	private void runInternalization(String configFile) {
		Controler controler = new Controler(configFile);
		EventsManager events = controler.getEvents();
		
//		controler.getConfig().getModule("plans").addParam("inputPlansFile", "/Users/Lars/Desktop/noiseInternalization20/output/output_plans.xml.gz");
		
		SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
		
		NoiseTollHandler tollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, 0.);
		NoiseCostPricingHandler pricingHandler = new NoiseCostPricingHandler(events);
		
		NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(tollHandler);
		controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
		
		controler.addControlerListener(new NoiseInternalizationControlerListener( (ScenarioImpl) controler.getScenario(), tollHandler, pricingHandler, spatialInfo ));
//		controler.addControlerListener(new NoiseInternalizationControlerListenerWithoutPricing( (ScenarioImpl) controler.getScenario(), tollHandler, noiseHandler, spatialInfo ));
		
// 		adapt the WelfareAnalysisControlerListener for the noise damage
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
