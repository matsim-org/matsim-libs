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
import java.util.ArrayList;
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

import playground.agarwalamit.analysis.legModeHandler.LegModeTravelTimeHandler;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */
public class LegModeTravelTimeDistribution extends AbstractAnalyisModule {

	private final Logger logger = Logger.getLogger(LegModeTravelTimeDistribution.class);
	private LegModeTravelTimeHandler lmth;
	private Map<String, Map<Id, Double>> mode2PersonId2TravelTime;
	private List<Integer> travelTimeClasses;
	private List<String> travelModes;
	private SortedMap<String, Map<Integer, Integer>> mode2TravelTimeClasses2LegCount;
	private String eventsFile;

	public LegModeTravelTimeDistribution(String eventsFile) {
		super(LegModeTravelTimeDistribution.class.getSimpleName());

		this.eventsFile = eventsFile;
		this.travelTimeClasses=new ArrayList<Integer>();
		this.travelModes = new ArrayList<String>();
		this.lmth=new LegModeTravelTimeHandler();
		this.lmth.reset(0);
	}

	public static void main(String[] args) {
		String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/";
		String [] runs = {"run33"};
		//		String [] runs = {"run113","run114","run115","run116"};

		for(String run:runs){
		
			String configFile = runDir+run+"/output_config.xml.gz";
			int lastItr = (int)getLastIteration(configFile);
			String eventsFile = runDir+run+"/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";
			
			LegModeTravelTimeDistribution lmttd = new LegModeTravelTimeDistribution(eventsFile);
			lmttd.preProcessData();
			lmttd.postProcessData();
			lmttd.writeResults(runDir+"/analysis/legModeDistributions/"+run);
			lmttd.writeResults(runDir+"/analysisExecutedPlans/legModeDistributions/"+run);
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
		manager.addHandler(this.lmth);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.mode2PersonId2TravelTime = this.lmth.getLegMode2PersonId2TravelTime();
		initializeTravelTimeClasses();
		getTravelModes();
		calculateMode2TravelTimeClases2LegCount();
	}

	private void calculateMode2TravelTimeClases2LegCount() {
		this.mode2TravelTimeClasses2LegCount= new TreeMap<String, Map<Integer,Integer>>();
		for(String mode:this.travelModes){
			SortedMap<Integer, Integer> travelTimeClasses2LegCount = new TreeMap<Integer, Integer>();
			for(int i=0;i<this.travelTimeClasses.size()-1;i++){
				int legCount =0;
				for(Id id:this.mode2PersonId2TravelTime.get(mode).keySet()){
					double tt = this.mode2PersonId2TravelTime.get(mode).get(id);
					if(tt>this.travelTimeClasses.get(i)&&tt<this.travelTimeClasses.get(i+1)){
						legCount++;
					}
				}
				travelTimeClasses2LegCount.put(this.travelTimeClasses.get(i+1), legCount);
			}
			this.mode2TravelTimeClasses2LegCount.put(mode, travelTimeClasses2LegCount);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+".rLegModeTravelTimeDistribution.txt");
		try {
			writer.write("# \t");
			for(String mode:this.travelModes){
				writer.write(mode+"\t");
			}
			writer.newLine();

			for(int i=0; i<this.travelTimeClasses.size()-1;i++){
				writer.write(this.travelTimeClasses.get(i+1)+"\t");
				for(String mode :this.travelModes){
					writer.write(this.mode2TravelTimeClasses2LegCount.get(mode).get(this.travelTimeClasses.get(i+1))+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in File. Reason : "+e);
		}
		logger.info("Files have been written to "+outputFolder+".rLegModeTravelTimeDistribution.txt");
	}
	private void initializeTravelTimeClasses() {
		double highestTravelTime = getHighestTravelTime();
		int endOfTravelTimeClass = 0;
		int classCounter = 0;
		this.travelTimeClasses.add(endOfTravelTimeClass);

		while(endOfTravelTimeClass <= highestTravelTime){
			endOfTravelTimeClass = 100 * (int) Math.pow(2, classCounter);
			classCounter++;
			this.travelTimeClasses.add(endOfTravelTimeClass);
		}
		logger.info("The following travel time classes were defined: " + this.travelTimeClasses);
	}

	private double getHighestTravelTime(){
		double highestTravelTime = 0;
		for(String mode : this.mode2PersonId2TravelTime.keySet()){
			for(Id id : this.mode2PersonId2TravelTime.get(mode).keySet()){
				if(highestTravelTime<this.mode2PersonId2TravelTime.get(mode).get(id)){
					highestTravelTime = this.mode2PersonId2TravelTime.get(mode).get(id);
				}
			}
		}
		return highestTravelTime;
	}
	private void getTravelModes(){
		this.travelModes.addAll(this.mode2PersonId2TravelTime.keySet());
	}
	private static double getLastIteration(String configFile){
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader reader= new MatsimConfigReader(config);
		reader.readFile(configFile);
		return config.controler().getLastIteration();
	}
}
