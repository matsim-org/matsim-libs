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

public class NoiseInternalizationControlerListener implements AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseInternalizationControlerListener.class);
	
	private final ScenarioImpl scenario;
	private NoiseTollHandler tollHandler;
	private NoiseHandler noiseHandler;
	private SpatialInfo spatialInfo;
	
	public NoiseInternalizationControlerListener (ScenarioImpl scenario, NoiseTollHandler tollHandler, NoiseHandler noiseHandler, SpatialInfo spatialInfo) {
		this.scenario = scenario;
		this.tollHandler = tollHandler;
		this.noiseHandler = noiseHandler;
		this.spatialInfo = spatialInfo;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		// Initialization
		spatialInfo.setReceiverPoints();
		spatialInfo.setActivityCoords();
		spatialInfo.setActivityCoord2NearestReceiverPointId();
		spatialInfo.setInitialAssignment(); // remove?
		spatialInfo.setRelevantLinkIds();
		
		// adding the required handlers
		event.getControler().getEvents().addHandler(tollHandler);
		event.getControler().getEvents().addHandler(noiseHandler);		
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// calculate the final noise emissions per link per time interval (Map<Id,Map<Double,Double>> linkId2timeInterval2noiseEmission)
		noiseHandler.calculateFinalNoiseEmissions();
		// calculate the final noise immissions per receiver point per time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission)
		// for that save the final isolated immissions per link (Map<Id,Map<Double,Map<Id,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission)
		noiseHandler.calculateImmissionSharesPerReceiverPointPerTimeInterval();
		noiseHandler.calculateFinalNoiseImmissions();
		// calculate damage per ReceiverPoint,
		// at first calculate the duration of stay for each agent at each receiver Point and sum up for each time interval (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits)
		// then calculate the damage (Map<Id,Map<Double,Double>> receiverPointId2timeInterval2damageCost)
		noiseHandler.calculateDurationOfStay();
//		noiseHandler.calculateDurationOfStayOnlyHomeActivity();
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
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		log.info("Set average tolls for each link Id and time bin.");
		tollHandler.setLinkId2timeBin2avgToll();
		
		log.info("Write toll stats");
		String filenameToll = "tollstats.csv";
		tollHandler.writeTollStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameToll);
		
		log.info("Write toll stats per hour");
		String filenameTollPerHour = "tollstatsPerHour.csv";
		tollHandler.writeTollStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerHour);
		
//		log.info("Write toll stats per activity");
//		String filenameTollPerActivity = "tollstatsPerActivity.csv";
//		tollHandler.writeTollStatsPerActivity(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollPerActivity);
//		
//		log.info("Write toll stats for comparing home-based vs. activity-based");
//		String filenameTollCompareHomeVsActivityBased = "tollstats_CompareHomeVsActivityBased.csv";
//		tollHandler.writeTollStatsCompareHomeVsActivityBased(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameTollCompareHomeVsActivityBased);
		
		log.info("Write noise emission stats");
		String filenameNoiseEmission = "noiseEmissionStats.csv";
		noiseHandler.writeNoiseEmissionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseEmission);
		
		log.info("Write noise emission stats per hour");
		String filenameNoiseEmissionPerHour = "noiseEmissionStatsPerHour.csv";
		noiseHandler.writeNoiseEmissionStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseEmissionPerHour);
		
		log.info("Write noise immission stats");
		String filenameNoiseImmission = "noiseImmissionStats.csv";
		noiseHandler.writeNoiseImmissionStats(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseImmission);
		
//		log.info("Write noise immission stats per hour");
//		String filenameNoiseImmissionPerHour = "noiseImmissionStatsPerHour.csv";
//		noiseHandler.writeNoiseImmissionStatsPerHour(this.scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + filenameNoiseImmissionPerHour);
	}

	

}
