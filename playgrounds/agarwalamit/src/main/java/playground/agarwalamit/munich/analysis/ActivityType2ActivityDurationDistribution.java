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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.ActivityType2DurationHandler;
import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * A class to get activity duration distribution for each activity type. 
 * If a person repeats same activity again, it will be counted as second leg.
 * Just use desired constructor with run(...) method.
 * @author amit
 */
public class ActivityType2ActivityDurationDistribution extends AbstractAnalyisModule {

	/**
	 * default simulation start and end times are taken as 00:00:00 and 30:00:00
	 */
	public ActivityType2ActivityDurationDistribution(String outputDir) {
		super(ActivityType2ActivityDurationDistribution.class.getSimpleName());
		simStartTime = 0;
		simEndTime = 30*3600;
		ActivityType2DurationHandler.log.warn("Simulation start time is " +simStartTime+ " and simulation end time is "+simEndTime);
		this.outputDir = outputDir;
	}

	/**
	 * @param simStartTime simulation start time
	 * @param simEndTime simulation end time
	 */
	public ActivityType2ActivityDurationDistribution(double simStartTime, double simEndTime, String outputDir) {
		super(ActivityType2ActivityDurationDistribution.class.getSimpleName());
		this.simStartTime = simStartTime;
		this.simEndTime = simEndTime;
		this.outputDir = outputDir;
	}

	private ActivityType2DurationHandler actDurHandler;
	private double simStartTime;
	private double simEndTime;
	private List<Integer> timeClasses;
	private Map<Id<Person>, Map<String, List<Double>>> personId2ActDurations;
	private SortedMap<String, SortedMap<Integer, Integer>> actType2ActDuration2LegCount;
	private String outputDir;
	private String eventsFile;


	public static void main(String[] args) {
		String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
		String [] runCases = { "baseCaseCtd","ei","ci","eci"};
		new ActivityType2ActivityDurationDistribution(outputDir).run(runCases);
	}


	public void run(String [] runCases){
		for(String runCase:runCases){
			initializeTimeClasses();
			init(runCase);
			String outputConfig = outputDir+runCase+"/output_config.xml";
			getActType2ActDurationDistributionData();
			writeResults(outputDir+runCase+"/analysis/");
			writeTypicalAndMinimumActivityDurations(outputConfig, runCase);
		}
	}

	public void init(String runCase){
		String outputConfig = outputDir+runCase+"/output_config.xml";
		int lastIt = LoadMyScenarios.getLastIteration(outputConfig);
		this.eventsFile = outputDir+runCase+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";

		personId2ActDurations = new HashMap<Id<Person>, Map<String,List<Double>>>();
		actDurHandler = new ActivityType2DurationHandler(this.simEndTime);
		timeClasses = new ArrayList<Integer>();
	
		preProcessData();
		postProcessData();
	}

	/**
	 * writes activity type to typical activity duration to file. 
	 */
	private void writeTypicalAndMinimumActivityDurations(String configFile,String runCase){

		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
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
		ActivityType2DurationHandler.log.info("Data is written to file "+fileName);
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(actDurHandler);
		reader.readFile(eventsFile);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> eh =  new ArrayList<EventHandler>();
		eh.add(actDurHandler);
		return eh;
	}

	@Override
	public void postProcessData() {
		personId2ActDurations = actDurHandler.getPersonId2ActDurations();
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder+"/actTyp2ActDurDistributionDuration.txt";
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
		ActivityType2DurationHandler.log.info("Data is written to file "+fileName);
	}

	private void getActType2ActDurationDistributionData(){
		actType2ActDuration2LegCount = new TreeMap<>();

		//initialize
		for(String actTyp : actDurHandler.getActivityTypes()){
			SortedMap<Integer,Integer> time2LegCount = new TreeMap<>();
			for(int i:timeClasses){
				time2LegCount.put(i, 0);
			}
			actType2ActDuration2LegCount.put(actTyp, time2LegCount);
		}

		PersonFilter pf = new PersonFilter();
		for(Id<Person> id : personId2ActDurations.keySet()){
//			if(pf.isPersonFromMID(id)){
				Map<String, List<Double>> actTyp2Dur = personId2ActDurations.get(id);
				for(String actTyp : actTyp2Dur.keySet()){
					List<Double> durs = actTyp2Dur.get(actTyp);
					for(double d :durs){
						for(int i=0;i<timeClasses.size();i++){
							if(d> timeClasses.get(i)&& d<=timeClasses.get(i+1)){
								SortedMap<Integer,Integer> time2LegCount = actType2ActDuration2LegCount.get(actTyp);
								int countSoFar = time2LegCount.get(timeClasses.get(i+1));
								int newCount = countSoFar+1;
								time2LegCount.put(timeClasses.get(i+1), newCount);
							}
						}
					}
				}
//			}//
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
		ActivityType2DurationHandler.log.info("Following activity duration classes are defined: "+timeClasses.toString());
	}

	public Map<Id<Person>, Map<String, List<Double>>> getPersonId2ActivityType2ActivityDurations(){
		return this.personId2ActDurations;
	}

}
