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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.activity.departureArrival.LegModeDepartureArrivalTimeHandler;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.AbstractAnalysisModule;


/**
 * @author amit
 */
public class LegModeDepartureArrivalTimeDistribution extends AbstractAnalysisModule {

	private final Logger logger = Logger.getLogger(LegModeDepartureArrivalTimeDistribution.class);
	private final LegModeDepartureArrivalTimeHandler lmdah;
	private Map<String, Map<Id<Person>, List<Double> >> mode2PersonId2DepartureTime;
	private final List<Integer> timeStepClasses;
	private final List<String> travelModes;
	private SortedMap<String, Map<Integer, Integer>> mode2DepartureTimeClasses2LegCount;
	private SortedMap<String, Map<Integer, Integer>> mode2ArrivalTimeClasses2LegCount;
	private final String eventsFile;
	private final String configFile;

	public LegModeDepartureArrivalTimeDistribution(String eventsFile, String configFile) {
		super(LegModeDepartureArrivalTimeDistribution.class.getSimpleName());

		this.eventsFile = eventsFile;
		this.configFile=configFile;
		this.timeStepClasses= new ArrayList<>();
		this.travelModes = new ArrayList<>();
		this.lmdah=new LegModeDepartureArrivalTimeHandler();
		this.lmdah.reset(0);
	}

	public static void main(String[] args) {
		String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/";
		final String [] runs = {"run33"};
		//		final String [] runs = {"run113","run114","run115","run116"};

		for(String run:runs){
			final String configFile = runDir+run+"/output_config.xml.gz";
			int lastItr = LoadMyScenarios.getLastIteration(configFile);
			String eventsFile = runDir+run+"/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";

			LegModeDepartureArrivalTimeDistribution lmdatd = new LegModeDepartureArrivalTimeDistribution(eventsFile, configFile);
			lmdatd.preProcessData();
			lmdatd.postProcessData();
			new File(runDir+"/analysis/legModeDistributions/").mkdir();
			lmdatd.writeResults(runDir+"/analysis/legModeDistributions/"+run);
			lmdatd.writeResults(runDir+"/analysisExecutedPlans/legModeDistributions/"+run);
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
		manager.addHandler(this.lmdah);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.mode2PersonId2DepartureTime = this.lmdah.getLegMode2PersonId2DepartureTime();
		initializeTimeStepClasses();
		getTravelModes();
		this.mode2DepartureTimeClasses2LegCount = calculateMode2DepOrArrTimeClases2LegCount(this.mode2PersonId2DepartureTime);
		this.mode2ArrivalTimeClasses2LegCount = calculateMode2DepOrArrTimeClases2LegCount(this.lmdah.getLegMode2PersonId2ArrivalTime());
	}

	private SortedMap<String, Map<Integer, Integer>> calculateMode2DepOrArrTimeClases2LegCount (Map<String, Map<Id<Person>, List<Double> >> mode2personId2DepOrArrTime) {
		SortedMap<String, Map<Integer, Integer>> mode2DepOrArrTime2LegCount= new TreeMap<>();
		for(String mode:this.travelModes){
			SortedMap<Integer, Integer> travelTimeClasses2LegCount = new TreeMap<>();
			for(int i=0;i<this.timeStepClasses.size()-1;i++){
				int legCount =0;
				for(Id<Person> id:mode2personId2DepOrArrTime.get(mode).keySet()){
					List<Double> tt = mode2personId2DepOrArrTime.get(mode).get(id);
					for(double d:tt){
						d=d/(3600);
						if(d > this.timeStepClasses.get(i) && d < this.timeStepClasses.get(i+1)){
							legCount++;
						}
					}
				}
				travelTimeClasses2LegCount.put(this.timeStepClasses.get(i+1), legCount);
			}
			mode2DepOrArrTime2LegCount.put(mode, travelTimeClasses2LegCount);
		}
		return mode2DepOrArrTime2LegCount;
	}

	@Override
	public void writeResults(String outputFolder) {

		writeLegMode2DepOrArrivalTimeDistribution(outputFolder,this.mode2DepartureTimeClasses2LegCount,"Departure");
		writeLegMode2DepOrArrivalTimeDistribution(outputFolder,this.mode2ArrivalTimeClasses2LegCount,"Arrival");
	}

	private void writeLegMode2DepOrArrivalTimeDistribution(String outputFolder, SortedMap<String, Map<Integer, Integer>> inputMap, String depOrArr){

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+".rLegMode"+depOrArr+"TimeDistribution.txt");
		try {
			writer.write("# \t");
			for(String mode:this.travelModes){
				writer.write(mode+"\t");
			}
			writer.newLine();

			for(int i=0; i<this.timeStepClasses.size()-1;i++){
				writer.write(this.timeStepClasses.get(i+1)+"\t");
				for(String mode :this.travelModes){
					writer.write(inputMap.get(mode).get(this.timeStepClasses.get(i+1))+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in File. Reason : "+e);
		}
		this.logger.info("Data is written at "+outputFolder+".rLegMode"+depOrArr+"TimeDistribution.txt");
	}

	private void initializeTimeStepClasses() {
		double simulationEndTime = LoadMyScenarios.getSimulationEndTime(this.configFile);

		for(int endOfTimeStep =0; endOfTimeStep<=(int)simulationEndTime/(2*3600);endOfTimeStep++){
			this.timeStepClasses.add(endOfTimeStep*2);
		}
		this.logger.info("The following time classes were defined: " + this.timeStepClasses);
	}

	private void getTravelModes(){
		this.travelModes.addAll(this.mode2PersonId2DepartureTime.keySet());
		this.logger.info("Travel modes are "+this.travelModes.toString());
	}
}
