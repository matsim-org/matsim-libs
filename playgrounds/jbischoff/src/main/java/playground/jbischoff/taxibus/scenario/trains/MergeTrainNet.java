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

package playground.jbischoff.taxibus.scenario.trains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.andreas.mzilske.bvg09.MergeNetworks;

/**
 * @author  jbischoff
 *
 */
public class MergeTrainNet {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/versions/network_trainonly.xml");
	new MatsimNetworkReader(scenario2.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/braunschweig/bs-network.xml");
	MergeNetworks.merge(scenario.getNetwork(), "", scenario2.getNetwork());
	
	new NetworkWriter(scenario.getNetwork()).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/versions/networkpt-feb.xml");
}
}
