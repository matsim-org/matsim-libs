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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class NoiseInternalizationControlerPostAnalysis {
	
	private static final Logger log = Logger.getLogger(NoiseInternalizationControlerPostAnalysis.class);
	
	static String configFile = null;
	
	String outputFolder = "/Users/Lars/workspace2/baseCaseCtd_8_250/output/x2";

	public static void main(String[] args) throws IOException {
//		configFile = "/Users/Lars/Desktop/VERSUCH/Berlin/Berlin_config.xml";
		configFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/config2.xml";
		
		NoiseInternalizationControlerPostAnalysis noiseInternalizationControlerPostAnalysis = new NoiseInternalizationControlerPostAnalysis();
		
//		noiseInternalizationControler.runBaseCase(configFile);
		noiseInternalizationControlerPostAnalysis.runPostCalculation(configFile);
	}

	private void runPostCalculation(String configFile) {
//		String eventsFile = "/Users/Lars/Desktop/VERSUCH/Berlin/100.events.xml.gz";
		String eventsFile = "/Users/Lars/workspace2/baseCaseCtd_8_250/ITERS/it.250/250.events.xml.gz";
		
		Config config = ConfigUtils.loadConfig(configFile);
		
//		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager events = EventsUtils.createEventsManager();
		
		SpatialInfo spatialInfo = new SpatialInfo(scenario);
		NoiseTollHandler tollHandler = new NoiseTollHandler(scenario, events, spatialInfo, 0);
		
		log.info("setActivityCoords...");
		spatialInfo.setActivityCoords();
		log.info("setReceiverPoints...");
		spatialInfo.setReceiverPoints();
		log.info("setActivityCoord2NearestReceiverPointId...");
		spatialInfo.setActivityCoord2NearestReceiverPointId();
		log.info("setDensityAndStreetWidth...");
		spatialInfo.setDensityAndStreetWidth();
		log.info("setRelevantLinkIds...");
//		spatialInfo.setInitialAssignment(); // necessary for a potential analysis at the end of the time-bin
		spatialInfo.setRelevantLinkIds();
		log.info("setHdvVehicles...");
		tollHandler.setHdvVehicles(); // If this classification would be done while setting the activityCoords, computational time could be saved (but it would be less modular for scenarios whithout agent heterogeneity)
		
		events.addHandler(tollHandler);
		
		ExtCostEventHandlerNoise extCostTripHandler = new ExtCostEventHandlerNoise(scenario, false);
		events.addHandler(extCostTripHandler);
		
//		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
//		events.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
		
// 		adapt the WelfareAnalysisControlerListener for the noise damage
//		events.addHandler(new WelfareAnalysisControlerListener((ScenarioImpl) scenario));
		log.info("Read events file ...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		log.info((tollHandler.getInterval2departures()));
		
		log.info("calculateFinalNoiseEmissions...");
		// calculate the final noise emissions per link per time interval (Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission)
		tollHandler.calculateFinalNoiseEmissions();
		log.info("calculateImmissionSharesPerReceiverPointPerTimeInterval...");
		// calculate the final noise immissions per receiver point per time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission)
		// for that save the final isolated immissions per link (Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission)
		tollHandler.calculateImmissionSharesPerReceiverPointPerTimeInterval();
		log.info("calculateFinalNoiseImmissions...");
		tollHandler.calculateFinalNoiseImmissions();

		log.info("agent-based L_den calculation...");
		tollHandler.calculatePersonId2Lden();
		
//+++++++++++++++++++++++		
		
//		log.info("calculateDurationOfStay...");
		// calculate damage per ReceiverPoint,
		// at first calculate the duration of stay for each agent at each receiver Point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// then calculate the damage (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)
//		tollHandler.calculateDurationOfStay();
//		log.info("calculateDamagePerReceiverPoint...");
//		noiseHandler.calculateDurationOfStayOnlyHomeActivity();
//		tollHandler.calculateDamagePerReceiverPoint();
//		log.info("calculateCostSharesPerLinkPerTimeInterval...");
				
		// Only the next two commands should not be applied during the base case run
		// because the damage costs should be considered for the base case welfare calculation, too.
		// There is the difference between congestion (and partially accidents) on the one side and noise and emissions as real external effects on the other side
				
		// apply the formula for calculating the cost shares of the links,
		// use the saved data of the isolated immissions
		tollHandler.calculateCostSharesPerLinkPerTimeIntervalAgentBased();
		
		log.info("total toll (second approach L_den)"+(tollHandler.getTotalTollAffectedAgentBasedCalculation()));
		log.info("control value: "+(tollHandler.getTotalTollAffectedAgentBasedCalculationControl()));
		log.info("total toll (first approach): "+(tollHandler.getTotalToll()));
		log.info("total toll affected (first approach): "+(tollHandler.getTotalTollAffected()));

//++++++++++++++++++++++++++		
		
		log.info("calculateDurationOfStay...");
		// at first calculate the duration of stay for each agent at each receiver Point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// then calculate the damage (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)
		tollHandler.calculateDurationOfStay();
		// calculate damage per ReceiverPoint,
		log.info("calculateDamagePerReceiverPoint...");
//		noiseHandler.calculateDurationOfStayOnlyHomeActivity();
		tollHandler.calculateDamagePerReceiverPoint();
		log.info("calculateCostSharesPerLinkPerTimeInterval...");
				
//+++++++++++++++++++++++++++		
		
		// Only the next two commands should not be applied during the base case run
		// because the damage costs should be considered for the base case welfare calculation, too.
		// There is the difference between congestion (and partially accidents) on the one side and noise and emissions as real external effects on the other side
				
		// apply the formula for calculating the cost shares of the links,
		// use the saved data of the isolated immissions
//		tollHandler.calculateCostSharesPerLinkPerTimeInterval();
		log.info("calculateCostsPerVehiclePerLinkPerTimeInterval...");
		tollHandler.calculateCostsPerVehiclePerLinkPerTimeInterval();
		log.info("throwNoiseEvents...");
		tollHandler.throwNoiseEvents();
		log.info("throwNoiseEventsAffected...");
		tollHandler.throwNoiseEventsAffected();
		// here, the noiseEvents and personMoneyEvents are thrown
		
		log.info("Set average tolls for each link Id and time bin.");
		tollHandler.setLinkId2timeBin2avgToll();
		tollHandler.setLinkId2timeBin2avgTollCar();
		tollHandler.setLinkId2timeBin2avgTollHdv();
		
		log.info("total toll (second approach L_den)"+(tollHandler.getTotalTollAffectedAgentBasedCalculation()));
		log.info("control value: "+(tollHandler.getTotalTollAffectedAgentBasedCalculationControl()));
		log.info("total toll (first approach): "+(tollHandler.getTotalToll()));
		log.info("total toll affected (first approach): "+(tollHandler.getTotalTollAffected()));
		
		log.info("Write toll stats");
		String filenameToll = "noise_tollstats.csv";
		String filenameTollCar = "noise_tollstatsCar.csv";
		String filenameTollHdv = "noise_tollstatsHdv.csv";
		tollHandler.writeTollStats(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameToll);
		tollHandler.writeTollStatsCar(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollCar);
		tollHandler.writeTollStatsHdv(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollHdv);
		
		log.info("Write toll stats per hour");
		String filenameTollPerHour = "tollstatsPerHour.csv";
		String filenameTollPerHourCar = "tollstatsPerHourCar.csv";
		String filenameTollPerHourHdv = "tollstatsPerHourHdv.csv";
		tollHandler.writeTollStatsPerHour(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollPerHour);
		tollHandler.writeTollStatsPerHourCar(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollPerHourCar);
		tollHandler.writeTollStatsPerHourHdv(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollPerHourHdv);
		
		log.info("Write toll stats per activity");
		String filenameTollPerActivity = "tollstatsPerActivity.csv";
		tollHandler.writeTollStatsPerActivity(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollPerActivity);
		
		log.info("Write toll stats for comparing home-based vs. activity-based");
		String filenameTollCompareHomeVsActivityBased = "tollstatsCompareHomeVsActivityBased.csv";
		tollHandler.writeTollStatsCompareHomeVsActivityBased(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameTollCompareHomeVsActivityBased);
		
		log.info("Write noise emission stats");
		String filenameNoiseEmission = "noiseEmissionStats.csv";
		tollHandler.writeNoiseEmissionStats(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameNoiseEmission);
		
		log.info("Write noise immission stats");
		String filenameNoiseImmission = "noiseImmissionStats.csv";
		
		tollHandler.writeNoiseImmissionStats(config.controler().getOutputDirectory()+"/postAnalysis/"+filenameNoiseImmission);

		TripInfoWriterNoise writer = new TripInfoWriterNoise(extCostTripHandler, config.controler().getOutputDirectory()+"/postAnalysis/personId2sum");
		writer.writeDetailedResults(TransportMode.car);
		writer.writeAvgTollPerDistance(TransportMode.car);
		writer.writeAvgTollPerTimeBin(TransportMode.car);
//		writer.writeDetailedResults(TransportMode.pt);
//		writer.writeAvgTollPerDistance(TransportMode.pt);
//		writer.writeAvgTollPerTimeBin(TransportMode.pt);
		writer.writePersonId2totalAmount();
		writer.writePersonId2totalAmountAffected();
	}
}
