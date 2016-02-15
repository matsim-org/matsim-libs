/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
* @author amit
*/

public class StatsWriter {
	
	public static void main(String[] args) {
		StatsWriter.run("../../../../repos/runs-svn/patnaIndia/run108/calibration/c2/");
	}

	public static void run (final String filesDir) {
		
//		String filesDir = "../../../../repos/runs-svn/patnaIndia/run108/calibration/congestionFree/";
		String eventsFile = filesDir+"/output_events.xml.gz";
		String plansFile = filesDir+"/output_plans.xml.gz";
		String networkFile = filesDir+"/output_network.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndNetwork(plansFile, networkFile);
		
		EventsManager events = EventsUtils.createEventsManager();
		
		Vehicle2DriverEventHandler vehicle2Driver = new Vehicle2DriverEventHandler();
		events.addHandler(vehicle2Driver);
		
		final KNAnalysisEventsHandler.Builder builder = new KNAnalysisEventsHandler.Builder(scenario) ;
		final KNAnalysisEventsHandler calcLegTimes = builder.build();
		
		events.addHandler( calcLegTimes );
		
		new MatsimEventsReader(events).readFile(eventsFile) ;
		calcLegTimes.writeStats(filesDir+"/analysis/_stats_");
	}
}