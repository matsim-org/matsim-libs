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
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.legModeHandler.LegModeRouteDistanceDistributionHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */
public class RouteDistancePerUserGroup {

	public RouteDistancePerUserGroup() {
		super();
		sc = LoadMyScenarios.loadScenarioFromNetworkAndPlans(populationFile, networkFile);
		this.usrGrpExtended = new UserGroupUtilsExtended();
	}

	private Logger logger = Logger.getLogger(RouteDistancePerUserGroup.class);
	private  String outputDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/eci/";/*"./output/run2/";*/
	private  String populationFile =outputDir+ "/output_plans.xml.gz";//"/network.xml";
	private  String networkFile =outputDir+ "/output_network.xml.gz";//"/network.xml";
	private Scenario sc;
	private UserGroupUtilsExtended usrGrpExtended;
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MeanDistance = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<UserGroup, SortedMap<String, Double>> usrGrp2Mode2MedianDistance = new TreeMap<UserGroup, SortedMap<String,Double>>();
	private SortedMap<String, Map<Id, List<Double>>> mode2PersonId2RouteDist;
	private SortedMap<String, Map<Id, Double>> mode2PersonId2TotalRouteDist;

	public static void main(String[] args) {
		RouteDistancePerUserGroup routeDistUG = new RouteDistancePerUserGroup();
		routeDistUG.run();
	}

	private void run(){
		LegModeRouteDistanceDistributionHandler	lmdfed = new LegModeRouteDistanceDistributionHandler();
		lmdfed.init(sc);
		lmdfed.preProcessData();
		lmdfed.postProcessData();
		lmdfed.writeResults(outputDir+"/analysis/");
		mode2PersonId2RouteDist = lmdfed.getMode2PersonId2RouteDistances();
		mode2PersonId2TotalRouteDist = lmdfed.getMode2PersonId2TotalRouteDistance();
		getUserGroupDistanceMeanAndMeadian();
		writeResults(outputDir+"/analysis/");
	}

	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/usrGrp2TravelMode2MeanAndMedianRouteDistance.txt");
		try {
			writer.write("UserGroup \t travelMode \t MeanRouteDistance \t MedianRouteDistance \n");
			for(UserGroup ug:usrGrp2Mode2MeanDistance.keySet()){
				for(String travelMode:usrGrp2Mode2MeanDistance.get(ug).keySet()){
					writer.write(ug+"\t"+travelMode+"\t"+usrGrp2Mode2MeanDistance.get(ug).get(travelMode)+"\t"+usrGrp2Mode2MedianDistance.get(ug).get(travelMode)+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to a file.");
		}
		logger.info("Data writing is finished.");
	}

	private void getUserGroupDistanceMeanAndMeadian(){
		PersonFilter pf = new PersonFilter();
		for(UserGroup ug:UserGroup.values()){
			Population pop = pf.getPopulation(sc.getPopulation(), ug);
			usrGrp2Mode2MeanDistance.put(ug, this.usrGrpExtended.calculateTravelMode2MeanFromLists(mode2PersonId2RouteDist, pop));
			usrGrp2Mode2MedianDistance.put(ug, this.usrGrpExtended.calculateTravelMode2MedianFromLists(mode2PersonId2RouteDist, pop));
//			usrGrp2Mode2MeanDistance.put(ug, this.usrGrpExtended.calculateTravelMode2Mean(mode2PersonId2TotalRouteDist, pop));
//			usrGrp2Mode2MedianDistance.put(ug, this.usrGrpExtended.calculateTravelMode2Median(mode2PersonId2TotalRouteDist, pop));
		}
	}
}
