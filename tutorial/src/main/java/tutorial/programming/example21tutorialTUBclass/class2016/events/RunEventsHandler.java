/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.example21tutorialTUBclass.class2016.events;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class RunEventsHandler {

	public static void main(String[] args) {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input/network.xml");
		
		CarTravelDistanceEvaluator carTravelDistanceEvaluator = new CarTravelDistanceEvaluator(scenario.getNetwork());
		eventsManager.addHandler(carTravelDistanceEvaluator);
		new MatsimEventsReader(eventsManager).readFile("output/davis-basecase1/davis01.output_events.xml.gz");
		
		writeDistancesToFile(carTravelDistanceEvaluator.getDistanceDistribution(), "output/davis-basecase1/cardistances.txt");
		
	}
	static void writeDistancesToFile(int[] distanceDistribution, String fileName){
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write("Distance\tRides");
			for (int i = 0;i<distanceDistribution.length;i++){
			bw.newLine();
			bw.write(i+"\t"+distanceDistribution[i]);	
			}
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

}
