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
package emissions.roadtypemapping;

import java.util.Map.Entry;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *	Maps road type for emissions calculation. Road categories are based solely on freespeed.
 */
public class RoadTypeMapper {
	private final static String dir = "D:\\runs-svn\\vw_rufbus\\";

	public static void main(String[] args) {


		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(dir+"newnet.xml.gz");
		Network network = scenario.getNetwork();
		
		
		for (Link l : network.getLinks().values()){
			double freespeed = l.getFreespeed();
			if (freespeed<9)
				NetworkUtils.setType( l, (String) "75");
			else if (freespeed<14)
				NetworkUtils.setType( l, (String) "43");
			else if (freespeed<17)
				NetworkUtils.setType( l, (String) "45");
			else if (freespeed<23)
				NetworkUtils.setType( l, (String) "14");
			else
				NetworkUtils.setType( l, (String) "11");
		}
	new NetworkWriter(network).write(dir+"newnet.xml.gz");
	}

	

}
