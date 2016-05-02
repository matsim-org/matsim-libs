/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.spatialGrid;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseWriter;
import org.matsim.contrib.noise.data.Grid;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.contrib.noise.utils.NoiseEventAnalysisHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * Computes the user benefits and caused noise costs based on the plans file and the events.
 * Maps the results to the home location of the causing agent. 
 * 
 * @author ikaddoura
 *
 */
public class SpatialGridAnalysis {
	private static final Logger log = Logger.getLogger(SpatialGridAnalysis.class);
	
	
	// yyyy pliease try to avoid static variables except if they refer to quantities that are truly constant throughout the whole world for this
	// version of the code.
	// (They don't do much damage here, but imagine you are calling this writer from two
	// places simultaneously, and expect it to write to two different locations.)  kai, jan'16
	
	private static String runDirectory;
	private static double receiverPointGap;
	private static String homeActivityType;
	private static String outputFilePath;
	
	// if the following variable is null the standard events file is used
	private static String eventsFileWithNoiseEvents;
	
	// if the following variable is true, the events have to contain noise events.
	// if the following variable is false, money events are analyzed and assumed to correspond to the caused noise costs
	private static boolean useNoiseEvents;
	
	private boolean useCompression = false ;
		
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
			
			receiverPointGap = Double.valueOf(args[1]);		
			log.info("receiver point gap: " + runDirectory);
			
			homeActivityType = args[2];		
			log.info("home activity: " + homeActivityType);
			
			eventsFileWithNoiseEvents = args[3];		
			log.info("events file with noise events: " + eventsFileWithNoiseEvents);
			
			outputFilePath = args[4];		
			log.info("analysis output path: " + outputFilePath);
			
			useNoiseEvents = Boolean.valueOf(args[5]);		
			log.info("use noise events: " + outputFilePath);
			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/int_1_averageCost/";
//			eventsFileWithNoiseEvents = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/baseCase_2_noiseAnalysis/r31341/noiseAnalysis_BlnBC2_2/analysis_it.100/100.events_NoiseImmission_Offline.xml.gz";
			eventsFileWithNoiseEvents = null;
			outputFilePath = runDirectory + "analysis_spatial_grid_2/";
			
