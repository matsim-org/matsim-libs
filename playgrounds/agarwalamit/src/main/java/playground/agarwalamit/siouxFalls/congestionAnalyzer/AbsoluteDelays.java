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
package playground.agarwalamit.siouxFalls.congestionAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;

/**
 * @author amit
 */
public class AbsoluteDelays {

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
	private static String [] runCase =  {"baseCaseCtd","ei","ci","eci"};//{"run201","run202","run203","run204"};
//	private final static String networkFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";

	public static void main(String[] args) {
		
		BufferedWriter writer =IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rAbsoluteDelays.txt");
		
		double [] delays = new double [runCase.length];
		
		for (int i=0; i<delays.length;i++){
			delays[i] = totalDelayInHoursFromEventsFile(runCase[i]);
		}
		
		try {
			writer.write("BAU \t EI \t CI \t ECI \n");
			for(double d:delays){
			writer.write(d+"\t");
			}
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
		
	}

	private static double totalDelayInHoursFromEventsFile(String runNumber) {
		EventsManager eventManager = EventsUtils.createEventsManager();
		String configFile = runNumber+"output_config.xml";
		int lastIteration = LoadMyScenarios.getLastIteration(configFile);
		String networkFile = runNumber+"output_network.xml.gz";
		Scenario sc =  LoadMyScenarios.loadScenarioFromNetwork(networkFile);
		MarginalCongestionHandlerImplV3 congestionHandlerImplV3= new MarginalCongestionHandlerImplV3(eventManager,(ScenarioImpl) sc);

		eventManager.addHandler(congestionHandlerImplV3);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String eventFileLocation = runNumber+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";
		String inputEventsFile = clusterPathDesktop+eventFileLocation;
		eventsReader.readFile(inputEventsFile);

		return congestionHandlerImplV3.getTotalDelay()/3600;
	}
}
