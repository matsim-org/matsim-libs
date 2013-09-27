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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
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
	
	new MatsimNetworkReader(sc).readFile("C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\network.xml");
	TaxiCustomerWaitTimeAnalyser tcwa = new TaxiCustomerWaitTimeAnalyser(sc);
	
	manager.addHandler(tcwa);
	new MatsimEventsReader(manager).readFile("C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\Modified Dispatch\\modifiedDispatch\\events.xml.gz");
	tcwa.writeCustomerWaitStats("C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\Modified Dispatch\\modifiedDispatch\\pp_WaitTimes");
}
}
