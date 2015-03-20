/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.foreign;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author johannes
 *
 */
public class NetworkAttributeChanger {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
		reader.parse("/home/johannes/sge/prj/matsim/run/632/output/network.xml");
		
		for(Link link : scenario.getNetwork().getLinks().values()) {
			if(link.getId().toString().contains(".l")) {
				link.setFreespeed(30/3.6);
//				link.setLength(9999999);
			}
		}
		
		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.write("/home/johannes/sge/prj/matsim/run/632/output/network2.xml");
	}

}
