/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.evaluation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;


/**
 *@author jbischoff
 *
 */

public class WaitTimesPostProcessor {
public static void main(String[] args) {
	EventsManager manager = EventsUtils.createEventsManager();
	Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new MatsimNetworkReader(sc).readFile("C:\\local_jb\\data\\scenarios\\2014_02_basic_scenario_v1\\berlin_brb.xml.gz");
TaxiCustomerDebugger tdb 	= new TaxiCustomerDebugger();
	manager.addHandler(tdb);
	new MatsimEventsReader(manager).readFile("C:\\local_jb\\data\\scenarios\\2014_02_basic_scenario_v1\\1slow_nolog\\events.xml.gz");
	
	System.out.println("Arrivals " +tdb.arrivals);
	System.out.println("Departures " +tdb.departures);
	System.out.println("Vehicle Enter " +tdb.vehicleEnts);

}
}
