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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class NoiseInternalizationControlerPostAnalysis {
	
	private static final Logger log = Logger.getLogger(NoiseInternalizationControler.class);
	
	static String configFile = null;

	public static void main(String[] args) throws IOException {
		
//		configFile = "/Users/Lars/Desktop/NoiseInternalization20/SiouxFalls/input/config.xml";
		configFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/config2.xml";
//		configFile = "C:/MA_Noise/Zwischenpraesentation/Testszenarien/input/config01Day.xml";
		
		NoiseInternalizationControlerPostAnalysis noiseInternalizationControlerPostAnalysis = new NoiseInternalizationControlerPostAnalysis();
		
//		noiseInternalizationControler.runBaseCase(configFile);
		noiseInternalizationControlerPostAnalysis.runPostCalculation(configFile);
	}

	private void runPostCalculation(String configFile) {
		String eventsFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/ITERS/it.250/250.events.xml.gz";
//		String eventsFile = "C:/MA_Noise/Zwischenpraesentation/Testszenarien/output/testSzenario01Day/ITERS/it.60/60.events.xml.gz";
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		NoiseHandler noiseHandler = new NoiseHandler(scenario);
		NoiseTollHandler tollHandler = new NoiseTollHandler(scenario, events);
		
		GetNearestReceiverPoint.getReceiverPoints(scenario);
		GetActivityCoords.getActivityCoords(scenario);
		GetNearestReceiverPoint.getReceiverPoints(scenario);
		GetActivityCoords.getActivityCoord2NearestReceiverPointId(scenario);
		GetActivityCoords.getInitialAssignment(scenario);
//		GetNearestReceiverPoint.getReceiverPoints(scenario);
		GetNearestReceiverPoint.getRelevantLinkIds(scenario);
		//TODO:!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		GetActivityCoords.getActivityCoord2NearestReceiverPointId(scenario);
		
		events.addHandler(noiseHandler);
		
		events.addHandler(tollHandler);
		
//		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
//		events.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
		
// 		adapt the WelfareAnalysisControlerListener for the noise damage
//		events.addHandler(new WelfareAnalysisControlerListener((ScenarioImpl) scenario));
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		// calculate the final noise emissions per link per time interval (Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission)
		noiseHandler.calculateFinalNoiseEmissions();
		// calculate the final noise immissions per receiver point per time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission)
		// for that save the final isolated immissions per link (Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission)
		noiseHandler.calculateImmissionSharesPerReceiverPointPerTimeInterval();
		noiseHandler.calculateFinalNoiseImmissions();
		// calculate damage per ReceiverPoint,
		// at first calculate the duration of stay for each agent at each receiver Point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// then calculate the damage (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)
//		noiseHandler.calculateDurationOfStay();
		noiseHandler.calculateDurationOfStayOnlyHomeActivity();
		noiseHandler.calculateDamagePerReceiverPoint();
				
		// Only the next two commands should not be applied during the base case run
		// because the damage costs should be considered for the base case welfare calculation, too.
		// There is the difference between congestion (and partially accidents) on the one side and noise and emissions as real external effects on the other side
				
		// apply the formula for calculating the cost shares of the links,
		// use the saved data of the isolated immissions
		tollHandler.calculateCostSharesPerLinkPerTimeInterval();
		tollHandler.calculateCostsPerVehiclePerLinkPerTimeInterval();
		tollHandler.throwNoiseEvents();
		tollHandler.throwNoiseEventsAffected();
		// here, the noiseEvents and personMoneyEvents are thrown

		log.info("Set average tolls for each link Id and time bin.");
		tollHandler.setLinkId2timeBin2avgToll();
		
		log.info("Write toll stats");
		String filenameToll = "tollstats.csv";
		tollHandler.writeTollStats("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameToll);
		
		log.info("Write toll stats per hour");
		String filenameTollPerHour = "tollstatsPerHour.csv";
		tollHandler.writeTollStatsPerHour("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameTollPerHour);
		
		log.info("Write toll stats per activity");
		String filenameTollPerActivity = "tollstatsPerActivity.csv";
		tollHandler.writeTollStatsPerActivity("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameTollPerActivity);
		
		log.info("Write toll stats for comparing home-based vs. activity-based");
		String filenameTollCompareHomeVsActivityBased = "tollstatsCompareHomeVsActivityBased.csv";
		tollHandler.writeTollStatsCompareHomeVsActivityBased("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameTollCompareHomeVsActivityBased);
		
		log.info("Write noise emission stats");
		String filenameNoiseEmission = "noiseEmissionStats.csv";
		noiseHandler.writeNoiseEmissionStats("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameNoiseEmission);
		
		log.info("Write noise immission stats");
		String filenameNoiseImmission = "noiseImmissionStats.csv";
		noiseHandler.writeNoiseImmissionStats("/Users/Lars/Desktop/ShapeFiles/CsvOutput_test/"+filenameNoiseImmission);
	}
	
}
