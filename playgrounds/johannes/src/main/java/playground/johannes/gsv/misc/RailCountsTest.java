/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.misc;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.johannes.gsv.analysis.RailCounts;
import playground.johannes.gsv.analysis.TransitLineAttributes;

/**
 * @author johannes
 *
 */
public class RailCountsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		
		TransitScheduleReader schedReader = new TransitScheduleReader(scenario);
		schedReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml");
		
		TransitLineAttributes lineAttribs = TransitLineAttributes.createFromFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitLineAttributes.xml");
		
		RailCounts.createFromFile("/home/johannes/gsv/matsim/studies/netz2030/data/railCounts.xml", lineAttribs, scenario.getNetwork(), scenario.getTransitSchedule());
	}

}
