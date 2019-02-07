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

package ft.wobscenario.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  thiel
 *
 */
public class CleanNetwork {
public static void main(String[] args) {
	Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(s.getNetwork()).readFile("C:\\Users\\VW7N5TD\\Desktop\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Network\\Hannover_streetnetwork.xml");
	new NetworkCleaner().run(s.getNetwork());
	new NetworkWriter(s.getNetwork()).write("C:\\Users\\VW7N5TD\\Desktop\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Hannover_streetnetworkc.xml");
	
}
}