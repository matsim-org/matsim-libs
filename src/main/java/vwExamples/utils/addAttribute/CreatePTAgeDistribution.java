/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package vwExamples.utils.addAttribute;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.opencsv.CSVReader;

import vwExamples.utils.DemandFromCSV.Trip;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class CreatePTAgeDistribution {
	String csvAgeDistFile;
	String populationFileOut;
	List<String[]> CSVData;
	Network stopnetwork;
	HashMap<Integer, Double> age2RatioMap;
	Scenario scenario;
	Random p = new Random();

	CreatePTAgeDistribution(String csvAgeDistFile, String populationFileIn, String populationFileOut) {
		this.csvAgeDistFile = csvAgeDistFile;
		this.populationFileOut = populationFileOut;
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationFileIn);

	}

	public static void main(String[] args) {

		CreatePTAgeDistribution createPTAgeDistribution = new CreatePTAgeDistribution(
				"Y:\\ÖV-Abos\\Aggregierte_OEV_Ticket.csv",
				"D:\\Matsim\\Axer\\Hannover\\Base\\vw280_0.1\\vw280_0.1.output_plans.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\Base\\vw280_0.1\\vw280_0.1.output_plans_withTickets.xml.gz");
		createPTAgeDistribution.readAgeCSV();
		createPTAgeDistribution.addPTSeasonTicket();

	}

	public void addPTSeasonTicket() {

		StreamingPopulationWriter modPopulation = new StreamingPopulationWriter();
		modPopulation.startStreaming(populationFileOut);

		int trues = 0;
		int falses = 0;

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Integer actualAge = Integer.parseInt(person.getAttributes().getAttribute("age").toString());
			Double prop2beSeasonTicketUser = age2RatioMap.get(actualAge);

			Boolean hasSeasonTicket = false;
			if (p.nextDouble() < prop2beSeasonTicketUser) {
				hasSeasonTicket = true;
			}

			person.getAttributes().putAttribute("hasSeasonTicket", hasSeasonTicket);
			if (hasSeasonTicket) {
				trues++;
			}
			else {
				falses++;
			}
			modPopulation.writePerson(person);

		}
		modPopulation.closeStreaming();
		System.out.println("Persons with ticket: "+ trues);
		System.out.println("Persons without ticket: "+ falses);

	}

	public void readAgeCSV() {

		this.age2RatioMap = new HashMap<Integer, Double>();
		// CSV HEADER
		// lon,lat,bearing

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(this.csvAgeDistFile));
			CSVData = reader.readAll();
			for (int i = 1; i < CSVData.size(); i++) {
				String[] lineContents = CSVData.get(i);
				int lowerAge = Integer.parseInt(lineContents[0]); // lowerAge,
				int upperAge = Integer.parseInt(lineContents[1]); // upperAge,
				double ratioPT = Double.parseDouble(lineContents[2]); // ratioPT,

				for (int age = lowerAge; age <= upperAge; age++) {
					// System.out.println("age: "+age);
					age2RatioMap.put(age, ratioPT);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
