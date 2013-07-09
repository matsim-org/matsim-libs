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

package playground.dgrether.signalsystems.cottbus.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *@author jbischoff
 *
 */

public class CottbusPTLivePlayer {

	
	public static void main(String[] args) {
//		String CottbusBaseDirectory = "\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\svn-checkouts\\cottbus\\cottbus_feb_fix\\";
		String CottbusBaseDirectory = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/";
		
		String configfile = CottbusBaseDirectory + "Cottbus-pt/config.xml";
		String network = CottbusBaseDirectory + "Cottbus-pt/network_pt.xml";
		String schedule = CottbusBaseDirectory + "Cottbus-pt/schedule.xml";
		String vehicles = CottbusBaseDirectory + "Cottbus-pt/transitVehicles.xml";
		Config config = ConfigUtils.loadConfig(configfile);

		config.network().setInputFile(network);
		config.transit().setVehiclesFile(vehicles);
		config.transit().setTransitScheduleFile(schedule);

		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		OTFVis.playScenario(scenario);

		
	}

}
