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

package playground.farzadalemi.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

/**
 * Template for a network converter
 *
 * @author boescpa
 */
public class TemplatePopulationConverter {

	private final Population newPopulation;
	private final PopulationFactory factory;

	private TemplatePopulationConverter() {
		newPopulation = PopulationUtils.createPopulation(new Config());
		factory = newPopulation.getFactory();
	}

	public static void main(final String[] args) {
		String pathToInputFile = args[0];
		String delimiter = args[1]; // the delimiter you use in your input file, for example ";"
		String pathToOutputFile = args[2]; // something like C:\MATSimStuff\population.xml.gz


		TemplatePopulationConverter populationConverter = new TemplatePopulationConverter();
		populationConverter.readPopulation(pathToInputFile, delimiter);
		populationConverter.writeOutputFile(pathToOutputFile);
		populationConverter.testPopulation(pathToOutputFile);
	}

	/**
	 * This is the population reading method you have to adapt to your input file...
	 */
	private void readPopulation(String pathToInputFile, String delimiter) {
		BufferedReader fileReader = IOUtils.getBufferedReader(pathToInputFile);
		try {
			String line = fileReader.readLine(); // read first line
			while (line != null) {
				String[] lineArgs = line.split(delimiter);
				if (lineArgs[0].equals("person")) {
					String personId = lineArgs[1];
					String sex = lineArgs[2];
					int age = Integer.parseInt(lineArgs[3]);
					String licence = lineArgs[4];
					boolean employed = Boolean.parseBoolean(lineArgs[5]);
					String carAvail = lineArgs[6];

					// create person
					Person newPerson = factory.createPerson(Id.createPersonId(personId));
					PersonUtils.setSex(newPerson, sex);
					PersonUtils.setAge(newPerson, age);
					PersonUtils.setLicence(newPerson, licence);
					PersonUtils.setEmployed(newPerson, employed);
					PersonUtils.setCarAvail(newPerson, carAvail);

					// create initial plan
					Plan plan = factory.createPlan();
					int i = 7;
					while (i < lineArgs.length) {
						if (i%2 != 0) {
							String activityType = lineArgs[i++];
							double xCoord = Double.parseDouble(lineArgs[i++]);
							double yCoord = Double.parseDouble(lineArgs[i++]);
							double startTime = Double.parseDouble(lineArgs[i++]); // in seconds from midnight
							double endTime = Double.parseDouble(lineArgs[i++]); // in seconds from midnight
							Activity act = new ActivityImpl(activityType, new Coord(xCoord, yCoord));
							act.setStartTime(startTime);
							act.setEndTime(endTime);
							plan.addActivity(act);
						} else {
							String transportMode = lineArgs[i++];
							Leg leg = new LegImpl(transportMode);
							plan.addLeg(leg);
						}
					}
					newPerson.addPlan(plan);
					newPopulation.addPerson(newPerson);
				} else {
					// header -> do nothing...
				}
				line = fileReader.readLine();
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutputFile(String pathToOutputFile) {
		new PopulationWriter(newPopulation).write(pathToOutputFile);
	}

	/**
	 * If the written population can be read with the population reader without error messages to the user,
	 * this is a first indication that the transformation was (technically) successful.
	 */
	private void testPopulation(String pathToOutputFile) {
		new PopulationReaderMatsimV5(ScenarioUtils.createScenario(ConfigUtils.createConfig())).readFile(pathToOutputFile);
	}
}
