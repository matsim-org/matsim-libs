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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeTravelTimeHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */
public class TravelTimePerUserGroup extends AbstractAnalyisModule {

	public TravelTimePerUserGroup() {
		super(TravelTimePerUserGroup.class.getSimpleName());
		this.travelTimeHandler = new LegModeTravelTimeHandler();
		sc = LoadMyScenarios.loadScenario(populationFile, networkFile, configFile);
		lastIteration = sc.getConfig().controler().getLastIteration();
		this.eventsFile = 	outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		this.usrGrpExtended = new UserGroupUtilsExtended();
	}

	private LegModeTravelTimeHandler travelTimeHandler;
	private Scenario sc; 
	private int lastIteration;
	private Logger logger = Logger.getLogger(TravelTimePerUserGroup.class);
	private Map<String, Map<Id, List<Double>>> mode2PersonId2TravelTimes;
	private String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/eci/";/*"./output/run2/";*/
	private String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private String configFile = outputDir+"/output_config.xml";//"/config.xml";//
	private String eventsFile;
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MeanTime = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MedianTime = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private UserGroupUtilsExtended usrGrpExtended;

	public static void main(String[] args) {

		TravelTimePerUserGroup ttUG = new TravelTimePerUserGroup();
		ttUG.run();
	}

	private void run(){
		preProcessData();
		postProcessData();
		writeResults(outputDir+"/analysis/");
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(this.travelTimeHandler);
		reader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		mode2PersonId2TravelTimes = travelTimeHandler.getLegMode2PesonId2TripTimes();
		getUserGroupTravelMeanAndMeadian();
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelMode2MeanAndMedianTravelTime.txt");
		try {
			writer.write("UserGroup \t travelMode \t MeanTravelTime \t MedianTravelTime \n");
			for(UserGroup ug:usrGrp2Mode2MeanTime.keySet()){
				for(String travelMode:usrGrp2Mode2MeanTime.get(ug).keySet()){
					writer.write(ug+"\t"+travelMode+"\t"+usrGrp2Mode2MeanTime.get(ug).get(travelMode)+"\t"+usrGrp2Mode2MedianTime.get(ug).get(travelMode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
		logger.info("Data writing is finished.");
	}

	
	private void getUserGroupTravelMeanAndMeadian(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug:UserGroup.values()){
			Population pop = pf.getPopulation(sc.getPopulation(), ug);
			usrGrp2Mode2MeanTime.put(ug, this.usrGrpExtended.calculateTravelMode2MeanFromLists(mode2PersonId2TravelTimes, pop));
			usrGrp2Mode2MedianTime.put(ug, this.usrGrpExtended.calculateTravelMode2MedianFromLists(mode2PersonId2TravelTimes, pop));
		}
	}
}
