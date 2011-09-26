/* *********************************************************************** *
 * project: org.matsim.*
 * EventsAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class BusCorridorEventFileReader {

	public static void main(String[] args) {
		
		String configFile = "../../shared-svn/studies/ihab/busCorridor/input/config_busline.xml";
		String eventFile = "../../shared-svn/studies/ihab/busCorridor/output/busline_10buses/ITERS/it.0/0.events.xml.gz";

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = new BusCorridorEventsManagerImpl();
		
//		BusCorridorLinkLeaveEventHandler handler1 = new BusCorridorLinkLeaveEventHandler(scenario);
		BusCorridorActivityEndEventHandler handler2 = new BusCorridorActivityEndEventHandler(scenario);
		BusCorridorPersonEntersVehicleEventHandler handler3 = new BusCorridorPersonEntersVehicleEventHandler(scenario);
		BusCorridorPersonLeavesVehicleEventHandler handler4 = new BusCorridorPersonLeavesVehicleEventHandler(scenario);

//		events.addHandler(handler1);	
		events.addHandler(handler2);
		events.addHandler(handler3);
		events.addHandler(handler4);

		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		
		System.out.println("Events file "+eventFile+" read!");
	}

}
