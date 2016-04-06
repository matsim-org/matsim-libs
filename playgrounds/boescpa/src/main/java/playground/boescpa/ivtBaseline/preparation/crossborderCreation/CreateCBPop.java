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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.lib.tools.FacilityUtils;

/**
 * Unifies the creations of CB-Sub-Population to the creation of a single CB-Population.
 *
 * @author boescpa
 */
public class CreateCBPop {

	public static void main(final String[] args) {
		final String pathToFacilities = args[0]; // all scenario facilities incl secondary facilities and bc facilities.
		final String pathToCumulativeDepartureProbabilities = args[1];
		final String samplePercentage = args[2];
		final String randomSeed = args[3];
		final String pathToInput_CBFiles = args[4];
		final String pathToOutput_CBPopulation = args[5].substring(0, args[5].indexOf(".xml"))
				+ "_" + randomSeed + "_" + samplePercentage + ".xml.gz";

		// CB-Transit
		String[] transitArgs = new String[]{
				pathToFacilities,
				pathToCumulativeDepartureProbabilities + "/CumulativeProbabilityTransitDeparture.txt",
				samplePercentage,
				randomSeed,
				pathToInput_CBFiles + "/OD_CB-Agents_Transit.txt",
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Transit.xml.gz"};
		CreateCBTransit.main(transitArgs);

		// CB-SecondaryActivities
		String[] saArgs = new String[]{
				pathToFacilities,
				pathToCumulativeDepartureProbabilities + "/CumulativeProbabilitySecondaryActivityDeparture.txt",
				samplePercentage,
				randomSeed,
				pathToInput_CBFiles + "/OD_CB-Agents_SecondaryActivities.txt",
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_SA.xml.gz"};
		CreateCBSecondaryActivities.main(saArgs);

		// CB-Work
		String[] workArgs = new String[]{
				pathToFacilities,
				pathToCumulativeDepartureProbabilities + "/CumulativeProbabilityWorkDeparture.txt",
				samplePercentage,
				randomSeed,
				pathToInput_CBFiles + "/D_CB-Agents_Work.txt",
				pathToInput_CBFiles + "/O_CB-Agents_Work.txt",
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Work.xml.gz"};
		CreateCBWork.main(workArgs);

		mergeFacilities(pathToOutput_CBPopulation);
		mergeSubpopulations(transitArgs[5], saArgs[5], pathToOutput_CBPopulation);
		mergeSubpopulations(pathToOutput_CBPopulation, workArgs[6], pathToOutput_CBPopulation);
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
			if (!cbFacilities.getFacilities().containsKey(facility.getId())) cbFacilities.addActivityFacility(facility);
		}
		ActivityFacilities workFacilities = FacilityUtils.readFacilities(
				pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Work_Facilities.xml.gz");
		for (ActivityFacility facility : workFacilities.getFacilities().values()) {
			if (!cbFacilities.getFacilities().containsKey(facility.getId())) cbFacilities.addActivityFacility(facility);
		}

		new FacilitiesWriter(cbFacilities).writeV1(pathToOutput_CBPopulation.substring(0, pathToOutput_CBPopulation.indexOf(".xml")) + "_Facilities.xml.gz");
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
