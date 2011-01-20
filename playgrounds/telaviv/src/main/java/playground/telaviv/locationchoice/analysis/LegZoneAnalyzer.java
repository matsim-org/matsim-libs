/* *********************************************************************** *
 * project: org.matsim.*
 * ShoppingLegZoneAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.locationchoice.ExtendedLocationChoicePlanModule;
import playground.telaviv.locationchoice.ExtendedLocationChoiceProbabilityCreator;
import playground.telaviv.zones.ZoneMapping;

public class LegZoneAnalyzer {

	private static final Logger log = Logger.getLogger(LegZoneAnalyzer.class);
	
	private static String basePath = "../../matsim/mysimulations/telaviv/";
//	private static String runPath = "output_JDEQSim_with_location_choice/";
	private static String runPath = "output_JDEQSim_with_location_choice_without_TravelTime/";
//	private static String runPath = "output_without_location_choice_0.10/";
	
	private static String networkFile = basePath + "input/network.xml";
//	private static String populationFile = basePath + "input/plans_10.xml.gz";
//	private static String populationFile = basePath + runPath + "ITERS/it.0/0.plans.xml.gz";
	private static String populationFile = basePath + runPath + "ITERS/it.100/100.plans.xml.gz";
	
//	private String eventsFile = basePath + runPath + "/ITERS/it.0/0.events.txt.gz";
	private String eventsFile = basePath + runPath + "/ITERS/it.100/100.events.txt.gz";
//	private String eventsFile = null;

//	private String shoppingOutFile = basePath + runPath + "initial.shoppingLegsCarProbabilities.txt";
//	private String otherOutFile = basePath + runPath + "initial.otherLegsCarProbabilities.txt";
//	private String workOutFile = basePath + runPath + "initial.workLegsCarProbabilities.txt";
//	private String educationOutFile = basePath + runPath + "initial.educationLegsCarProbabilities.txt";
	private String shoppingOutFile = basePath + runPath + "100.shoppingLegsCarProbabilities.txt";
	private String otherOutFile = basePath + runPath + "100.otherLegsCarProbabilities.txt";
	private String workOutFile = basePath + runPath + "100.workLegsCarProbabilities.txt";
	private String educationOutFile = basePath + runPath + "100.educationLegsCarProbabilities.txt";

	private String delimiter = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	
	private ZoneMapping zoneMapping;
	private ExtendedLocationChoiceProbabilityCreator extendedLocationChoiceProbabilityCreator;
	
	private Map<Id, List<Integer>> shoppingActivities;	// <PersonId, List<Index of Activity>
	private Map<Id, List<Integer>> otherActivities;	// <PersonId, List<Index of Activity>
	private Map<Id, List<Integer>> workActivities;	// <PersonId, List<Index of Activity>
	private Map<Id, List<Integer>> educationActivities;	// <PersonId, List<Index of Activity>
	
	private List<Double> shoppingProbabilities;
	private List<Double> otherProbabilities;
	private List<Double> workProbabilities;
	private List<Double> educationProbabilities;
	
	public static void main(String[] args) {
		Scenario scenario = new ScenarioImpl();
		
		// load network
		new MatsimNetworkReader(scenario).readFile(networkFile);
			
		// load population
		new MatsimPopulationReader(scenario).readFile(populationFile);

		new LegZoneAnalyzer(scenario);
	}
	
	public LegZoneAnalyzer(Scenario scenario) {
		this.scenario = scenario;
		
		TravelTimeCalculator travelTime = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		if (eventsFile != null) {
			// We use a new EventsManager where we only register the TravelTimeCalculator.
			EventsManager eventsManager = new EventsManagerImpl();
			eventsManager.addHandler(travelTime);
			
			log.info("Processing events file to get initial travel times...");
			EventsReaderTXTv1 reader = new EventsReaderTXTv1(eventsManager);
			reader.readFile(eventsFile);
			
			eventsManager.removeHandler(travelTime);
			eventsManager = null;
		}
				
		log.info("Identifying shopping activities...");
		ExtendedLocationChoicePlanModule elcpm = new ExtendedLocationChoicePlanModule(scenario, null);
		shoppingActivities = elcpm.getShoppingActivities();
		otherActivities = elcpm.getOtherActivities();
		workActivities = elcpm.getWorkActivities();
		educationActivities = elcpm.getEducationActivities();
		log.info("done.");
		
		log.info("Creating ZoneMapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");
		
		log.info("Creating LocationChoiceProbabilityCreator...");
		extendedLocationChoiceProbabilityCreator = new ExtendedLocationChoiceProbabilityCreator(scenario, travelTime);
		extendedLocationChoiceProbabilityCreator.calculateDynamicProbabilities();
		extendedLocationChoiceProbabilityCreator.calculateTotalProbabilities();
		log.info("done.");
		
		// types: Shop, Other, Work, Education
		log.info("Get probabilities of selected zones...");
		shoppingProbabilities = getProbabilities(shoppingActivities, 0);
		otherProbabilities = getProbabilities(otherActivities, 1);
		workProbabilities = getProbabilities(workActivities, 2);
		educationProbabilities = getProbabilities(educationActivities, 3);
		log.info("done.");
		
		log.info("Writing probabilities to file...");
		writeFile(shoppingOutFile, shoppingProbabilities);
		writeFile(otherOutFile, otherProbabilities);
		writeFile(workOutFile, workProbabilities);
		writeFile(educationOutFile, educationProbabilities);
		log.info("done.");
	}
	
	private List<Double> getProbabilities(Map<Id, List<Integer>> activities, int type) {
		List<Double> probabilities = new ArrayList<Double>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			List<Integer> activitiesList = activities.get(person.getId());
			
			if (activitiesList == null) continue;
			
			/*
			 * The first Activity is always being at home.
			 */
			Activity homeActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			
			for (int index : activitiesList) {	
				Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(index);
				double probability = getProbability(homeActivity, activity, type);
				
				probabilities.add(probability);
			}
		}
		return probabilities;
	}
	
	private double getProbability(Activity homeActivity, Activity activity, int type) {
		Id homeLinkId = homeActivity.getLinkId();
		Id shoppingLinkId = activity.getLinkId();
		
		int homeTAZ = zoneMapping.getLinkTAZ(homeLinkId);
		int activityTAZ = zoneMapping.getLinkTAZ(shoppingLinkId);
		
//		Map<Integer, Double> probabilities = extendedLocationChoiceProbabilityCreator.getFromZoneProbabilities(type, homeTAZ, activity.getEndTime());
		
		// Tuple<TAZ, Probability>
		Tuple<int[], double[]> tuple = extendedLocationChoiceProbabilityCreator.getFromZoneProbabilities(type, homeTAZ, activity.getEndTime());
				
		int[] indices = tuple.getFirst();
		double[] probabilities = tuple.getSecond();
		
		for (int i = 0; i < indices.length; i++) {
			if (indices[i] == activityTAZ) return probabilities[i];
		}
		
		return 0.0;
	}
	
	private void writeFile(String outFile, List<Double> probabilities) {
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		
	    try {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("probability" + "\n");
			
			// write Values
			for (Double probability : probabilities) {
				bw.write(String.valueOf(probability));
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}