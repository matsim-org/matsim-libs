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

package playground.jbischoff.taxibus.scenario.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class ReduceNetworkSpeeds {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String basedir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/";
		new MatsimNetworkReader(scenario).readFile(basedir+"network.xml");
		for (Link link : scenario.getNetwork().getLinks().values()){
			double speed = link.getFreespeed();
			if (speed<10) link.setFreespeed(0.75*speed);
			else if (speed<20) link.setFreespeed(0.7*speed);
			else if (speed<30) 
			{
				if (link.getNumberOfLanes()<2) link.setFreespeed(0.7*speed);
				else link.setFreespeed(0.75*speed);
			}
			else link.setFreespeed(0.7*speed);
			
		}
		new NetworkWriter(scenario.getNetwork()).write(basedir+"networks.xml");
		
		
		
	}

}
