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
package playground.agarwalamit.munich.calibration;

import java.io.BufferedWriter;
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

import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 */
public class LegModeTravelTimeDistribution extends AbstractAnalysisModule {

	private final Logger logger = Logger.getLogger(LegModeTravelTimeDistribution.class);
	private ModalTripTravelTimeHandler lmth;
	private Map<String, Map<Id<Person>, List<Double>>> mode2PersonId2TravelTimes;
	private List<Integer> travelTimeClasses;
	private List<String> travelModes;
	private SortedMap<String, SortedMap<Integer, Integer>> mode2TravelTimeClasses2LegCount;
	private String eventsFile;
	private UserGroup userGroup = null;

	public LegModeTravelTimeDistribution(String eventsFile, UserGroup userGroup) {
		super(LegModeTravelTimeDistribution.class.getSimpleName());

		this.eventsFile = eventsFile;
		this.travelTimeClasses=new ArrayList<Integer>();
		this.travelModes = new ArrayList<String>();
		this.lmth=new ModalTripTravelTimeHandler();
		this.lmth.reset(0);
		this.userGroup = userGroup;
	}
	
	public LegModeTravelTimeDistribution(String eventsFile) {
		this(eventsFile, null);
	}

	public static void main(String[] args) {
		String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";
		String [] runs = {"bau","ei","ci","eci","10ei"};

		for(String run:runs){
		
			String configFile = runDir+run+"/output_config.xml";
			int lastItr = LoadMyScenarios.getLastIteration(configFile);
			String eventsFile = runDir+run+"/ITERS/it."+lastItr+"/"+lastItr+".events.xml.gz";
			UserGroup ug = UserGroup.COMMUTER;
			
			LegModeTravelTimeDistribution lmttd = new LegModeTravelTimeDistribution(eventsFile/*,ug*/);
			lmttd.preProcessData();
			lmttd.postProcessData();
			lmttd.writeResults(runDir+"/analysis/legModeDistributions/"+run+"_it."+lastItr+"_");
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
		this.mode2PersonId2TravelTimes = this.lmth.getLegMode2PesonId2TripTimes();
		initializeTravelTimeClasses();
		getTravelModes();
		calculateMode2TravelTimeClases2LegCount();
	}

	private void calculateMode2TravelTimeClases2LegCount() {
		PersonFilter pf = new PersonFilter();
		this.mode2TravelTimeClasses2LegCount= new TreeMap<String, SortedMap<Integer,Integer>>();
		for(String mode:this.travelModes){
			SortedMap<Integer, Integer> travelTimeClasses2LegCount = new TreeMap<Integer, Integer>();
			for(int i=0;i<this.travelTimeClasses.size()-1;i++){
				int legCount =0;
				for(Id<Person> id:this.mode2PersonId2TravelTimes.get(mode).keySet()){
					if(this.userGroup!=null ){
						if(pf.isPersonIdFromUserGroup(id, this.userGroup)){
							for(double tt :this.mode2PersonId2TravelTimes.get(mode).get(id)){
								if(tt>this.travelTimeClasses.get(i)&&tt<this.travelTimeClasses.get(i+1)){
									legCount++;
								}
							}
						}
					} else {
						for(double tt :this.mode2PersonId2TravelTimes.get(mode).get(id)){
							if(tt>this.travelTimeClasses.get(i)&&tt<this.travelTimeClasses.get(i+1)){
								legCount++;
							}
						}
					}
				}
				travelTimeClasses2LegCount.put(this.travelTimeClasses.get(i+1), legCount);
			}
			this.mode2TravelTimeClasses2LegCount.put(mode, travelTimeClasses2LegCount);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		String outputFile = outputFolder+".LegModeTravelTimeDistribution.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("class \t");
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
		this.logger.info("Files have been written to " + outputFile);
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
		this.logger.info("The following travel time classes were defined: " + this.travelTimeClasses);
	}

	private double getHighestTravelTime(){
		double highestTravelTime = Double.NEGATIVE_INFINITY;
		for(String mode : this.mode2PersonId2TravelTimes.keySet()){
			for(Id<Person> id : this.mode2PersonId2TravelTimes.get(mode).keySet()){
				for (double d : this.mode2PersonId2TravelTimes.get(mode).get(id)){
					if(highestTravelTime<d){
						highestTravelTime = d;
					}
				}
			}
		}
		this.logger.info("Highest travel time is "+ highestTravelTime+" sec.");
		return highestTravelTime;
	}
	private void getTravelModes(){
		this.travelModes.addAll(this.mode2PersonId2TravelTimes.keySet());
	}
}
