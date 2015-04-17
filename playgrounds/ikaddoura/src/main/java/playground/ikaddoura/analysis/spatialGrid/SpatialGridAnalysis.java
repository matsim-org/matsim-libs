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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.noise2.NoiseParameters;
import playground.ikaddoura.noise2.NoiseWriter;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.data.ReceiverPoint;
import playground.ikaddoura.noise2.events.NoiseEventsReader;
import playground.ikaddoura.noise2.utils.NoiseEventAnalysisHandler;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

public class SpatialGridAnalysis {
	private static final Logger log = Logger.getLogger(SpatialGridAnalysis.class);
	
	private static String runDirectory;
	private static double receiverPointGap;
	private static String homeActivityType;
	private static String outputFilePath;
	
	// if the following variable is null the standard events file is used
	private static String eventsFileWithNoiseEvents;
	private static boolean useMoneyEvents;
		
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
			
			useMoneyEvents = Boolean.valueOf(args[5]);		
			log.info("use money events: " + outputFilePath);
			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/noise_int_1a/";
//			eventsFileWithNoiseEvents = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalizationCar/output/baseCase_2_noiseAnalysis/r31341/noiseAnalysis_BlnBC2_1/analysis_it.100/100.events_NoiseImmission_Offline.xml.gz";
			eventsFileWithNoiseEvents = null;
			outputFilePath = "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/noise_int_1a/analysis_spatial_grid_1/";
			
			receiverPointGap = 100.;
			homeActivityType = "home";
			useMoneyEvents = false;
		}
		
		SpatialGridAnalysis analysis = new SpatialGridAnalysis();
		analysis.run();		
	}

	private void run() {
		
		HashMap<Id<ReceiverPoint>, Double> rp2totalUserBenefit = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2homeLocations = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2totalCausedNoiseCost = new HashMap<>();
		HashMap<Id<ReceiverPoint>, Double> rp2totalAffectedNoiseCost = new HashMap<>();

		// noise parameters 
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setScaleFactor(10.);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setThrowNoiseEventsAffected(false);
		noiseParameters.setThrowNoiseEventsCaused(false);
			
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
//		noiseParameters.setReceiverPointsGridMinX(xMin);
//		noiseParameters.setReceiverPointsGridMinY(yMin);
//		noiseParameters.setReceiverPointsGridMaxX(xMax);
//		noiseParameters.setReceiverPointsGridMaxY(yMax);
			
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);
			
		String configFile = runDirectory + "output_config.xml.gz";
		String populationFile = runDirectory + "output_plans.xml.gz";
		String networkFile = runDirectory + "output_network.xml.gz";
	
		Config config = ConfigUtils.loadConfig(configFile);		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
				
		NoiseContext noiseContext = new NoiseContext(scenario, noiseParameters);
		noiseContext.initialize();
		
		File file = new File(outputFilePath);
		file.mkdirs();
		
		String eventsFile;
		if (eventsFileWithNoiseEvents == null) {
			int iteration = config.controler().getLastIteration();
			eventsFile = runDirectory + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		} else {
			eventsFile = eventsFileWithNoiseEvents;
		}
		
		EventsManager events = EventsUtils.createEventsManager();

		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		NoiseEventAnalysisHandler noiseHandler = new NoiseEventAnalysisHandler();
		
		log.info("Reading events file...");

		if (useMoneyEvents) {
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
				
			} else {

				// get the nearest receiver point 
				Id<ReceiverPoint> homeRPid = noiseContext.getActivityCoord2receiverPointId().get(homeActivity.getCoord());
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
				
				if (useMoneyEvents) {
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
				
				if (useMoneyEvents) {
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
			
		// write the results
		
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath);
		
		HashMap<Id<ReceiverPoint>,Double> id2xCoord = new HashMap<>();
		HashMap<Id<ReceiverPoint>,Double> id2yCoord = new HashMap<>();
		int c = 0;
		for(Id<ReceiverPoint> id : noiseContext.getReceiverPoints().keySet()) {
			c++;
			if(c % 10000 == 0) {
				log.info("Writing out receiver point # "+ c);
			}
			id2xCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getX());
			id2yCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getY());
		}
		List<String> headers = new ArrayList<String>();
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");
		headers.add("homeLocations");
		headers.add("userBenefits [money]");
		headers.add("causedNoiseCost [money]");
		headers.add("affectedNoiseCost [money]");
		
		List<HashMap<Id<ReceiverPoint>,Double>> values = new ArrayList<>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		values.add(rp2homeLocations);
		values.add(rp2totalUserBenefit);
		values.add(rp2totalCausedNoiseCost);
		values.add(rp2totalAffectedNoiseCost);
		
		NoiseWriter.write(outputFilePath, 7, headers, values);

	}
			 
}
		

