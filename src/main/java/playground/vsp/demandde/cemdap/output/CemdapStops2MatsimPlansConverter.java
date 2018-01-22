/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * CemdapStops2MatsimPlansConverter.java                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.cemdap.output;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.demandde.cemdap.LogToOutputSaver;
import playground.vsp.demandde.corineLandcover.CorineLandCoverData;

/**
 * @author dziemke
 */
public class CemdapStops2MatsimPlansConverter {
	private static final Logger LOG = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);

	public static void main(String[] args) throws IOException {
		// Local use
		String cemdapDataRoot = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/cemdap_output/";
		int numberOfFirstCemdapOutputFile = 200;
		int numberOfPlans = 5;
		int numberOfPlansFile = 200;
		String outputDirectory = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/matsim_initial/" + numberOfPlansFile + "/";
		String zonalShapeFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/gemeindenLOR_DHDN_GK4.shp";
		String zoneIdTag = "NR";		
		boolean allowVariousWorkAndEducationLocations = true;
		boolean addStayHomePlan = true;
		boolean useLandCoverData = false;
		String landCoverFile = "CURRENTLY_JUST_A_PLACEHOLDER";
		String stopFile = "Stops.out";
		String activityFile = "Activity.out";
		boolean simplifyGeometries = false;
		boolean assignCoordinatesToActivities = true; // if set to false, the zone id will be attached to activity types and a fake coordinate will be given.
		boolean combiningGeoms = true;
		
		// Server use
		if (args.length != 0) {
			cemdapDataRoot = args[6];
			numberOfFirstCemdapOutputFile = Integer.parseInt(args[0]);
			numberOfPlans = Integer.parseInt(args[1]);
			allowVariousWorkAndEducationLocations = Boolean.parseBoolean(args[2]);
			addStayHomePlan = Boolean.parseBoolean(args[3]);
			outputDirectory = args[4];
			zonalShapeFile = args[5];
			useLandCoverData = Boolean.parseBoolean(args[7]); // I think, it is not necessary, one can provide landCoverFile or not. Amit Oct'17
			landCoverFile = args[8];
			simplifyGeometries = Boolean.valueOf(args[9]);
			combiningGeoms = Boolean.valueOf(args[10]);
			assignCoordinatesToActivities = Boolean.valueOf(args[11]);
		}

		Map<String, String> shapeFileToFeatureKey = new HashMap<>();
		shapeFileToFeatureKey.put(zonalShapeFile, zoneIdTag);
		
		convert(cemdapDataRoot, numberOfFirstCemdapOutputFile, numberOfPlans, outputDirectory,
				shapeFileToFeatureKey, allowVariousWorkAndEducationLocations, addStayHomePlan,
				useLandCoverData, landCoverFile, stopFile, activityFile, simplifyGeometries, combiningGeoms, assignCoordinatesToActivities);
	}

	
	public static void convert(String cemdapDataRoot, int numberOfFirstCemdapOutputFile, int numberOfPlans, String outputDirectory,
			Map<String, String> shapeFileToFeatureKey, // ensures, unique shape files with same or different featureKey. Amit Nov'17
							   boolean allowVariousWorkAndEducationLocations, boolean addStayHomePlan,
			boolean useLandCoverData, String landCoverFile, String stopFile, String activityFile, boolean simplifyGeometries, boolean combiningGeoms, boolean assignCoordinatesToActivities) throws IOException {

		CorineLandCoverData corineLandCoverData = null;
		// CORINE landcover
		if (landCoverFile!=null && useLandCoverData) {
			corineLandCoverData = new CorineLandCoverData(landCoverFile, simplifyGeometries, combiningGeoms);
		}

		LogToOutputSaver.setOutputDirectory(outputDirectory);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// Find respective stops file
		Map<Integer, String> cemdapStopsFilesMap = new HashMap<>();
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			int numberOfCurrentInputFile = numberOfFirstCemdapOutputFile + planNumber;
			String cemdapStopsFile = cemdapDataRoot + numberOfCurrentInputFile + "/" + stopFile;
			cemdapStopsFilesMap.put(planNumber, cemdapStopsFile);
		}
	
		// Create ObjectAttributes for each agent and each plan
		Map<Integer, ObjectAttributes> personZoneAttributesMap = new HashMap<>();
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			ObjectAttributes personZoneAttributes = new ObjectAttributes();
			personZoneAttributesMap.put(planNumber, personZoneAttributes);
		}
		
		Map<Id<Person>, Coord> homeZones = new HashMap<>();
		
		// Write all (geographic) features of planning area to a map
		Map<String,SimpleFeature> zones = new HashMap<>();
		for (Map.Entry<String, String> entry : shapeFileToFeatureKey.entrySet()) {
			for (SimpleFeature feature : ShapeFileReader.getAllFeatures(entry.getKey())) {
				String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString((String) feature.getAttribute(entry.getValue()));
				// TODO check if removal of leading zero is always valid
				// It was necessary since code may interpret number incorrectly if leading zero present; dz, jul'17
				// Can this lead to duplicate keys?? Amit Nov'17
				zones.put(shapeId, feature);
			}
		}
		
		// Get all persons from activity file
