/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.emissions;

import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *	Maps road type for emissions calculation. Road categories are based solely on freespeed.
 */
public class RoadTypeMapper {
	private final static String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/2014_10_basic_scenario_v4/emissions/";

	public static void main(String[] args) {


		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(dir+"berlin_brb.xml");
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		NetworkImpl net2 = (NetworkImpl) NetworkUtils.createNetwork();
		
		 for (Node n : network.getNodes().values()){
	            Node newNode = n;
	            newNode.getInLinks().clear();
	            newNode.getOutLinks().clear();
	            net2.addNode(newNode);
	        }
		
		for (Entry<Id<Link>, Link> e :  network.getLinks().entrySet()){
			LinkImpl l = (LinkImpl) e.getValue();
			double freespeed = l.getFreespeed();
			if (freespeed<9) l.setType("75");
			else if (freespeed<14) l.setType("43");
			else if (freespeed<17) l.setType("45");
			else if (freespeed<23) l.setType("14");
			else  l.setType("11");
			net2.addLink(l);
		}
	new NetworkWriter(net2).write(dir+"berlin_brb_t.xml");
	}

}
