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
public class TestDemandCreatorFromCounts {
	static String netFile = "";
	static String countsPath = "../../detailedEval/teststrecke/zaehlstellen_einfluss/";
	static String testVehiclePath = "../../detailedEval/teststrecke/testVehicle/";
	static String outPath = "../../detailedEval/teststrecke/sim/input/";
	static String day1 = "20090707";
	static String day2 = "20090708";
	static String day3 = "20090709";
	
	static String lane1 = "4006013";
	static String lane2 = "4006014";


	public static void main(String[] args) {

		//instancing one population for every day
		Population pop1 = generatePopulation(countsPath + day1 + "/" + lane1 + ".txt", countsPath + day1 + "/" + lane2 + ".txt");
		Population pop2 = generatePopulation(countsPath + day2 + "/" + lane1 + ".txt", countsPath + day2 + "/" + lane2 + ".txt");
		Population pop3 = generatePopulation(countsPath + day3 + "/" + lane1 + ".txt", countsPath + day3 + "/" + lane2 + ".txt");
		
		fuzzifyTimes(pop1);
		fuzzifyTimes(pop2);
		fuzzifyTimes(pop3);
		
		addTestVehicle(pop1, testVehiclePath + day1 + "_inflowTimes.txt");
		addTestVehicle(pop2, testVehiclePath + day2 + "_inflowTimes.txt");
		addTestVehicle(pop3, testVehiclePath + day3 + "_inflowTimes.txt");		
		
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

		List<Integer> inflowTimes = getTestVehicleInflowTimes(dayFile);
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

	/**
	 * @param dayFile
	 * @return
	 */
	private static List<Integer> getTestVehicleInflowTimes(String dayFile) {
		final List<Integer> inflowTimes = new ArrayList<Integer>();
		
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
					inflowTimes.add(departureTimeInSeconds);
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return inflowTimes;
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
//		System.out.println("fuzzified times for population");
	}

	/**
	 * @param dayPath
	 * @param laneFile2 
	 * @param laneFile1 
	 * @return
	 */
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
				
//				System.out.println(/*"End of time interval: " +*/ endTime + "\t" + /*"Sum of cars: " +*/ vehicelesTotal);
			}
		}
		return aggregatedEndTime2NoOfVehicles;
	}

	/**
	 * @param lane
	 * @return
	 */
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
