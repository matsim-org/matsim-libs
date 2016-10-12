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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * A class to get the distribution of activity duration differences.
 * @author amit
 */
public class ActDurationDiffDistribution {
	private final String outputDir;
	private final String bau;
	private Map<Id<Person>, Map<String, List<Double>>> personId2ActDurationDiff;
	private List<Double> allDurDiffs;
	private List<Integer> timeClasses;
	private Set<String> actTypes ;
	private SortedMap<String, SortedMap<Integer, Integer>> actType2ActDurationDiff2LegCount;
	private final boolean sortPersons;
	private final String userGroup;

	/**
	 * @param outputDir
	 * @param bau business as usual scenario or baseCaseCtd or initial scenario
	 * Use this to get distribution for whole population
	 */
	public ActDurationDiffDistribution(final String outputDir, final String bau) {
		this.outputDir = outputDir;
		this.bau = bau;
		this.sortPersons = false;
		this.userGroup ="";
	}
	
	/**
	 * @param outputDir
	 * @param bau business as usual scenario name or run number for baseCaseCtd
	 * @param userGroup for which distribution is required
	 */
	public ActDurationDiffDistribution(final String outputDir, final String bau, final UserGroup userGroup) {
		this.outputDir = outputDir;
		this.bau = bau;
		this.sortPersons = true;
		this.userGroup = userGroup.toString();
		ActivityType2DurationHandler.LOG.warn("Result will consider persons from "+this.userGroup+" sub population group.");
	}

	public static void main(String[] args) {
		String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
		String bau = "baseCaseCtd";
		String runCases [] = {"ei","ci","eci"};
		for(String runCase:runCases){
			new ActDurationDiffDistribution(outputDir,bau).run(runCase);
		}
	}

	public void run (final String runCase){
		personId2ActDurationDiff = new HashMap<>();
		allDurDiffs = new ArrayList<>();
		timeClasses = new ArrayList<>();
		actType2ActDurationDiff2LegCount = new TreeMap<>();
		actTypes = new HashSet<>();

		String configFile = outputDir+runCase+"/output_config.xml";

		getPersonId2ActDurationDiff(bau, runCase);

		initializeTimeClasses();
		getActType2ActDurationDiffDistributionData();
		writeResults(runCase);
		writeTypicalAndMinimumActivityDurations(configFile);
	}
	
	/**
	 * @param bau initial scenario
	 * @param runCase policy scenario
	 */
	private void getPersonId2ActDurationDiff(final String bau, final String runCase){
		Map<Id<Person>, Map<String, List<Double>>> bauPerson2ActDurations = getPersonId2ActType2ActDurations(bau);
		Map<Id<Person>, Map<String, List<Double>>> runCasePerson2ActDurations = getPersonId2ActType2ActDurations(runCase);

		for (Id<Person> personId : bauPerson2ActDurations.keySet()){
			Map<String, List<Double>> actType2DurationDiff = new HashMap<>();
			for(String actType : bauPerson2ActDurations.get(personId).keySet()){
				actTypes.add(actType);
				List<Double> actDurDiff = new ArrayList<>();
				for(int i = 0; i< bauPerson2ActDurations.get(personId).get(actType).size();i++){
					double bauDur = bauPerson2ActDurations.get(personId).get(actType).get(i);
					double runCaseDur = runCasePerson2ActDurations.get(personId).get(actType).get(i);
					double durDiff = runCaseDur-bauDur;
					actDurDiff.add(durDiff);
					allDurDiffs.add(durDiff);

				}
				actType2DurationDiff.put(actType,actDurDiff);
			}
			personId2ActDurationDiff.put(personId, actType2DurationDiff);
		}
	}

	private Map<Id<Person>, Map<String, List<Double>>> getPersonId2ActType2ActDurations(final String run){
		ActivityType2ActDurationsAnalyzer actDurAnalyzer = new ActivityType2ActDurationsAnalyzer(outputDir+run);
		actDurAnalyzer.preProcessData();
		actDurAnalyzer.postProcessData();
		return actDurAnalyzer.getPersonId2ActivityType2ActivityDurations();
	}

	private void writeResults(final String runCase) {
		String fileName = outputDir+"/analysis/"+runCase+userGroup+"_actTyp2ActDur_Diff_DistributionDuration.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("timeIndex \t ");
			for(String actType : actType2ActDurationDiff2LegCount.keySet()){
				writer.write(actType+"\t");
			}
			writer.newLine();

			for(int i:timeClasses){
				writer.write(i+"\t");
				for(String actType : actType2ActDurationDiff2LegCount.keySet()){
					writer.write(actType2ActDurationDiff2LegCount.get(actType).get(i)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}
		ActivityType2DurationHandler.LOG.info("Data is written to file "+fileName);
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
	
	private void getActType2ActDurationDiffDistributionData(){
		actType2ActDurationDiff2LegCount = new TreeMap<>();

		//initialize
		for(String actTyp : actTypes){
			SortedMap<Integer,Integer> time2LegCount = new TreeMap<>();
			for(int i:timeClasses){
				time2LegCount.put(i, 0);
			}
			actType2ActDurationDiff2LegCount.put(actTyp, time2LegCount);
		}

		PersonFilter pf = new PersonFilter();
		for(Id<Person> id : personId2ActDurationDiff.keySet()){
			if(sortPersons ){
				if(pf.isPersonIdFromUserGroup(id, UserGroup.valueOf(userGroup))){
					storeData(id);
				}
			} else {
				storeData(id);
			}
		}
	}

	private void storeData(final Id<Person> id) {
		Map<String, List<Double>> actTyp2Dur = personId2ActDurationDiff.get(id);
		for(String actTyp : actTyp2Dur.keySet()){
			List<Double> durs = actTyp2Dur.get(actTyp);
			for(double d :durs){
				for(int i=0;i<timeClasses.size();i++){
					if(d> timeClasses.get(i)&& d<=timeClasses.get(i+1)){
						SortedMap<Integer,Integer> time2LegCount = actType2ActDurationDiff2LegCount.get(actTyp);
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
		double minTime = Collections.min(allDurDiffs);
		double maxTime = Collections.max(allDurDiffs);

		timeClasses.add(endOfTimeClass);

		while(endOfTimeClass <= maxTime){
			endOfTimeClass = 100* (int) Math.pow(2, classCounter);
			classCounter++;
			timeClasses.add(endOfTimeClass);
		}

		endOfTimeClass = 0;
		classCounter =0;
		while(endOfTimeClass <= -minTime){
			endOfTimeClass = 100* (int) Math.pow(2, classCounter);
			classCounter++;
			timeClasses.add(-endOfTimeClass);
		}
		Collections.sort(timeClasses);
		ActivityType2DurationHandler.LOG.info("Following activity duration classes are defined: "+timeClasses.toString());
	}
}