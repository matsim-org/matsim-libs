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

package playground.mrieser.osm.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Senozon AG
 */
public class Main {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Osm2TransitLines osm2pt = new Osm2TransitLines(scenario.getTransitSchedule(), scenario.getNetwork());
		osm2pt.convert("/Volumes/Data/data/osm/switzerland/20130223/switzerland.osm");
		
		new NetworkWriter(scenario.getNetwork()).write("/Volumes/Data/data/osm/switzerland/20130223/ptNetwork.xml.gz");
//		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Volumes/Data/data/osm/switzerland/20130223/transitSchedule.xml.gz");
	}
}
