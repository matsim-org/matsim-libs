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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.munich.utils.UserGroupUtilsExtended;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class TravelTimePerUserGroup  {

	public TravelTimePerUserGroup() {
		this.sc = LoadMyScenarios.loadScenarioFromPlansNetworkAndConfig(this.populationFile, this.networkFile, this.configFile);
		
		this.usrGrpExtended = new UserGroupUtilsExtended();
		this.userGrpToBoxPlotData = new TreeMap<UserGroup, List<Double>>();
		
		int lastIteration = LoadMyScenarios.getLastIteration(this.configFile);
		String eventsFile = this.outputDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		this.travelTimeAnalyzer = new ModalTravelTimeAnalyzer(eventsFile);
	}

	private final ModalTravelTimeAnalyzer travelTimeAnalyzer;
	private final Scenario sc; 
	private static final Logger LOGGER = Logger.getLogger(TravelTimePerUserGroup.class);
	private Map<String, Map<Id<Person>, List<Double>>> mode2PersonId2TravelTimes;
	
	private final String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/25ExI/";
	private final String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private final String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private final String configFile = outputDir+"/output_config.xml";//"/config.xml";//

	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MeanTime = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MedianTime = new TreeMap<UserGroup, SortedMap<String,Double>>();
	
	private final UserGroupUtilsExtended usrGrpExtended;
	private final SortedMap<UserGroup, List<Double>> userGrpToBoxPlotData;

	public static void main(String[] args) {

		TravelTimePerUserGroup ttUG = new TravelTimePerUserGroup();
		ttUG.run();
	}

	private void run(){
		this.travelTimeAnalyzer.run();
		
		String mainMode = TransportMode.car;
		this.mode2PersonId2TravelTimes = this.travelTimeAnalyzer.getMode2PesonId2TripTimes();
		getUserGroupTravelMeanAndMeadian();
		createBoxPlotData(this.travelTimeAnalyzer.getMode2PersonId2TotalTravelTime().get(mainMode));
		
		writeResults(this.outputDir+"/analysis/");
	}

	public void writeResults(String outputFolder) {
		LOGGER.info("Writing user group to travel mode to mean and mediam time.");
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelMode2MeanAndMedianTravelTime.txt");
		try {
			writer.write("UserGroup \t travelMode \t MeanTravelTime \t MedianTravelTime \n");
			for(UserGroup ug:this.usrGrp2Mode2MeanTime.keySet()){
				for(String travelMode:this.usrGrp2Mode2MeanTime.get(ug).keySet()){
					writer.write(ug+"\t"+travelMode+"\t"+this.usrGrp2Mode2MeanTime.get(ug).get(travelMode)+"\t"+this.usrGrp2Mode2MedianTime.get(ug).get(travelMode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}

		LOGGER.info("Writing data for box plots for each user group.");
		try {
			String outputFile = outputFolder+"/boxPlot/";
			new File(outputFile).mkdirs();
			for(UserGroup ug :this.userGrpToBoxPlotData.keySet()){
				writer = IOUtils.getBufferedWriter(outputFile+"/travelTime_"+ug+".txt");
				writer.write(ug+"\n");
				for(double d :this.userGrpToBoxPlotData.get(ug)){
					writer.write(d+"\n");
				}
				writer.close();
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
		LOGGER.info("Writing data is finished.");
	}

	private void createBoxPlotData (Map<Id<Person>, Double> map){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug:UserGroup.values()){
			Population relevantPop = pf.getPopulation(sc.getPopulation(), ug);
			userGrpToBoxPlotData.put(ug, this.usrGrpExtended.getTotalStatListForBoxPlot(map, relevantPop));
		}
	}
	
	private void getUserGroupTravelMeanAndMeadian(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug:UserGroup.values()){
			Population pop = pf.getPopulation(sc.getPopulation(), ug);
			this.usrGrp2Mode2MeanTime.put(ug, this.usrGrpExtended.calculateTravelMode2MeanFromLists(this.mode2PersonId2TravelTimes, pop));
			this.usrGrp2Mode2MedianTime.put(ug, this.usrGrpExtended.calculateTravelMode2MedianFromLists(this.mode2PersonId2TravelTimes, pop));
		}
	}
}
