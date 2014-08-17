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
	
	public NoiseInternalizationControlerListenerWithoutPricing (ScenarioImpl scenario, NoiseTollHandler tollHandler, SpatialInfo spatialInfo) {
		this.scenario = scenario;
		this.tollHandler = tollHandler;
		this.spatialInfo = spatialInfo;
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
		
//		log.info("Set average tolls for each link Id and time bin.");
//		tollHandler.setLinkId2timeBin2avgToll();
//		tollHandler.setLinkId2timeBin2avgTollCar();
//		tollHandler.setLinkId2timeBin2avgTollHdv();
		
		log.info("Write toll stats");
		String filenameToll = "noise_tollstats.csv";
		String filenameTollCar = "noise_tollstatsCar.csv";
		String filenameTollHdv = "noise_tollstatsHdv.csv";
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameToll);
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollCar);
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollHdv);
		
		tollHandler.setIteration2TollSum(event.getIteration());
		tollHandler.setIteration2TollSumCar(event.getIteration());
		tollHandler.setIteration2TollSumHdv(event.getIteration());
		log.info("Write damageSum stats (iteration2damageSum)");
		String filenameIteration2amageSum = "iteration2damageSum.csv";
		tollHandler.writeTollStatsIteration2tollSum(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameIteration2amageSum);

//		log.info("Write toll stats per hour");
//		String filenameTollPerHour = "noise_tollstatsPerHour.csv";
//		String filenameTollPerHourCar = "noise_tollstatsPerHourCar.csv";
//		String filenameTollPerHourHdv = "noise_tollstatsPerHourHdv.csv";
//		tollHandler.writeTollStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerHour);
//		tollHandler.writeTollStatsPerHourCar(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerHourCar);
//		tollHandler.writeTollStatsPerHourHdv(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerHourHdv);
		
//		log.info("Write toll stats per activity");
//		String filenameTollPerActivity = "noise_tollstatsPerActivity.csv";
//		tollHandler.writeTollStatsPerActivity(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerActivity);
//		
//		log.info("Write toll stats for comparing home-based vs. activity-based");
//		String filenameTollCompareHomeVsActivityBased = "noise_tollstats_CompareHomeVsActivityBased.csv";
//		tollHandler.writeTollStatsCompareHomeVsActivityBased(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollCompareHomeVsActivityBased);
		
//		log.info("Write noise emission stats");
//		String filenameNoiseEmission = "noiseEmissionStats.csv";
//		noiseHandler.writeNoiseEmissionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseEmission);
//		
//		log.info("Write noise emission stats per hour");
//		String filenameNoiseEmissionPerHour = "noiseEmissionStatsPerHour.csv";
//		noiseHandler.writeNoiseEmissionStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseEmissionPerHour);
//		
//		log.info("Write noise immission stats");
//		String filenameNoiseImmission = "noiseImmissionStats.csv";
//		noiseHandler.writeNoiseImmissionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseImmission);
		
//		log.info("Write noise immission stats per hour");
//		String filenameNoiseImmissionPerHour = "noiseImmissionStatsPerHour.csv";
//		noiseHandler.writeNoiseImmissionStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseImmissionPerHour);
	}
}
