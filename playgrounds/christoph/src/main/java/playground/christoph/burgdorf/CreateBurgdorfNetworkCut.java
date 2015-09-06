/* *********************************************************************** *
 * project: org.matsim.*
 * CreateBurgdorfNetworkCut.java
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

package playground.christoph.burgdorf;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkScenarioCut;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateBurgdorfNetworkCut {

	public static void main(String[] args) {		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("../../matsim/mysimulations/burgdorf/input/network_burgdorf.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		new NetworkScenarioCut(new Coord(613789.0, 211908.0), 30000.0).run(scenario.getNetwork());
		
		new NetworkWriter(scenario.getNetwork()).write("../../matsim/mysimulations/burgdorf/input/network_burgdorf_cut.xml.gz");
	}
	
}