//		List<Id<Person>> personsIds = new LinkedList<>();
		Map<Id<Person>, String> personHomeMap = new HashMap<>();
		// Additional information from files read in below (e.g. Persons) would be required to take person-specific information like age
		// along for later analyses; dz, jul'17
//		CemdapPersonParser cemdapPersonParser = new CemdapPersonParser();
//		cemdapPersonParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + cemdapAdultsFilename, personsIds);
//		cemdapPersonParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + cemdapChildrenFilename, personsIds);
		CemdapActivityParser cemdapActivityParser = new CemdapActivityParser();
		cemdapActivityParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + activityFile, personHomeMap);
		
		Population population = scenario.getPopulation();
		
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(planNumber), planNumber, population, personZoneAttributesMap.get(planNumber));
			
			// Commenting this for the time being; it does not do anything if the activity file is not considered on top of the stops file, dz,aa, sep'17
			// Add a stay-home plan for those people who have no stops (i.e. no travel) in current stop file
			// Following is required to add persons who just stays at home. Such persons does not appear in the Stops.out file. dz, aa Oct'17
			LOG.info("Start assigning stay-home plans to persons who are not in stops file.");
			LOG.info("Size of personHomeMap = " + personHomeMap.size() + ".");
			int counter = 0;
			for (Id<Person> personId : personHomeMap.keySet()) {
				Person person = population.getPersons().get(personId);
				if (person == null) {
					person = population.getFactory().createPerson(personId);
					population.addPerson(person);
				}
				if (person.getPlans().size() <= planNumber) {
					Plan stayHomePlan = population.getFactory().createPlan();
					stayHomePlan.addActivity(population.getFactory().createActivityFromCoord(ActivityTypes.HOME, new Coord(-1.0, -1.0))); // TODO maybe improve later
					person.addPlan(stayHomePlan);
					personZoneAttributesMap.get(planNumber).putAttribute(personId.toString(), "zone" + "0", personHomeMap.get(personId)); // TODO maybe improve later
					counter++;
				}
			}
			LOG.info("For " + counter + " persons, stay-home plans have been added. Plan number is " + planNumber + ".");
		}

		if (assignCoordinatesToActivities) {
			// Assign home coordinates
			Feature2Coord feature2Coord = new Feature2Coord();
			// TODO consideration of CORINE land cover probably in this step
			//Done. Amit Oct'17
			feature2Coord.assignHomeCoords(population,
					personZoneAttributesMap.get(0),
					zones,
					homeZones,
					corineLandCoverData);

			// Assign coordinates to all other activities
			for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
				feature2Coord.assignCoords(population, planNumber, personZoneAttributesMap.get(planNumber), zones, homeZones, allowVariousWorkAndEducationLocations, corineLandCoverData);
				// TODO consideration of CORINE land cover probably in this step
				//Done. Amit Oct'17
			}
		} else {
			LOG.warn("No coordinate will be assigned to activities. The zone id for each activity will be concatenated at the end of activityType seperated by '_'.");
			// for EXTRA LARGE scenario, one can assign coordinates after sampling which would be at least '1/sampleSize' times faster. Amit Oct'17
			for (Person person : population.getPersons().values()) {
				for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
					int activityIndex = 0;
					for (PlanElement planElement : person.getPlans().get(planNumber).getPlanElements()) {
						if (planElement instanceof Activity) {
							Activity activity = (Activity)planElement;
							String activityType = activity.getType();
							String zoneId = (String) personZoneAttributesMap.get(planNumber).getAttribute(person.getId().toString(), CemdapStopsParser.ZONE + activityIndex);
							if (zoneId == null) {
								throw new RuntimeException("Person with ID " + person.getId() + ": Object attribute '" + CemdapStopsParser.ZONE + activityIndex + "' not found.");
							}
							activity.setType(activityType+"_"+zoneId);
						}
					}
				}
			}
		}

				
		// If applicable, add a stay-home plan for everybody
		if (addStayHomePlan) {
			numberOfPlans++;
			
			for (Person person : population.getPersons().values()) {
				Plan firstPlan = person.getPlans().get(0);
				// Get first (i.e. presumably "home") activity from agent's first plan
				Activity firstActivity = (Activity) firstPlan.getPlanElements().get(0);

				Plan stayHomePlan = population.getFactory().createPlan();
				// Create new activity with type and coordinates (but without end time) and add it to stay-home plan
				stayHomePlan.addActivity(population.getFactory().createActivityFromCoord(firstActivity.getType(), firstActivity.getCoord()));
				person.addPlan(stayHomePlan);
			}
		}
			
		// Check if number of plans that each agent has is correct
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getPlans().size() < numberOfPlans) {
				LOG.warn("Person with ID " + person.getId() + " has less than " + numberOfPlans + " plans");
			}
			if (person.getPlans().size() > numberOfPlans) {
				LOG.warn("Person with ID " + person.getId() + " has more than " + numberOfPlans + " plans");
				}
		}
		
		// Write population file
		new File(outputDirectory).mkdir();
		new PopulationWriter(scenario.getPopulation(), null).write(outputDirectory + "plans.xml.gz");
	}
}