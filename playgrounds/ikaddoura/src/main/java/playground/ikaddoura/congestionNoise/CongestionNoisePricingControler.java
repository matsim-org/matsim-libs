/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.congestionNoise;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;
import playground.ikaddoura.noise.ExtCostEventHandlerNoise;
import playground.ikaddoura.noise.NoiseCostPricingHandler;
import playground.ikaddoura.noise.NoiseInternalizationControlerListener;
import playground.ikaddoura.noise.NoiseInternalizationControlerListenerWithoutPricing;
import playground.ikaddoura.noise.NoiseTollDisutilityCalculatorFactory;
import playground.ikaddoura.noise.NoiseTollHandler;
import playground.ikaddoura.noise.SpatialInfo;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListner;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author ikaddoura
 *
 */
public class CongestionNoisePricingControler {
	
	private static final Logger log = Logger.getLogger(CongestionNoisePricingControler.class);
	
	static String configFile;
	static String runId; // congestion, noise, baseCase, congestionNoise
	static double annualCostRate;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("first argument (config file): "+ configFile);
			
			runId = args[1];
			log.info("second argument (rund Id): "+ runId);
			
			annualCostRate = Double.valueOf(args[2]);
			log.info("third argument (annual cost rate): "+ annualCostRate);	

		} else {
			configFile = "../../shared-svn/studies/lars/congestionNoise/config02.xml";
			runId = "congestionNoise";
			annualCostRate = (85.0/(1.95583))*(Math.pow(1.02, (2014-1995)));
		}
		
		CongestionNoisePricingControler main = new CongestionNoisePricingControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);

		if (runId.equalsIgnoreCase("congestion")) {
			
			log.info("Internalization of congestion cost is enabled.");
			
			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());		
			NoiseTollHandler noiseTollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, annualCostRate);
		
			ExtCostEventHandlerNoise extCostEventHandlerNoise = new ExtCostEventHandlerNoise(controler.getScenario(), false);
			
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCongestionPricingContolerListner( controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())  ));
			controler.addControlerListener(new NoiseInternalizationControlerListenerWithoutPricing((ScenarioImpl) controler.getScenario(), noiseTollHandler, spatialInfo, extCostEventHandlerNoise));
			
		} else if(runId.equalsIgnoreCase("noise")) {
			
			log.info("Internalization of noise cost is enabled.");

			EventsManager events = controler.getEvents();
			
			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
			
			NoiseTollHandler noiseTollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, annualCostRate);
			NoiseCostPricingHandler pricingHandler = new NoiseCostPricingHandler(events);
			
			NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(noiseTollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			
			ExtCostEventHandlerNoise extCostEventHandlerNoise = new ExtCostEventHandlerNoise(controler.getScenario(), false);
			
			controler.addControlerListener(new NoiseInternalizationControlerListener( (ScenarioImpl) controler.getScenario(), noiseTollHandler, pricingHandler, spatialInfo , extCostEventHandlerNoise));
			
		} else if(runId.equalsIgnoreCase("congestionNoise")) {
			
			log.info("Internalization of noise and congestion cost is enabled.");
			
			EventsManager events = controler.getEvents();
			
			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
			
			NoiseTollHandler noiseTollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, annualCostRate);
			NoiseCostPricingHandler pricingHandler = new NoiseCostPricingHandler(events);
			
			TollHandler congestionTollHandler = new TollHandler(controler.getScenario());

			CongestionNoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new CongestionNoiseTollDisutilityCalculatorFactory(congestionTollHandler, noiseTollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			
			ExtCostEventHandlerNoise extCostEventHandlerNoise = new ExtCostEventHandlerNoise(controler.getScenario(), false);
			
			controler.addControlerListener(new MarginalCongestionPricingContolerListner( controler.getScenario(), congestionTollHandler, new CongestionHandlerImplV3(events, (ScenarioImpl) controler.getScenario())  ));
			controler.addControlerListener(new NoiseInternalizationControlerListener( (ScenarioImpl) controler.getScenario(), noiseTollHandler, pricingHandler , spatialInfo , extCostEventHandlerNoise ));
			
		} else if(runId.equalsIgnoreCase("baseCase")) {
			log.info("Internalization of congestion/noise cost disabled.");
			
			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
			
			ExtCostEventHandlerNoise extCostEventHandlerNoise = new ExtCostEventHandlerNoise(controler.getScenario(), false);
			
			NoiseTollHandler noiseTollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, annualCostRate);
			
			controler.addControlerListener(new NoiseInternalizationControlerListenerWithoutPricing( (ScenarioImpl) controler.getScenario(), noiseTollHandler, spatialInfo, extCostEventHandlerNoise ));
		 
		} else {
			throw new RuntimeException("Run Id " + runId + " unknown. Aborting...");
		}
		
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
		
	}
}
	
