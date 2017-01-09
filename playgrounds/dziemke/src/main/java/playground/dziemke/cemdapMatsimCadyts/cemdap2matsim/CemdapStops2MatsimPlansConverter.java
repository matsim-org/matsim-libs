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

package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;
import playground.dziemke.utils.LogToOutputSaver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dziemke
 */
public class CemdapStops2MatsimPlansConverter {
	private static final Logger LOG = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);
	
	// Parameters
	private int numberOfFirstCemdapOutputFile = -1;
	private int numberOfPlans = -1;
	private boolean allowVariousWorkAndEducationLocations = false;
	private boolean addStayHomePlan = false;
	
	// Input and output
	private String outputDirectory;
	private String zonalShapeFile;
	private String cemdapDataRoot;
	private String cemdapStopsFilename = "Stops.out";
//	private String cemdapAdultsFilename = "Adults.out";
//	private String cemdapChildrenFilename = "Children.out";
	private String cemdapActivityFilename = "Activity.out";
	
	public static void main(String[] args) {
		int numberOfFirstCemdapOutputFile = 100;
//		int numberOfFirstCemdapOutputFile = 90;

		int numberOfPlans = 5;
		boolean allowVariousWorkAndEducationLocations = true;
		boolean addStayHomePlan = true;

		int numberOfPlansFile = 100;
//		int numberOfPlansFile = 35;

//		String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap2matsim/" + numberOfPlansFile + "/";
		String outputDirectory = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/matsim_initial/" + numberOfPlansFile + "/";
		String zonalShapeFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/gemeindenLOR_DHDN_GK4.shp";
//		String cemdapDataRoot = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_output/";
		String cemdapDataRoot = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/cemdap_output/";

		if (args.length != 0) {
			numberOfFirstCemdapOutputFile = Integer.parseInt(args[0]);
			numberOfPlans = Integer.parseInt(args[1]);
			allowVariousWorkAndEducationLocations = Boolean.parseBoolean(args[2]);
			addStayHomePlan = Boolean.parseBoolean(args[3]);

			outputDirectory = args[4];
			zonalShapeFile = args[5];
			cemdapDataRoot = args[6];
		}
		
		CemdapStops2MatsimPlansConverter converter = new CemdapStops2MatsimPlansConverter(zonalShapeFile, cemdapDataRoot);
		
		converter.setOutputDirectory(outputDirectory);
		converter.setNumberOfFirstCemdapOutputFile(numberOfFirstCemdapOutputFile);
		converter.setNumberOfPlans(numberOfPlans);
		converter.setAllowVariousWorkAndEducationLocations(allowVariousWorkAndEducationLocations);
		converter.setAddStayHomePlan(addStayHomePlan);
		
		try {
			converter.convert();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public CemdapStops2MatsimPlansConverter(String zonalShapeFile, String cemdapDataRoot) {
		this.zonalShapeFile = zonalShapeFile;
		this.cemdapDataRoot = cemdapDataRoot;
	}
	
	public void convert() throws IOException {
		if (!checkIfParametersValid()) return;
		LogToOutputSaver.setOutputDirectory(outputDirectory);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// Find respective stops file
		Map<Integer, String> cemdapStopsFilesMap = new HashMap<>();
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			int numberOfCurrentInputFile = numberOfFirstCemdapOutputFile + planNumber;
			String cemdapStopsFile = cemdapDataRoot + numberOfCurrentInputFile + "/" + cemdapStopsFilename;
			cemdapStopsFilesMap.put(planNumber, cemdapStopsFile);
		}
	
		// Create ObjectAttrubutes for each agent and each plan
		Map<Integer, ObjectAttributes> personZoneAttributesMap = new HashMap<>();
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			ObjectAttributes personZoneAttributes = new ObjectAttributes();
			personZoneAttributesMap.put(planNumber, personZoneAttributes);
		}
		
		Map<Id<Person>, Coord> homeZones = new HashMap<>();
		
		// Write all (geographic) features of planning area to a map
		Map<String,SimpleFeature> zones = new HashMap<>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(zonalShapeFile)) {
			String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString((String) feature.getAttribute("NR"));
			zones.put(shapeId,feature);
		}
		
		// Get all persons from activity file
//		List<Id<Person>> personsIds = new LinkedList<>();
		Map<Id<Person>, String> personHomeMap = new HashMap<>();
//		CemdapPersonParser cemdapPersonParser = new CemdapPersonParser();
//		cemdapPersonParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + cemdapAdultsFilename, personsIds);
//		cemdapPersonParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + cemdapChildrenFilename, personsIds);
		CemdapActivityParser cemdapActivityParser = new CemdapActivityParser();
		cemdapActivityParser.parse(cemdapDataRoot + numberOfFirstCemdapOutputFile + "/" + cemdapActivityFilename, personHomeMap);
		
		Population population = scenario.getPopulation();
		
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			// Parse cemdap stops file
			new CemdapStopsParser().parse(cemdapStopsFilesMap.get(planNumber), planNumber, population, personZoneAttributesMap.get(planNumber));
			
			// Add a stay-home plan for those people who have no stops (i.e. no travel) in current stop file
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
		
		// Assign home coordinates
		Feature2Coord feature2Coord = new Feature2Coord();
		feature2Coord.assignHomeCoords(population, personZoneAttributesMap.get(0), zones, homeZones);
		
		// Assign coordinates to all other activities
		for (int planNumber = 0; planNumber < numberOfPlans; planNumber++) {
			feature2Coord.assignCoords(population, planNumber, personZoneAttributesMap.get(planNumber), zones, homeZones, allowVariousWorkAndEducationLocations);
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
		//new ObjectAttributesXmlWriter(personObjectAttributesMap.get(0)).writeFile(outputBase+"personObjectAttributes0.xml.gz");
	}

	private boolean checkIfParametersValid() {
		if (numberOfFirstCemdapOutputFile == -1) {
			LOG.warn("NumberOfFirstCemdapOutputFile not set.");
			return false;
		}
		if (numberOfPlans == -1) {
			LOG.warn("NumberOfPlans not set.");
			return false;
		}
		if (outputDirectory.isEmpty()) {
			LOG.warn("OutputDirectory is empty.");
			return false;
		}
		if (cemdapStopsFilename.isEmpty()) {
			LOG.warn("CemdapStopsFilename is empty.");
			return false;
		}
		return true;
	}

	public int getNumberOfFirstCemdapOutputFile() {
		return numberOfFirstCemdapOutputFile;
	}

	public void setNumberOfFirstCemdapOutputFile(int numberOfFirstCemdapOutputFile) {
		this.numberOfFirstCemdapOutputFile = numberOfFirstCemdapOutputFile;
	}

	public int getNumberOfPlans() {
		return numberOfPlans;
	}

	public void setNumberOfPlans(int numberOfPlans) {
		this.numberOfPlans = numberOfPlans;
	}

	public boolean isAddStayHomePlan() {
		return addStayHomePlan;
	}

	public void setAddStayHomePlan(boolean addStayHomePlan) {
		this.addStayHomePlan = addStayHomePlan;
	}
	
	public boolean isAllowVariousWorkAndEducationLocations() {
		return allowVariousWorkAndEducationLocations;
	}

	public void setAllowVariousWorkAndEducationLocations(boolean allowVariousWorkAndEducationLocations) {
		this.allowVariousWorkAndEducationLocations = allowVariousWorkAndEducationLocations;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getCemdapStopFilename() {
		return cemdapStopsFilename;
	}

	public void setCemdapStopFilename(String cemdapStopFilename) {
		this.cemdapStopsFilename = cemdapStopFilename;
	}
}