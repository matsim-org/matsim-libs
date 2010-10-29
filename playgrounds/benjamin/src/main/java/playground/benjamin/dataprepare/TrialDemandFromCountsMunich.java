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
package playground.benjamin.dataprepare;

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

/**
 * @author benjamin
 *
 */
public class TrialDemandFromCountsMunich {
	static String netFile = "";
	static String countsPath = "../../detailedEval/teststrecke/zaehlstellen_einfluss/";
	static String testVehiclePath = "../../detailedEval/teststrecke/testVehicle/";
	static String outPath = "../../detailedEval/teststrecke/sim/input/";
	static String day1 = "20090707";
	static String day2 = "20090708";
	static String day3 = "20090709";


	public static void main(String[] args) {

		//instancing one population for every day
		Population pop1 = generatePopulation(countsPath + day1 + "/");
		Population pop2 = generatePopulation(countsPath + day2 + "/");
		Population pop3 = generatePopulation(countsPath + day3 + "/");
		
		fuzzifyTimes(pop1);
		fuzzifyTimes(pop1);
		fuzzifyTimes(pop1);
		
		addTestVehicle(pop1, testVehiclePath + day1 + "_departureTimes.txt");
//		addTestVehicle(pop2, testVehiclePath + day2 + "_departureTimes.txt");
//		addTestVehicle(pop3, testVehiclePath + day3 + "_departureTimes.txt");		
		
		writePlans(pop1, day1);
		writePlans(pop2, day2);
		writePlans(pop3, day3);
	}

	/**
	 * @param population
	 * @param dayFile 
	 */
	private static void addTestVehicle(Population population, String dayFile) {
		Scenario sc = new ScenarioImpl();

		List<Integer> departureTimes = getTestVehicleDepartureTimes(dayFile);
		for(int time : departureTimes){
			Id personId = sc.createId(time + "_TW");
			Person person = population.getFactory().createPerson(personId);
			Plan plan = population.getFactory().createPlan();
			person.addPlan(plan);

			String actTypeHome = "h";
			String actTypeWork = "h";
			Id linkIdHome = sc.createId("52902684");
			Id linkIdWork = sc.createId("52799758");

			Activity home = population.getFactory().createActivityFromLinkId(actTypeHome, linkIdHome);
			home.setEndTime(time);
			plan.addActivity(home);

			Leg leg = population.getFactory().createLeg(TransportMode.car);
			plan.addLeg(leg);

			Activity work = population.getFactory().createActivityFromLinkId(actTypeWork, linkIdWork);
			plan.addActivity(work);

			population.addPerson(person);
		}
	}

	/**
	 * @param dayFile
	 * @return
	 */
	private static List<Integer> getTestVehicleDepartureTimes(String dayFile) {
		final List<Integer> departureTimes = new ArrayList<Integer>();
		
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(dayFile);
		tabFileParserConfig.setDelimiterTags(new String[] {":"});
		
		try {
			new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

				private static final int HOURS = 0;
				private static final int MINUTES = 1;
				private static final int SECONDS = 2;
				
				public void startRow(String[] row) {
					first = false;
					numColumns = row.length;
					check(row);
					addDepartureTime(row);
				}

				private void addDepartureTime(String[] row) {
					Integer hours = new Integer(row[HOURS]);
					Integer minutes = new Integer(row[MINUTES]);
					Integer seconds = new Integer(row[SECONDS]);
					Integer departureTimeInSeconds = 60 * 60 * hours + 60 * minutes + seconds;
					departureTimes.add(departureTimeInSeconds);
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return departureTimes;
	}

	/**
	 * @param population
	 */
	private static void fuzzifyTimes(Population population) {
		PlanMutateTimeAllocation planMutateTimeAllocation = new PlanMutateTimeAllocation(1 * 60, new Random());
		planMutateTimeAllocation.setUseActivityDurations(false);
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getPlans().iterator().next();
			planMutateTimeAllocation.run(plan);
		}
	}

	/**
	 * @param dayPath
	 * @return
	 */
	private static Population generatePopulation(String dayPath) {
		Scenario sc = new ScenarioImpl();
		Population population = sc.getPopulation();

		String lane1 = dayPath + "4006013.txt";
		String lane2 = dayPath + "4006014.txt";

		SortedMap<Integer, Double> endTime2NoOfVehiclesLane1 = getEndTime2NoOfVehicles(lane1);
		SortedMap<Integer, Double> endTime2NoOfVehiclesLane2 = getEndTime2NoOfVehicles(lane2);
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

	/**
	 * @param endTime2NoOfVehiclesLane1
	 * @param endTime2NoOfVehiclesLane2
	 * @return
	 */
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
				System.out.println(/*"End of time interval: " +*/ endTime + "\t" + /*"Sum of cars: " +*/ vehicelesTotal);
			}
		}
		return aggregatedEndTime2NoOfVehicles;
	}

	/**
	 * @param lane
	 * @return
	 */
	private static SortedMap<Integer, Double> getEndTime2NoOfVehicles(String lane) {
		final SortedMap<Integer, Double> EndTime2NoOfVehicles = new TreeMap<Integer, Double>();

		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(lane);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return EndTime2NoOfVehicles;
	}

	private static void writePlans(Population pop, String day) {
		PopulationWriter populationWriter = new PopulationWriter(pop, null);
		populationWriter.write(outPath + day + "_plans.xml.gz");
	}
}
