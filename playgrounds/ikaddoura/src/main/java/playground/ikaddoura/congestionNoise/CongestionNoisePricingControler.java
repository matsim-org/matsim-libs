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
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.internalizationCar.MarginalCostPricing;
import playground.ikaddoura.internalizationCar.TollDisutilityCalculatorFactory;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;
import playground.ikaddoura.noise.NoiseHandler;
import playground.ikaddoura.noise.NoiseInternalizationControlerListener;
import playground.ikaddoura.noise.NoiseTollDisutilityCalculatorFactory;
import playground.ikaddoura.noise.NoiseTollHandler;
import playground.ikaddoura.noise.SpatialInfo;

/**
 * @author ikaddoura
 *
 */
public class CongestionNoisePricingControler {
	
	private static final Logger log = Logger.getLogger(CongestionNoisePricingControler.class);
	
	static String configFile;
	static String runId; // congestion, noise, baseCase, congestionNoise
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("first argument (config file): "+ configFile);
			
			runId = args[1];
			log.info("second argument (rund Id): "+ runId);

		} else {
			configFile = "../../shared-svn/studies/lars/congestionNoise/config02.xml";
			runId = "noise";
		}
		
		CongestionNoisePricingControler main = new CongestionNoisePricingControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);

		if (runId.equalsIgnoreCase("congestion")) {
			
			log.info("Internalization of congestion cost is enabled.");
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCostPricing( (ScenarioImpl) controler.getScenario(), tollHandler ));
		
		} else if(runId.equalsIgnoreCase("noise")) {
			
			log.info("Internalization of noise cost is enabled.");

			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
			
			NoiseHandler noiseHandler = new NoiseHandler(controler.getScenario(), spatialInfo);
			NoiseTollHandler tollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, noiseHandler);
			
			NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			
			controler.addControlerListener(new NoiseInternalizationControlerListener( (ScenarioImpl) controler.getScenario(), tollHandler, noiseHandler, spatialInfo ));
			
		} else if(runId.equalsIgnoreCase("congestionNoise")) {
			
			log.info("Internalization of noise and congestion cost is enabled.");
			
			SpatialInfo spatialInfo = new SpatialInfo( (ScenarioImpl) controler.getScenario());
			NoiseHandler noiseHandler = new NoiseHandler(controler.getScenario(), spatialInfo);
			NoiseTollHandler noiseTollHandler = new NoiseTollHandler(controler.getScenario(), controler.getEvents(), spatialInfo, noiseHandler);

			TollHandler congestionTollHandler = new TollHandler(controler.getScenario());

			CongestionNoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new CongestionNoiseTollDisutilityCalculatorFactory(congestionTollHandler, noiseTollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			
			controler.addControlerListener(new MarginalCostPricing( (ScenarioImpl) controler.getScenario(), congestionTollHandler ));
			controler.addControlerListener(new NoiseInternalizationControlerListener( (ScenarioImpl) controler.getScenario(), noiseTollHandler, noiseHandler, spatialInfo ));
			
		} else if(runId.equalsIgnoreCase("baseCase")) {
			log.info("Internalization of congestion/noise cost disabled.");
		 
		} else {
			throw new RuntimeException("Run Id " + runId + " unknown. Aborting...");
		}
		
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
		
	}
}
	
