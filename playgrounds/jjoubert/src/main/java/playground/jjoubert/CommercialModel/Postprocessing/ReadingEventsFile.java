/* *********************************************************************** *
 * project: org.matsim.*
 * ReadingEventsFile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.CommercialModel.Postprocessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jjoubert.CommercialModel.Listeners.MyCommercialActivityDensityWriter;

public class ReadingEventsFile {
	
	public static void main(String [] args){

		String root = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Commercial/";
		String networkFilename = root + "Input/networkSA.xml";
		String outputCommercialActivityDensityFilename = root + "Output/Run06/it.100-7699/100.eventsTruckMinor.txt";
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(new File( outputCommercialActivityDensityFilename )));
			try{
				Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				Network nl = scenario.getNetwork();
				MatsimNetworkReader nr = new MatsimNetworkReader(scenario.getNetwork());
				nr.readFile(networkFilename);
				
				String input = root + "/Output/Run06/it.100-7699/100.events.txt.gz";
				EventsManager events = EventsUtils.createEventsManager();
				MyCommercialActivityDensityWriter handler = new MyCommercialActivityDensityWriter(bw, nl);	
				events.addHandler(handler);

				MatsimEventsReader reader = new MatsimEventsReader(events);
				reader.readFile(input);
				System.out.println("Events file read!");					
			} finally{
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
