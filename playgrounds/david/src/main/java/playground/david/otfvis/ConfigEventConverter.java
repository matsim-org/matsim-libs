/* *********************************************************************** *
 * project: org.matsim.*
 * PadangEventConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;

//Usage ConfigEventConverter event-file config-file mvi-file
public class ConfigEventConverter {
	public static void main(String[] args) {
		if ( args.length != 3  ){
			System.out.println("Wrong argument count: Usage ConfigEventConverter event-file config-file mvi-file");
			System.exit(0);
		}
		
		Scenario scenario = new ScenarioLoaderImpl(args[1]).getScenario();

		String netFileName = scenario.getConfig().network().getInputFile();
		Double period = scenario.getConfig().simulation().getSnapshotPeriod();
		if(period == 0.0) period = 600.; // in the movie writing a period of zero does not make sense, use default value 
		new MatsimNetworkReader(scenario).readFile(netFileName);

		OTFEvent2MVI converter = new OTFEvent2MVI(new QueueNetwork(scenario.getNetwork()), args[0], args[2], period);
		converter.convert();

	}
}
