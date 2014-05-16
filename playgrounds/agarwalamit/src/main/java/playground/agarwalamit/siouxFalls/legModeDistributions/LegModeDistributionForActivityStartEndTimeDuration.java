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
package playground.agarwalamit.siouxFalls.legModeDistributions;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalyisModule;


/**
 * @author amit
 */
public class LegModeDistributionForActivityStartEndTimeDuration extends AbstractAnalyisModule {

	private final Logger logger = Logger.getLogger(LegModeDistributionForActivityStartEndTimeDuration.class);
	private ActivityStartEndTimeAndDurationHandler actStrEndur;
	private Map<String, Map<Id, Double>> actTyp2PersonId2ActStartTime;
	private Map<String, Map<Id, Double>> actTypPersonId2ActEndTime;
	private Map<Id, String> personId2LegMode ;
	private List<Integer> timeStepClasses;
	private List<String> travelModes;
	private SortedMap<String, Map<String, Map<Integer, Integer>>> actType2Mode2ActStartTimeClasses2LegCount;
	private SortedMap<String, Map<String, Map<Integer, Integer>>> actType2Mode2ActEndTimeClasses2LegCount;
	private SortedMap<String, Map<String, Map<Integer, Integer>>> actType2Mode2ActDurationClasses2LegCount;
	private String eventsFile;
	private String configFile;

	public LegModeDistributionForActivityStartEndTimeDuration(String eventsFile, String configFile) {
		super(LegModeDistributionForActivityStartEndTimeDuration.class.getSimpleName());

		this.eventsFile = eventsFile;
		this.configFile=configFile;
		this.timeStepClasses=new ArrayList<Integer>();
		this.travelModes = new ArrayList<String>();
		this.actStrEndur=new ActivityStartEndTimeAndDurationHandler();
	}

