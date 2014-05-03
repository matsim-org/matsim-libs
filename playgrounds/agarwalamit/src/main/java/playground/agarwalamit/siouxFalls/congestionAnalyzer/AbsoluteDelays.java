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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;

/**
 * @author amit
 */
public class AbsoluteDelays {

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/";
	private static String [] runNumber =  {"run101","run102","run103","run104"};
	public static void main(String[] args) {
		
		BufferedWriter writer =IOUtils.getBufferedWriter(clusterPathDesktop+"/outputMC/analysis/r/rAbsoluteDelays.txt");
		
		double [] delays = new double [runNumber.length];
		
		for (int i=0; i<delays.length;i++){
			delays[i] = totalDelayInHoursFromEventsFile(runNumber[i]);
		}
		
		try {
			writer.write("BAU \t EI \t CI \t ECI \n");
			writer.write(delays[0]+"\t"+delays[1]+"\t"+delays[2]+"\t"+delays[3]+"\n");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into File. Reason : "+e);
		}
		
	}

	public static double totalDelayInHoursFromEventsFile(String runNumber) {
		EventsManager eventManager = EventsUtils.createEventsManager();
		ScenarioImpl sc = loadScenario(runNumber);
		MarginalCongestionHandlerImplV3 congestionHandlerImplV3= new MarginalCongestionHandlerImplV3(eventManager, sc);

		eventManager.addHandler(congestionHandlerImplV3);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String inputEventsFile = clusterPathDesktop+"/outputMC/"+runNumber+"/ITERS/it.100/100.events.xml.gz";
		eventsReader.readFile(inputEventsFile);

		return congestionHandlerImplV3.getTotalDelay()/3600;
	}
	
	private static ScenarioImpl loadScenario(String runNumber) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(clusterPathDesktop+"/input/SiouxFalls_networkWithRoadType.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return (ScenarioImpl) scenario;
	}
}
