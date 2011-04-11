/* *********************************************************************** *
 * project: org.matsim.*
 * MkLinkStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.analysis;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

public class MkLinkStats {
	
	public static void main(String [] args) {
		String outDir = "../../../arbeit/svn/runs-svn/run316/stage2/output";
		String it = "201";
		String net = outDir + "/output_network.xml.gz";
		String eventsFile = outDir + "/ITERS/it." + it + "/" + it + ".events.txt.gz";
		String stats =  outDir + "/ITERS/it." + it + "/" + it + ".linkstats.txt.gz";
		
		ScenarioImpl scenario = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(outDir + "/output_config.xml.gz").getScenario();
		
		NetworkImpl netzzz = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(net);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		VolumesAnalyzer h = new VolumesAnalyzer(60,5*3600,netzzz);
		CalcLinkStats ls = new CalcLinkStats(netzzz);
		events.addHandler(h);
		
		TravelTimeCalculator tt = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(netzzz, scenario.getConfig().travelTimeCalculator());
		events.addHandler(tt);
		new EventsReaderTXTv1(events).readFile(eventsFile);
		ls.addData(h, tt);
		ls.writeFile(stats);
		
		
	}

}
