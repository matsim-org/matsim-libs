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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;


/**
 * @author lkroeger
 *
 */

public class NoiseInternalizationControlerListenerWithoutPricing implements AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseInternalizationControlerListenerWithoutPricing.class);
	
	private final ScenarioImpl scenario;
	private NoiseTollHandler tollHandler;
	private SpatialInfo spatialInfo;
	private ExtCostEventHandlerNoise extCostTripHandler;
	
	
	public NoiseInternalizationControlerListenerWithoutPricing (ScenarioImpl scenario, NoiseTollHandler tollHandler, SpatialInfo spatialInfo, ExtCostEventHandlerNoise extCostTripHandler) {
		this.scenario = scenario;
		this.tollHandler = tollHandler;
		this.spatialInfo = spatialInfo;
		this.extCostTripHandler = extCostTripHandler;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		// Initialization
		log.info("setActivityCoords...");
		spatialInfo.setActivityCoords();
		log.info("setReceiverPoints...");
		spatialInfo.setReceiverPoints();
		log.info("setActivityCoord2NearestReceiverPointId...");
		spatialInfo.setActivityCoord2NearestReceiverPointId();
		log.info("setDensityAndStreetWidth...");
		spatialInfo.setDensityAndStreetWidth();
//		spatialInfo.setInitialAssignment(); // necessary for a potential analysis at the end of the time-bin
		log.info("setRelevantLinkIds...");
		spatialInfo.setRelevantLinkIds();
		log.info("setHdvVehicles...");
		tollHandler.setHdvVehicles(); // If this classification would be done while setting the activityCoords, computational time could be saved (but it would be less modular for scenarios whithout agent heterogeneity)
		
		
		// adding the required handlers
		event.getControler().getEvents().addHandler(tollHandler);
		event.getControler().getEvents().addHandler(extCostTripHandler);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// calculate the final noise emissions per link per time interval (Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission)
		log.info("calculateFinalNoiseEmissions...");
		tollHandler.calculateFinalNoiseEmissions();
		// calculate the final noise immissions per receiver point per time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission)
		// for that save the final isolated immissions per link (Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission)
		log.info("calculateImmissionSharesPerReceiverPointPerTimeInterval...");
		tollHandler.calculateImmissionSharesPerReceiverPointPerTimeInterval();
		log.info("calculateFinalNoiseImmissions...");
		tollHandler.calculateFinalNoiseImmissions();
		// calculate damage per ReceiverPoint,
		// at first calculate the duration of stay for each agent at each receiver Point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// then calculate the damage (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)
		
		log.info("agent-based L_den calculation...");
		tollHandler.calculatePersonId2Lden();
		
		tollHandler.calculateCostSharesPerLinkPerTimeIntervalAgentBased();
		
		log.info("calculateDurationOfStay...");
		tollHandler.calculateDurationOfStay();
//		noiseHandler.calculateDurationOfStayOnlyHomeActivity();
		log.info("calculateDamagePerReceiverPoint...");
		tollHandler.calculateDamagePerReceiverPoint();
		
		// Only the next two commands should not be applied during the base case run
		// because the damage costs should be considered for the base case welfare calculation, too.
		// There is the difference between congestion (and partially accidents) on the one side and noise and emissions as real external effects on the other side
		
		// apply the formula for calculating the cost shares of the links,
		// use the saved data of the isolated immissions
		log.info("calculateCostSharesPerLinkPerTimeInterval...");
		tollHandler.calculateCostSharesPerLinkPerTimeInterval();
		log.info("calculateCostsPerVehiclePerLinkPerTimeInterval...");
		tollHandler.calculateCostsPerVehiclePerLinkPerTimeInterval();
		log.info("throwNoiseEvents...");
		tollHandler.throwNoiseEvents();
		log.info("throwNoiseEventsAffected...");
		tollHandler.throwNoiseEventsAffected();
		// here, the noiseEvents and personMoneyEvents are thrown
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
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
		tollHandler.writeTollStats(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameToll);
		tollHandler.writeTollStatsCar(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollCar);
		tollHandler.writeTollStatsHdv(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollHdv);
		
		log.info("Write toll stats per hour");
		String filenameTollPerHour = "tollstatsPerHour.csv";
		String filenameTollPerHourCar = "tollstatsPerHourCar.csv";
		String filenameTollPerHourHdv = "tollstatsPerHourHdv.csv";
		tollHandler.writeTollStatsPerHour(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollPerHour);
		tollHandler.writeTollStatsPerHourCar(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollPerHourCar);
		tollHandler.writeTollStatsPerHourHdv(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollPerHourHdv);
		
		log.info("Write toll stats per activity");
		String filenameTollPerActivity = "tollstatsPerActivity.csv";
		tollHandler.writeTollStatsPerActivity(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollPerActivity);
		
		log.info("Write toll stats for comparing home-based vs. activity-based");
		String filenameTollCompareHomeVsActivityBased = "tollstatsCompareHomeVsActivityBased.csv";
		tollHandler.writeTollStatsCompareHomeVsActivityBased(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameTollCompareHomeVsActivityBased);
		
		log.info("Write noise emission stats");
		String filenameNoiseEmission = "noiseEmissionStats.csv";
		tollHandler.writeNoiseEmissionStats(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameNoiseEmission);
		
		log.info("Write noise immission stats");
		String filenameNoiseImmission = "noiseImmissionStats.csv";
		
		tollHandler.writeNoiseImmissionStats(scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/"+filenameNoiseImmission);

		TripInfoWriterNoise writer = new TripInfoWriterNoise(extCostTripHandler, scenario.getConfig().controler().getOutputDirectory()+"/postAnalysis_it."+event.getIteration()+"/personId2sum/");
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
