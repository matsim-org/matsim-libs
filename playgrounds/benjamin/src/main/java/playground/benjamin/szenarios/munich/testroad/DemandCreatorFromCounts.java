/* *********************************************************************** *
 * project: org.matsim.*
 * TrialDemandFromCountsMunich.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;

import playground.benjamin.dataprepare.CheckingTabularFileHandler;

/**
 * @author benjamin
 *
 */
public class DemandCreatorFromCounts {
	static String netFile = "";
	static String countsPath = "../../detailedEval/teststrecke/zaehlstellen_einfluss/";
	static String testVehiclePath = "../../detailedEval/teststrecke/testVehicle/";
	static String outPath = "../../detailedEval/teststrecke/sim/input/";

	static String lane1 = "4006013";
	static String lane2 = "4006014";

	static Integer [] days = {
		// dont use this since they changed counts logfile format during the day!
		//20060125,

		20060127,
		20060131,
		20090317,
		20090318,
		20090319,
		20090707,
		20090708,
		20090709
	};

	public static void main(String[] args) {

		for(int day : days){
			Population population = generatePopulation(countsPath + day + "/" + lane1 + ".txt", countsPath + day + "/" + lane2 + ".txt");

			/*inflow times are randomly equally mutated within a 2min time bin;
			one could think of modelling peak inflows due to upstream signals systems...*/
			fuzzifyTimes(population);

			addTestVehicle(population, testVehiclePath + day + "_travelTimes.csv");
			writePlans(population, day);
		}
	}

	private static void addTestVehicle(Population population, String inflowTimesFile) {
		Scenario sc = new ScenarioImpl();

		List<Integer> inflowTimes = getTestVehicleInflowTimes(inflowTimesFile);
		for(int time : inflowTimes){
			Id personId = sc.createId(time + "testVehicle");

			System.out.println(time);

			Person person = population.getFactory().createPerson(personId);
			Plan plan = population.getFactory().createPlan();
			person.addPlan(plan);

			String actTypeHome = "h";
			String actTypeWork = "h";
			Id linkIdHome = sc.createId("52799702");
			Id linkIdWork = sc.createId("52799758");

			Activity home = population.getFactory().createActivityFromLinkId(actTypeHome, linkIdHome);
			// endTime needs to be set as follows (if my calculation is right :))
			home.setEndTime(time - 12);
			plan.addActivity(home);

			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addLeg(leg);

			Activity work = population.getFactory().createActivityFromLinkId(actTypeWork, linkIdWork);
			plan.addActivity(work);

			population.addPerson(person);
		}
		System.out.println("=========");
	}

	private static List<Integer> getTestVehicleInflowTimes(String inflowTimesFile) {
		final List<Integer> inflowTimes = new ArrayList<Integer>();

		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(inflowTimesFile);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});

		try {
			new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

				private static final int INFLOWTIME = 0;
				private static final int TRAVELTIME = 1;

				public void startRow(String[] row) {
					first = false;
					numColumns = row.length;
					check(row);
					addDepartureTime(row);
				}

				private void addDepartureTime(String[] row) {
					Integer inflowTime = new Integer(row[INFLOWTIME]);
					Integer travelTime = new Integer(row[TRAVELTIME]);
					inflowTimes.add(inflowTime);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return inflowTimes;
	}

	private static void fuzzifyTimes(Population population) {
		PlanMutateTimeAllocation planMutateTimeAllocation = new PlanMutateTimeAllocation(1 * 60, new Random());
		planMutateTimeAllocation.setUseActivityDurations(false);
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getPlans().iterator().next();
			planMutateTimeAllocation.run(plan);
		}
		System.out.println("fuzzified times for population " + population);
	}

	private static Population generatePopulation(String laneFile1, String laneFile2) {
		Scenario sc = new ScenarioImpl();
		Population population = sc.getPopulation();

		SortedMap<Integer, Double> endTime2NoOfVehiclesLane1 = getEndTime2NoOfVehicles(laneFile1);
		SortedMap<Integer, Double> endTime2NoOfVehiclesLane2 = getEndTime2NoOfVehicles(laneFile2);
		SortedMap<Integer, Double> aggregatedEndTime2NoOfVehicles = aggregateVehicles(endTime2NoOfVehiclesLane1, endTime2NoOfVehiclesLane2);

		for(Entry<Integer, Double> entry : aggregatedEndTime2NoOfVehicles.entrySet()){
			Integer endTimeInSeconds = entry.getKey();
			Double vehicelesTotal = entry.getValue();
			for(int i=0; i < vehicelesTotal; i++){

				int j = i+1;
				Id personId = sc.createId(endTimeInSeconds + "_" + j);
				Person person = population.getFactory().createPerson(personId);
				Plan plan = population.getFactory().createPlan();
				person.addPlan(plan);

				String actTypeHome = "h";
				String actTypeWork = "h";
				Id linkIdHome = sc.createId("576273431");
				Id linkIdWork = sc.createId("52799758");

				Activity home = population.getFactory().createActivityFromLinkId(actTypeHome, linkIdHome);
				home.setEndTime(endTimeInSeconds);
				plan.addActivity(home);

				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addLeg(leg);

				Activity work = population.getFactory().createActivityFromLinkId(actTypeWork, linkIdWork);
				plan.addActivity(work);

				population.addPerson(person);
			}
		}
		return population;
	}

	private static SortedMap<Integer, Double> aggregateVehicles(Map<Integer, Double> endTime2NoOfVehiclesLane1, Map<Integer, Double> endTime2NoOfVehiclesLane2) {
		SortedMap<Integer, Double> aggregatedEndTime2NoOfVehicles = new TreeMap<Integer, Double>();

		for(Entry<Integer, Double> entry : endTime2NoOfVehiclesLane1.entrySet()){
			Integer endTime = entry.getKey();
			Double vehiclesLane1 = entry.getValue();
			//find the value for the same key in other map
			Double vehiclesLane2 = endTime2NoOfVehiclesLane2.get(endTime);

			if(vehiclesLane2 == null){
				System.out.println("Couldn't find mapping for key " + endTime );
			}
			else{
				Double vehicelesTotal = vehiclesLane1 + vehiclesLane2;
				aggregatedEndTime2NoOfVehicles.put(endTime, vehicelesTotal);

				//				System.out.println(/*"End of time interval: " +*/ endTime + "\t" + /*"Sum of cars: " +*/ vehicelesTotal);
			}
		}
		return aggregatedEndTime2NoOfVehicles;
	}

	private static SortedMap<Integer, Double> getEndTime2NoOfVehicles(String laneFile) {
		final SortedMap<Integer, Double> EndTime2NoOfVehicles = new TreeMap<Integer, Double>();

		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(laneFile);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		tabFileParserConfig.setCommentTags(new String[] {"#"});

		try {
			new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

				private static final int ENDTIME = 0;
				private static final int NUMBER = 20;

				public void startRow(String[] row) {
					first = false;
					numColumns = row.length;
					check(row);
					//					if(!row[0].startsWith("#")) {
					addEndTimeNoOfVehicles(row);
					//					} else {
					// This is the header. Nothing to do.
					//					}
				}

				private void addEndTimeNoOfVehicles(String[] row) {
					Integer endTime = new Integer(row[ENDTIME]);
					Double NoOfVehicles = new Double(row[NUMBER]);
					EndTime2NoOfVehicles.put(endTime, NoOfVehicles);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return EndTime2NoOfVehicles;
	}

	private static void writePlans(Population pop, int day) {
		PopulationWriter populationWriter = new PopulationWriter(pop, null);
		populationWriter.write(outPath + day + "_plans.xml.gz");
	}
}