			receiverPointGap = 100.;
			homeActivityType = "home";
			useNoiseEvents = true;
		}
		
		SpatialGridAnalysis analysis = new SpatialGridAnalysis();
		analysis.run();		
	}

	private void run() {
		
		HashMap<Id<ReceiverPoint>, Double> rp2totalUserBenefit = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2homeLocations = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2totalCausedNoiseCost = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2totalAffectedNoiseCost = new HashMap<>();
			
		// scenario
		
//		String configFile = runDirectory + "output_config.xml.gz";
		String populationFile = runDirectory + "output_plans.xml.gz";
		String networkFile = runDirectory + "output_network.xml.gz";
	
//		Config config = ConfigUtils.loadConfig(configFile);		
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		NoiseConfigGroup gridParameters = (NoiseConfigGroup) config.getModule("noise");
		gridParameters.setReceiverPointGap(receiverPointGap);
					
//		// Berlin Coordinates: Area around the city center of Berlin (Tiergarten)
//		double xMin = 4590855.;
//		double yMin = 5819679.;
//		double xMax = 4594202.;
//		double yMax = 5821736.;
		
//      // Berlin Coordinates: Area of Berlin
//		double xMin = 4573258.;
//		double yMin = 5801225.;
//		double xMax = 4620323.;
//		double yMax = 5839639.;
//		
//		gridParameters.setReceiverPointsGridMinX(xMin);
//		gridParameters.setReceiverPointsGridMinY(yMin);
//		gridParameters.setReceiverPointsGridMaxX(xMax);
//		gridParameters.setReceiverPointsGridMaxY(yMax);
			
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		File file = new File(outputFilePath);
		file.mkdirs();
				
		Grid grid = new Grid(scenario);
		
		// events	
		
		String eventsFile;
		if (eventsFileWithNoiseEvents == null) {
//			int iteration = config.controler().getLastIteration();
			int iteration = 100;
			eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		} else {
			eventsFile = eventsFileWithNoiseEvents;
		}
		
		EventsManager events = EventsUtils.createEventsManager();

		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		NoiseEventAnalysisHandler noiseHandler = new NoiseEventAnalysisHandler();
		
		log.info("Reading events file...");

		if (useNoiseEvents == false) {
			events.addHandler(moneyHandler);
			
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(eventsFile);
		
		} else {			
			// assuming the provided events file to contain noise events (caused)
			events.addHandler(noiseHandler);
			
			NoiseEventsReader noiseEventReader = new NoiseEventsReader(events);		
			noiseEventReader.parse(eventsFile);
		}
		
		log.info("Reading events file... Done.");
		
		// analysis
		
		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(scenario.getConfig(), WelfareMeasure.SELECTED, false);
		userBenefitsCalculator_selected.calculateUtility_money(scenario.getPopulation());
		
		double userBenefitsNoHomeLocation = 0.;
		double causedCostNoHomeLocation = 0.;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			// try to get the home location
			Activity homeActivity = null;
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (homeActivity == null) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						if (act.getType().equals(homeActivityType)) {
							homeActivity = act;
						}
					}
				} else {
					break;
				}
			}
			
			if (homeActivity == null) {
				// an agent without home location
				double personsMonetizedBenefit = userBenefitsCalculator_selected.getPersonId2MonetizedUtility().get(person.getId());
				
				double personsCausedNoiseCost = 0.;
				
				if (useNoiseEvents == false) {
					personsCausedNoiseCost = moneyHandler.getPersonId2amount().get(person.getId());		
					
				} else {
					if (noiseHandler.getPersonId2causedNoiseCost().containsKey(person.getId())) {
						personsCausedNoiseCost = noiseHandler.getPersonId2causedNoiseCost().get(person.getId());
					}
				}
				
				userBenefitsNoHomeLocation = userBenefitsNoHomeLocation + personsMonetizedBenefit;
				causedCostNoHomeLocation = causedCostNoHomeLocation + personsCausedNoiseCost;
				
			} else {

				// get the nearest receiver point 
				Id<ReceiverPoint> homeRPid = grid.getActivityCoord2receiverPointId().get(homeActivity.getCoord());
				if (rp2homeLocations.containsKey(homeRPid)) {
					double homeLocationsNew = rp2homeLocations.get(homeRPid) + 1;
					rp2homeLocations.put(homeRPid, homeLocationsNew);
				} else {
					rp2homeLocations.put(homeRPid, 1.0);
				}
				
				// user benefit
				double personsMonetizedBenefit = userBenefitsCalculator_selected.getPersonId2MonetizedUtility().get(person.getId());
				if (rp2totalUserBenefit.containsKey(homeRPid)) {
					double userBenefitsNew = rp2totalUserBenefit.get(homeRPid) + personsMonetizedBenefit;
					rp2totalUserBenefit.put(homeRPid, userBenefitsNew);
				} else {
					rp2totalUserBenefit.put(homeRPid, personsMonetizedBenefit);
				}
			
				// caused noise cost
				double personsCausedNoiseCost = 0.;
				
				if (useNoiseEvents == false) {
					personsCausedNoiseCost = moneyHandler.getPersonId2amount().get(person.getId());		
					
				} else {
					if (noiseHandler.getPersonId2causedNoiseCost().containsKey(person.getId())) {
						personsCausedNoiseCost = noiseHandler.getPersonId2causedNoiseCost().get(person.getId());
					}
				}
				
				if (rp2totalCausedNoiseCost.containsKey(homeRPid)) {
					double causedNoiseCostNew = rp2totalCausedNoiseCost.get(homeRPid) + personsCausedNoiseCost;
					rp2totalCausedNoiseCost.put(homeRPid, causedNoiseCostNew);
				} else {
					rp2totalCausedNoiseCost.put(homeRPid, personsCausedNoiseCost);
				}
				
				// affected noise cost
				double personsAffectedNoiseCost = 0.;
				
				if (useNoiseEvents == false) {
					log.warn("The amounts in the money events correspond to the caused noise cost. To analyze the amount that each person is affected by noise use the noise events.");
				} else {
					if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(person.getId())) {
						personsAffectedNoiseCost = noiseHandler.getPersonId2affectedNoiseCost().get(person.getId());
					}
				}
				
				if (rp2totalAffectedNoiseCost.containsKey(homeRPid)) {
					double affectedNoiseCostNew = rp2totalAffectedNoiseCost.get(homeRPid) + personsAffectedNoiseCost;
					rp2totalAffectedNoiseCost.put(homeRPid, affectedNoiseCostNew);
				} else {
					rp2totalAffectedNoiseCost.put(homeRPid, personsAffectedNoiseCost);
				}
			
			}
		}
			
		log.info("user benefits of persons without home location: " + userBenefitsNoHomeLocation);	
		log.info("caused noise costs of persons without home location: " + causedCostNoHomeLocation);		

		// write the results
			
		HashMap<Id<ReceiverPoint>,Double> id2xCoord = new HashMap<>();
		HashMap<Id<ReceiverPoint>,Double> id2yCoord = new HashMap<>();
		int c = 0;
		for(Id<ReceiverPoint> id : grid.getReceiverPoints().keySet()) {
			c++;
			if(c % 10000 == 0) {
				log.info("Writing out receiver point # "+ c);
			}
			id2xCoord.put(id, grid.getReceiverPoints().get(id).getCoord().getX());
			id2yCoord.put(id, grid.getReceiverPoints().get(id).getCoord().getY());
		}
		List<String> headers = new ArrayList<String>();
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");
		headers.add("homeLocations (sample size)");
		headers.add("userBenefits (sample size) [money]");
		headers.add("causedNoiseCost (sample size) [money]");
		headers.add("affectedNoiseCost (sample size) [money]");
		
		List<HashMap<Id<ReceiverPoint>,Double>> values = new ArrayList<>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		values.add(rp2homeLocations);
		values.add(rp2totalUserBenefit);
		values.add(rp2totalCausedNoiseCost);
		values.add(rp2totalAffectedNoiseCost);
		
		NoiseWriter.write(outputFilePath, 7, headers, values, useCompression);

	}

	public final boolean isUseCompression() {
		return this.useCompression;
	}

	public final void setUseCompression(boolean useCompression) {
		this.useCompression = useCompression;
	}
			 
}
		

