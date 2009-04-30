/* *********************************************************************** *
 * project: org.matsim.*
 * DEQSimStarter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.wrashid.deqsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;

public class PDESStarter2 {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: DEQSimStarter configfile.xml [config_v1.dtd]");
			return;
		}
		
		// read, prepare configuratiaon
		Config config = Gbl.createConfig(args);
		
		// prepare data
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		NetworkLayer network = (NetworkLayer) data.getNetwork();
		Population population = data.getPopulation();
		Events events = new Events();
		
		// run simulation
		JavaPDEQSim2 client = new JavaPDEQSim2(network, population, events);
		client.run();

		// finish
		return;
	}

}
