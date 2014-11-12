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

public class NoiseCalculationOnline implements AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	private NoiseParameters noiseParameters;
	private NoiseSpatialInfo spatialInfo;
	private NoiseEmissionHandler noiseEmissionHandler;
	private NoiseImmissionCalculation noiseImmission;
	private PersonActivityHandler personActivityTracker;
	private NoiseDamageCalculation noiseDamageCosts;
	
	public NoiseCalculationOnline(NoiseParameters noiseParameters) {
		this.noiseParameters = noiseParameters;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("Initialization...");
		
		this.spatialInfo = new NoiseSpatialInfo(event.getControler().getScenario(), noiseParameters);
		this.spatialInfo.setActivityCoords();
		this.spatialInfo.setReceiverPoints();
		this.spatialInfo.setActivityCoord2NearestReceiverPointId();
		this.spatialInfo.setRelevantLinkIds();
		this.spatialInfo.writeReceiverPoints(event.getControler().getConfig().controler().getOutputDirectory() + "/receiverPoints/");
		
		this.noiseEmissionHandler = new NoiseEmissionHandler(event.getControler().getScenario(), noiseParameters);
		this.noiseEmissionHandler.setHdvVehicles(null);
		
		this.personActivityTracker = new PersonActivityHandler(event.getControler().getScenario(), this.spatialInfo, noiseParameters);

		log.info("Initialization... Done.");
		
		event.getControler().getEvents().addHandler(noiseEmissionHandler);
		event.getControler().getEvents().addHandler(personActivityTracker);
		
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		// calculate noise emission for each link and time interval
		log.info("Calculating noise emission...");
		this.noiseEmissionHandler.calculateNoiseEmission();
		this.noiseEmissionHandler.writeNoiseEmissionStats(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStats.csv");
		this.noiseEmissionHandler.writeNoiseEmissionStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStatsPerHour.csv");
		log.info("Calculating noise emission... Done.");
		
		// calculate the noise immission for each receiver point and time interval
		log.info("Calculating noise immission...");
		this.noiseImmission = new NoiseImmissionCalculation(this.spatialInfo, this.noiseEmissionHandler, noiseParameters);
		noiseImmission.setTunnelLinks(null);
		noiseImmission.setNoiseBarrierLinks(null);
		noiseImmission.calculateNoiseImmission();
		this.noiseImmission.writeNoiseImmissionStats(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStats.csv");
		this.noiseImmission.writeNoiseImmissionStatsPerHour(event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStatsPerHour.csv");
		log.info("Calculating noise immission... Done.");
		
		// calculate activity durations for each agent
		log.info("Calculating each agent's activity durations...");
		this.personActivityTracker.calculateDurationOfStay();
		log.info("Calculating each agent's activity durations... Done.");
				
		log.info("Calculating noise damage costs and throwing noise events...");
		this.noiseDamageCosts = new NoiseDamageCalculation(event.getControler().getScenario(), event.getControler().getEvents(), spatialInfo, noiseParameters, noiseEmissionHandler, personActivityTracker, noiseImmission);
		this.noiseDamageCosts.calculateNoiseDamageCosts();
		log.info("Calculating noise damage costs and throwing noise events... Done.");
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
//		log.info("Set average tolls for each link Id and time bin.");
//		noiseImmissionHandler.setLinkId2timeBin2avgToll();
//		noiseImmissionHandler.setLinkId2timeBin2avgTollCar();
//		noiseImmissionHandler.setLinkId2timeBin2avgTollHdv();
		
		log.info("Total Caused Noise Cost: " + noiseDamageCosts.getTotalCausedNoiseCost());
		log.info("Total Affected Noise Cost: " + noiseDamageCosts.getTotalAffectedNoiseCost());

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
	
	// for testing purposes
	
	public NoiseEmissionHandler getNoiseEmissionHandler() {
		return noiseEmissionHandler;
	}

	public PersonActivityHandler getPersonActivityTracker() {
		return personActivityTracker;
	}

	public NoiseImmissionCalculation getNoiseImmission() {
		return noiseImmission;
	}

	public NoiseDamageCalculation getNoiseDamageCosts() {
		return noiseDamageCosts;
	}

	public NoiseSpatialInfo getSpatialInfo() {
		return spatialInfo;
	}
		
}
