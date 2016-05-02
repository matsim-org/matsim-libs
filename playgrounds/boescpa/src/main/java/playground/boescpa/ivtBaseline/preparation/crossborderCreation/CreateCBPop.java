/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.preparation.crossborderCreation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.lib.tools.FacilityUtils;

/**
 * Unifies the creations of CB-Sub-Population to the creation of a single CB-Population.
 *
 * @author boescpa
 */
public class CreateCBPop {

	public static final String CB_TAG = "cb";

	public static void main(final String[] args) {
		final Config fullConfig = ConfigUtils.loadConfig(args[0], new CreateSingleTripPopulationConfigGroup());
		CreateSingleTripPopulationConfigGroup configGroup =
				(CreateSingleTripPopulationConfigGroup) fullConfig.getModule(CreateSingleTripPopulationConfigGroup.GROUP_NAME) ;

		final String pathToOutput_CBPopulation = configGroup.getPathToOutput().substring(0, configGroup.getPathToOutput().indexOf(".xml"))
				+ "_" + configGroup.getRandomSeed() + "_" + configGroup.getSamplePercentage() + ".xml.gz";
		configGroup.setPathToOutput(pathToOutput_CBPopulation);
		configGroup.setTag(CB_TAG);


		// CB-Transit
		//	config creation
		CreateSingleTripPopulationConfigGroup transitConfig = configGroup.copy();
		transitConfig.setPathToCumulativeDepartureProbabilities(
				configGroup.getPathToCumulativeDepartureProbabilities() + "CumulativeProbabilityTransitDeparture.txt");
		transitConfig.setPathToOriginsFile(configGroup.getPathToOriginsFile() + "OD_CB-Agents_Transit.txt");
		transitConfig.setPathToOutput(
				configGroup.getPathToOutput().substring(0,configGroup.getPathToOutput().indexOf(".xml")) + "_Transit.xml.gz");
		//	population creation
		CreateCBTransit cbTransit = new CreateCBTransit(transitConfig);
		cbTransit.runPopulationCreation();
		cbTransit.writeOutput();

		// CB-SecondaryActivities
		//	config creation
		CreateSingleTripPopulationConfigGroup saConfig = configGroup.copy();
		saConfig.setPathToCumulativeDepartureProbabilities(
				configGroup.getPathToCumulativeDepartureProbabilities() + "CumulativeProbabilitySecondaryActivityDeparture.txt");
		saConfig.setPathToOriginsFile(configGroup.getPathToOriginsFile() + "OD_CB-Agents_SecondaryActivities.txt");
		saConfig.setPathToOutput(
				configGroup.getPathToOutput().substring(0,configGroup.getPathToOutput().indexOf(".xml")) + "_SA.xml.gz");
		//	population creation
		CreateCBSecondaryActivities cbSecondaryActivities = new CreateCBSecondaryActivities(saConfig);
		cbSecondaryActivities.runPopulationCreation();
		cbSecondaryActivities.writeOutput();

		// CB-Work
		//	config creation
		CreateSingleTripPopulationConfigGroup workConfig = configGroup.copy();
		workConfig.setPathToCumulativeDepartureProbabilities(
				configGroup.getPathToCumulativeDepartureProbabilities() + "CumulativeProbabilityWorkDeparture.txt");
		workConfig.setPathToOriginsFile(configGroup.getPathToOriginsFile() + "O_CB-Agents_Work.txt");
		workConfig.setPathToDestinationsFile(configGroup.getPathToDestinationsFile() + "D_CB-Agents_Work.txt");
		workConfig.setPathToOutput(
				configGroup.getPathToOutput().substring(0,configGroup.getPathToOutput().indexOf(".xml")) + "_Work.xml.gz");
		//	population creation
		CreateCBWork cbWork = new CreateCBWork(workConfig);
		cbWork.runPopulationCreation();
		cbWork.writeOutput();


		mergeFacilities(pathToOutput_CBPopulation);
		mergeSubpopulations(transitConfig.getPathToOutput(), saConfig.getPathToOutput(), pathToOutput_CBPopulation);
		mergeSubpopulations(pathToOutput_CBPopulation, workConfig.getPathToOutput(), pathToOutput_CBPopulation);
	}

	private static void mergeFacilities(String pathToOutput_CBPopulation) {
		ActivityFacilities cbFacilities = FacilitiesUtils.createActivityFacilities();
		ActivityFacilities transitFacilities = FacilityUtils.readFacilities(
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Transit_Facilities.xml.gz");
		for (ActivityFacility facility : transitFacilities.getFacilities().values()) {
			cbFacilities.addActivityFacility(facility);
		}
		ActivityFacilities saFacilities = FacilityUtils.readFacilities(
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_SA_Facilities.xml.gz");
		for (ActivityFacility facility : saFacilities.getFacilities().values()) {
			addFacility(cbFacilities, facility);
		}
		ActivityFacilities workFacilities = FacilityUtils.readFacilities(
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Work_Facilities.xml.gz");
		for (ActivityFacility facility : workFacilities.getFacilities().values()) {
			addFacility(cbFacilities, facility);
		}

		new FacilitiesWriter(cbFacilities).writeV1(pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Facilities.xml.gz");
	}

	private static void addFacility(ActivityFacilities cbFacilities, ActivityFacility facility) {
		if (!cbFacilities.getFacilities().containsKey(facility.getId())) {
			cbFacilities.addActivityFacility(facility);
		} else {
			for (String activityType : facility.getActivityOptions().keySet()) {
				ActivityFacility cbFacility = cbFacilities.getFacilities().get(facility.getId());
				if (!cbFacility.getActivityOptions().containsKey(activityType)) {
					cbFacility.addActivityOption(facility.getActivityOptions().get(activityType));
				}
			}
		}
	}

	private static void mergeSubpopulations(String pathToMainPop, String pathToAdditionalPop, String pathToOutputPop) {
		// read the scenario population
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(pathToMainPop);
		Population scenarioPopulation = scenario.getPopulation();
		ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(scenarioPopulation.getPersonAttributes());
		attributesReader.parse(pathToMainPop.substring(0, pathToMainPop.indexOf(".xml")) + "_Attributes.xml.gz");
		// add the freight population to the scenario population
		plansReader.readFile(pathToAdditionalPop);
		attributesReader.parse(pathToAdditionalPop.substring(0, pathToAdditionalPop.indexOf(".xml")) + "_Attributes.xml.gz");
		// write the new, merged population and its attributes:
		PopulationWriter writer = new PopulationWriter(scenarioPopulation);
		writer.write(pathToOutputPop);
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(scenarioPopulation.getPersonAttributes());
		attributesWriter.writeFile(pathToOutputPop.substring(0, pathToOutputPop.indexOf(".xml")) + "_Attributes.xml.gz");
	}
}
