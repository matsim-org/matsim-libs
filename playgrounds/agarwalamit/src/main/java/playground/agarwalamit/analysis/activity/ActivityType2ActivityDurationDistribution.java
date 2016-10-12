/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.activity;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * A class to get activity duration distribution for each activity type. 
 * If a person repeats same activity again, it will be counted as second leg.
 * @author amit
 */
public class ActivityType2ActivityDurationDistribution {
	private ActivityType2ActDurationsAnalyzer actDurAnalyzer;
	private List<Integer> timeClasses;
	private Map<Id<Person>, Map<String, List<Double>>> personId2ActDurations;
	private SortedMap<String, SortedMap<Integer, Integer>> actType2ActDuration2LegCount;
	private final String outputDir;
	private double simEndTime;
	private final boolean sortPersons;
	private final String userGroup;
	
	/**
	 * @param outputDir
	 * Use this to get distribution for whole population
	 */
	public ActivityType2ActivityDurationDistribution(final String outputDir) {
		this.outputDir = outputDir;
		this.sortPersons = false;
		this.userGroup = "";
	}

	/**
	 * @param outputDir
	 * @param userGroup for which distribution is required
	 */
	public ActivityType2ActivityDurationDistribution(final String outputDir, final UserGroup userGroup) {
		this.outputDir = outputDir;
		this.sortPersons = true;
		this.userGroup = userGroup.toString();
		ActivityType2DurationHandler.LOG.warn("Result will consider persons from "+this.userGroup+" sub population group.");
	}

	public static void main(String[] args) {
		String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
		String [] runCases = { "baseCaseCtd","ei","ci","eci"};
		new ActivityType2ActivityDurationDistribution(outputDir,UserGroup.URBAN).run(runCases);
	}

	public void run(final String [] runCases){
		for(String runCase:runCases){
			init(runCase);
		}
	}

	public void init(final String runCase){

		actDurAnalyzer = new ActivityType2ActDurationsAnalyzer(outputDir+runCase);
		actDurAnalyzer.preProcessData();
		actDurAnalyzer.postProcessData();
		personId2ActDurations = actDurAnalyzer.getPersonId2ActivityType2ActivityDurations();

		String outputConfig = outputDir+runCase+"/output_config.xml";
		simEndTime = LoadMyScenarios.getSimulationEndTime(outputConfig);

		timeClasses = new ArrayList<>();
		initializeTimeClasses();

		getActType2ActDurationDistributionData();

		writeResults(outputDir+runCase+"/analysis/");
		writeTypicalAndMinimumActivityDurations(outputConfig);
	}

	/**
	 * writes activity type to typical activity duration to file. 
	 */
	private void writeTypicalAndMinimumActivityDurations(final String configFile){

		Config config = new Config();
		config.addCoreModules();
		ConfigReader reader = new ConfigReader(config);
		reader.readFile(configFile);

		SortedMap<String, Double> act2TypDur = new TreeMap<>();
		SortedMap<String, Double> act2MinDur = new TreeMap<>();

		for (String actTyp :config.planCalcScore().getActivityTypes()){
			act2TypDur.put(actTyp, config.planCalcScore().getActivityParams(actTyp).getTypicalDuration());
			act2MinDur.put(actTyp, config.planCalcScore().getActivityParams(actTyp).getMinimalDuration());
		}

		String fileName = outputDir+"/analysis/actTyp2TypicalAndMinimumActDurations.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("actType \t typicalActDuration \t minimumActDuration \n");
			for (String actTyp :act2MinDur.keySet()){
				writer.write(actTyp+"\t"+act2TypDur.get(actTyp)+"\t"
						+act2MinDur.get(actTyp)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}
		ActivityType2DurationHandler.LOG.info("Data is written to file "+fileName);
	}

	public void writeResults(final String outputFolder) {
		String fileName = outputFolder+"/"+userGroup+"actTyp2ActDurDistributionDuration.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("timeIndex \t ");
			for(String actType : actType2ActDuration2LegCount.keySet()){
				writer.write(actType+"\t");
			}
			writer.newLine();

			for(int i:timeClasses){
				writer.write(i+"\t");
				for(String actType : actType2ActDuration2LegCount.keySet()){
					writer.write(actType2ActDuration2LegCount.get(actType).get(i)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}
		ActivityType2DurationHandler.LOG.info("Data is written to file "+fileName);
	}

	private void getActType2ActDurationDistributionData(){
		actType2ActDuration2LegCount = new TreeMap<>();

		//initialize
		for(String actTyp : actDurAnalyzer.getActivityTypes()){
			SortedMap<Integer,Integer> time2LegCount = new TreeMap<>();
			for(int i:timeClasses){
				time2LegCount.put(i, 0);
			}
			actType2ActDuration2LegCount.put(actTyp, time2LegCount);
		}

		PersonFilter pf = new PersonFilter();
		for(Id<Person> id : personId2ActDurations.keySet()){
			if(sortPersons ){
				if(pf.isPersonIdFromUserGroup(id, UserGroup.valueOf(userGroup))){
					storeData(id);
				}
			} else {
				storeData(id);
			}
		}
	}

	private void storeData(final Id<Person> id){
		Map<String, List<Double>> actTyp2Dur = personId2ActDurations.get(id);
		for(String actTyp : actTyp2Dur.keySet()){
			List<Double> durs = actTyp2Dur.get(actTyp);
			for(double d :durs){
				for(int i=0;i<timeClasses.size()-1;i++){
					if(d> timeClasses.get(i)&& d<=timeClasses.get(i+1)){
						SortedMap<Integer,Integer> time2LegCount = actType2ActDuration2LegCount.get(actTyp);
						int countSoFar = time2LegCount.get(timeClasses.get(i+1));
						int newCount = countSoFar+1;
						time2LegCount.put(timeClasses.get(i+1), newCount);
					}
				}
			}
		}
	}
	private void initializeTimeClasses(){
		int endOfTimeClass =0;
		int classCounter = 0;
		timeClasses.add(endOfTimeClass);

		while(endOfTimeClass <= simEndTime){
			endOfTimeClass = 100* (int) Math.pow(2, classCounter);
			classCounter++;
			timeClasses.add(endOfTimeClass);
		}
		ActivityType2DurationHandler.LOG.info("Following activity duration classes are defined: "+timeClasses.toString());
	}

	public Map<Id<Person>, Map<String, List<Double>>> getPersonId2ActivityType2ActivityDurations(){
		return this.personId2ActDurations;
	}
}