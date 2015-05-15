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
package org.matsim.contrib.analysis.travelsummary;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunEventsToTravelSummaryExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// I don't mind the "properties" approach ... but then to also need a config file seems overkill to me. kai, mar'15
		
		String eventsFileName = args[0] ;
		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( args[1] );
		
		Scenario scenario = ScenarioUtils.loadScenario(config); // maybe overkill, but one often needs additional elements later. kai, may'15
		
		EventsToTravelSummaryTables handler = new EventsToTravelSummaryTables( scenario.getNetwork(), config ) ;
		
		EventsManager events = new EventsManagerImpl() ;
		
		events.addHandler( handler ) ;
		
		new MatsimEventsReader(events).readFile( eventsFileName );
		
		try {
			handler.writeSimulationResultsToCSV( config.controler().getOutputDirectory(), "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
