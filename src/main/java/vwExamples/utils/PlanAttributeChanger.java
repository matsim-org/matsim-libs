/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author saxer
 */
public class PlanAttributeChanger {
	HashSet<String> validPrimaryActs = new HashSet<String>();
	HashSet<String> bridgingActs = new HashSet<String>();
	String populationInputFile;
	String populationOutPutFile;

	Map<String, Geometry> zoneMap = new HashMap<>();
	Set<String> zones = new HashSet<>();
	String shapeFile;
	String shapeFeature = "NO";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	StreamingPopulationWriter popwriter;
	long nr = 1896;
	Random r = MatsimRandom.getRandom();

	PlanAttributeChanger(String populationInputFile, String populationOutPutFile) {
		this.populationInputFile = populationInputFile;
		this.populationOutPutFile = populationOutPutFile;

		this.popwriter = new StreamingPopulationWriter();
		popwriter.startStreaming(this.populationOutPutFile);

		new PopulationReader(scenario).readFile(this.populationInputFile);

	}

	public static void main(String[] args) {

		PlanAttributeChanger manipulator = new PlanAttributeChanger(
				"Y:\\aws\\vw280_100pct\\input\\plans\\finishedPlans_1.0_timeFIX.xml.gz",
				"Y:\\\\aws\\\\vw280_100pct\\\\input\\\\plans\\\\finishedPlans_1.0_timeFIX_License.xml.gz");

		manipulator.run();

	}

	public void run() {
		manipulateCandidates();
		writePopulation();

	}

	public void writePopulation() {
		for (Person person : scenario.getPopulation().getPersons().values())

		{
			PersonUtils.removeUnselectedPlans(person);
			this.popwriter.writePerson(person);
		}

		this.popwriter.closeStreaming();
	}

	public void manipulateCandidates() {

		for (Person person : scenario.getPopulation().getPersons().values()) {

			double randomRatio = r.nextDouble();

			int age = (int) person.getAttributes().getAttribute("age");
			String sex = (String) person.getAttributes().getAttribute("sex");

			if (age < 17 && age >= 0) {

				person.getAttributes().putAttribute("hasLicense", "no");
				person.getAttributes().putAttribute("carAvail", "never");
				continue;
			}


			if (age > 17 && age < 75) {

				if (sex.contains("male") && randomRatio < 0.1) {
					person.getAttributes().putAttribute("hasLicense", "no");
					person.getAttributes().putAttribute("carAvail", "never");
					continue;
				}

				else if (sex.contains("male") && randomRatio >= 0.1) {
					person.getAttributes().putAttribute("hasLicense", "yes");
					person.getAttributes().putAttribute("carAvail", "yes");
					continue;
				}

				else if (sex.contains("female") && randomRatio < 0.1) {
					person.getAttributes().putAttribute("hasLicense", "no");
					person.getAttributes().putAttribute("carAvail", "never");
					continue;
				} else if (sex.contains("female") && randomRatio >= 0.1) {
					person.getAttributes().putAttribute("hasLicense", "yes");
					person.getAttributes().putAttribute("carAvail", "yes");
					continue;
				}

			} else if (age >= 75) {

				if (sex.contains("male") && randomRatio < 0.1) {
					person.getAttributes().putAttribute("hasLicense", "no");
					person.getAttributes().putAttribute("carAvail", "never");
					continue;
				}

				else if (sex.contains("male") && randomRatio >= 0.1) {
					person.getAttributes().putAttribute("hasLicense", "yes");
					person.getAttributes().putAttribute("carAvail", "yes");
					continue;
				}

				else if (sex.contains("female") && randomRatio < 0.3) {
					person.getAttributes().putAttribute("hasLicense", "no");
					person.getAttributes().putAttribute("carAvail", "never");
					continue;
				} else if (sex.contains("female") && randomRatio >= 0.3) {
					person.getAttributes().putAttribute("hasLicense", "yes");
					person.getAttributes().putAttribute("carAvail", "yes");
					continue;
				}

			}

		}

	}

}