	public static void main(String[] args) {
		String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/";
		//		String [] runs = {"run33"};
		String [] runs = {"run105","run106","run107","run108"};

		for(String run:runs){
			String eventsFile = runDir+run+"/ITERS/it.100/100.events.xml.gz";
			String configFile = runDir+run+"/output_config.xml.gz";
			LegModeDistributionForActivityStartEndTimeDuration lmdatd = new LegModeDistributionForActivityStartEndTimeDuration(eventsFile, configFile);
			lmdatd.preProcessData();
			lmdatd.postProcessData();
			new File(runDir+run+"/analysis/legModeDistribution/").mkdir();
			lmdatd.writeResults(runDir+run+"/analysis/legModeDistribution/");
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(this.actStrEndur);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.actTyp2PersonId2ActStartTime = this.actStrEndur.getActivityType2PersonId2ActStartTime();
		this.actTypPersonId2ActEndTime=this.actStrEndur.getActivityType2PersonId2ActEndTime();
		this.personId2LegMode= this.actStrEndur.getPersonId2LegMode();
		initializeTimeStepClasses();
		getTravelModes();
		this.actType2Mode2ActStartTimeClasses2LegCount = calculateMode2DepOrArrTimeClases2LegCount(this.actTyp2PersonId2ActStartTime);
		this.actType2Mode2ActEndTimeClasses2LegCount = calculateMode2DepOrArrTimeClases2LegCount(this.actTypPersonId2ActEndTime);
		this.actType2Mode2ActDurationClasses2LegCount = calculateMode2DepOrArrTimeClases2LegCount(this.actStrEndur.getActivityType2PersonId2ActDuration());
	}

	private SortedMap<String, Map<String, Map<Integer, Integer>>> calculateMode2DepOrArrTimeClases2LegCount (Map<String, Map<Id, Double>> actType2personId2StartOrEndTime) {
		SortedMap<String, Map<String, Map<Integer, Integer>>> actType2Mode2ActStartOrEndTime2LegCount= new TreeMap<String, Map<String,Map<Integer,Integer>>>();

		for(String actType:actType2personId2StartOrEndTime.keySet()){
			SortedMap<String, Map<Integer, Integer>> mode2TimeClasses2LegCount = new TreeMap<String, Map<Integer,Integer>>();
			for(String travelMode:this.travelModes){
				Map<Integer, Integer> timeClassesToLegCount = new HashMap<Integer, Integer>();
				for(Integer i:this.timeStepClasses){
					timeClassesToLegCount.put(i, 0);
				}
				mode2TimeClasses2LegCount.put(travelMode, timeClassesToLegCount);
			}
			actType2Mode2ActStartOrEndTime2LegCount.put(actType, mode2TimeClasses2LegCount);

			for(Id personId:actType2personId2StartOrEndTime.get(actType).keySet()){
				String mode = this.personId2LegMode.get(personId);
				double time = (actType2personId2StartOrEndTime.get(actType).get(personId))/(2*3600);
				Map<Integer, Integer> timeClassToLegCount = mode2TimeClasses2LegCount.get(mode);
				for(int j=0; j< this.timeStepClasses.size()-1;j++){
					timeClassToLegCount = actType2Mode2ActStartOrEndTime2LegCount.get(actType).get(mode);

					if(time>this.timeStepClasses.get(j) && time<timeStepClasses.get(j+1)){
						Integer countSoFar = timeClassToLegCount.get(j);
						Integer newCount = countSoFar+1;
						timeClassToLegCount.put(j, newCount);
					}
				}
				mode2TimeClasses2LegCount.put(mode, timeClassToLegCount);
			}

			actType2Mode2ActStartOrEndTime2LegCount.put(actType, mode2TimeClasses2LegCount);
		}
		return actType2Mode2ActStartOrEndTime2LegCount;
	}

	@Override
	public void writeResults(String outputFolder) {
		
		writeActType2LegMode2TimeClass2LegCountDistribution(outputFolder, actType2Mode2ActStartTimeClasses2LegCount, "Start");
		writeActType2LegMode2TimeClass2LegCountDistribution(outputFolder, actType2Mode2ActEndTimeClasses2LegCount, "End");
		writeActType2LegMode2TimeClass2LegCountDistribution(outputFolder, actType2Mode2ActDurationClasses2LegCount, "Duration");
		
	}
	
	private void writeActType2LegMode2TimeClass2LegCountDistribution(String outputFolder,SortedMap<String, Map<String, Map<Integer, Integer>>> inputMap,String actStartActEndOrActDuration){
		BufferedWriter writer;

		for(String actTyp:inputMap.keySet()){
			writer = IOUtils.getBufferedWriter(outputFolder+"r"+actTyp+"Act2Mode2Act"+actStartActEndOrActDuration+"Distribution.txt");
			try {
				writer.write("# \t");
				for(String mode:this.travelModes){
					writer.write(mode+"\t");
				}
				writer.newLine();

				for(int i=0; i<this.timeStepClasses.size()-1;i++){
					writer.write(this.timeStepClasses.get(i+1)+"\t");
					for(String mode :this.travelModes){
						writer.write(inputMap.get(actTyp).get(mode).get(this.timeStepClasses.get(i+1))+"\t");
					}
					writer.newLine();
				}
				writer.close();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written in File. Reason : "+e);
			}
			logger.info("Data file is writted at "+outputFolder+".r"+actTyp+"Act2Mode2Act"+actStartActEndOrActDuration+"Distribution.txt");
		}
	}
	
	private void initializeTimeStepClasses() {
		double simulationEndTime = getSimulationEndTime();

		for(int endOfTimeStep =0; endOfTimeStep<=(int)simulationEndTime/(2*3600);endOfTimeStep++){
			this.timeStepClasses.add(endOfTimeStep);
		}

		logger.info("The following time classes were defined: " + this.timeStepClasses);
	}

	private double getSimulationEndTime(){
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader reader= new MatsimConfigReader(config);
		reader.readFile(this.configFile);
		return config.qsim().getEndTime();
	}
	
	private void getTravelModes(){
		for(Id pId:this.personId2LegMode.keySet()){
			if(this.travelModes.contains(this.personId2LegMode.get(pId))){

			} else {
				this.travelModes.add(this.personId2LegMode.get(pId));
			}
		}
		logger.info("Travel modes are "+this.travelModes.toString());
	}
}
