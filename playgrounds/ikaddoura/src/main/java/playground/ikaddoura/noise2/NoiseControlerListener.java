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
package playground.ikaddoura.noise2;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseControlerListener implements AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseControlerListener.class);

	// ##############################################################################
	
	final double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));

	// ##############################################################################

	private NoiseSpatialInfo spatialInfo;
	private NoiseEmissionHandler noiseEmissionHandler;
	private PersonActivityHandler personActivityTracker;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("Initialization...");
		
		this.spatialInfo = new NoiseSpatialInfo(event.getControler().getScenario());
		this.spatialInfo.setActivityCoords();
		this.spatialInfo.setReceiverPoints();
		this.spatialInfo.setActivityCoord2NearestReceiverPointId();
		this.spatialInfo.setRelevantLinkIds();
		
		this.noiseEmissionHandler = new NoiseEmissionHandler(event.getControler().getScenario());
		this.noiseEmissionHandler.setHdvVehicles(null);
		
		this.personActivityTracker = new PersonActivityHandler(event.getControler().getScenario(), this.spatialInfo);

		log.info("Initialization... Done.");
		
		event.getControler().getEvents().addHandler(noiseEmissionHandler);
		event.getControler().getEvents().addHandler(personActivityTracker);
		
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		log.info("Calculating noise emissions and immissions...");
		
		// calculate noise emission for each link and time interval
		this.noiseEmissionHandler.calculateNoiseEmission();
		this.noiseEmissionHandler.writeNoiseEmissionStats(event.getControler().getConfig().controler().getOutputDirectory() + "/it." + event.getIteration() + ".analysis_emissionStats.csv");
		this.noiseEmissionHandler.writeNoiseEmissionStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory() + "/it." + event.getIteration() + ".analysis_emissionStatsPerHour.csv");
		// TODO: test
//		Map<Id, Map<Double, Double>> linkId2timeInterval2noiseEmission = this.noiseEmissionHandler.getLinkId2timeInterval2noiseEmission();
		
		// calculate activity durations for each agent
		this.personActivityTracker.calculateDurationOfStay();
		// TODO: test
		
		NoiseImmission noiseImmission = new NoiseImmission(event.getControler().getScenario(), event.getControler().getEvents(), this.spatialInfo, this.annualCostRate, this.noiseEmissionHandler, this.personActivityTracker);
		noiseImmission.setTunnelLinks(null);
		noiseImmission.setNoiseBarrierLinks(null);
		// calculate the noise immission for each receiver point and time interval and save for each receiver point and for each time interval the contribution (immission) of each link's noise level
		log.info("calculating noise immission...");
		noiseImmission.calculateNoiseImmission();
		
		log.info("Calculating noise emissions and immissions... Done.");
		
		// test
//		Map<Id,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission = noiseImmissionHandler.getReceiverPointId2timeInterval2noiseImmission();
//		Map<Id,Map<Double,Map<Id,Double>>> receiverPointId2timeIntervals2noiseLinks2isolatedImmission = noiseImmissionHandler.getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission();
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
//		log.info("Set average tolls for each link Id and time bin.");
//		noiseImmissionHandler.setLinkId2timeBin2avgToll();
//		noiseImmissionHandler.setLinkId2timeBin2avgTollCar();
//		noiseImmissionHandler.setLinkId2timeBin2avgTollHdv();
//		
//		log.info("total toll (second approach L_den)"+(noiseImmissionHandler.getTotalTollAffectedAgentBasedCalculation()));
//		log.info("control value: "+(noiseImmissionHandler.getTotalTollAffectedAgentBasedCalculationControl()));
//		log.info("total toll (first approach): "+(noiseImmissionHandler.getTotalToll()));
//		log.info("total toll affected (first approach): "+(noiseImmissionHandler.getTotalTollAffected()));
//		
//		log.info("Write toll stats");
//		String filenameToll = "noise_tollstats.csv";
//		String filenameTollCar = "noise_tollstatsCar.csv";
//		String filenameTollHdv = "noise_tollstatsHdv.csv";
//		noiseImmissionHandler.writeTollStats(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameToll);
//		noiseImmissionHandler.writeTollStatsCar(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollCar);
//		noiseImmissionHandler.writeTollStatsHdv(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollHdv);
//		
//		log.info("Write toll stats per hour");
//		String filenameTollPerHour = "tollstatsPerHour.csv";
//		String filenameTollPerHourCar = "tollstatsPerHourCar.csv";
//		String filenameTollPerHourHdv = "tollstatsPerHourHdv.csv";
//		noiseImmissionHandler.writeTollStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollPerHour);
//		noiseImmissionHandler.writeTollStatsPerHourCar(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollPerHourCar);
//		noiseImmissionHandler.writeTollStatsPerHourHdv(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollPerHourHdv);
//		
//		log.info("Write toll stats per activity");
//		String filenameTollPerActivity = "tollstatsPerActivity.csv";
//		noiseImmissionHandler.writeTollStatsPerActivity(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollPerActivity);
//		
//		log.info("Write toll stats for comparing home-based vs. activity-based");
//		String filenameTollCompareHomeVsActivityBased = "tollstatsCompareHomeVsActivityBased.csv";
//		noiseImmissionHandler.writeTollStatsCompareHomeVsActivityBased(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameTollCompareHomeVsActivityBased);
//		
//		log.info("Write noise emission stats");
//		String filenameNoiseEmission = "noiseEmissionStats.csv";
//		noiseImmissionHandler.writeNoiseEmissionStats(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameNoiseEmission);
//		
//		log.info("Write noise immission stats");
//		String filenameNoiseImmission = "noiseImmissionStats.csv";
//		
//		noiseImmissionHandler.writeNoiseImmissionStats(event.getControler().getConfig().controler().getOutputDirectory()+"/it."+event.getIteration()+"."+filenameNoiseImmission);

	}
}
