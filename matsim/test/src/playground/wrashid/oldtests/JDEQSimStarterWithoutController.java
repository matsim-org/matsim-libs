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

package playground.wrashid.oldtests;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.Events;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.network.NetworkLayer;


public class JDEQSimStarterWithoutController {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: DEQSimStarter configfile.xml [config_v1.dtd]");
			return;
		}
		
		// read, prepare configuration
		Config config = Gbl.createConfig(null);
		if ((args.length > 1) && args[1].toLowerCase().endsWith(".dtd")) {
			try {
				new MatsimConfigReader(config).readFile(args[0], args[1]);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else {
			new MatsimConfigReader(config).readFile(args[0]);
		}
		
		// prepare data
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		NetworkLayer network = (NetworkLayer) data.getNetwork();
		Population population = data.getPopulation();
		// TODO: remove this after integration into core.
		Events events = new ParallelEvents(1);
		
		events.initProcessing();
		
		// run simulation
		JDEQSimulation client = new JDEQSimulation(network, population, events);
		client.run();

		events.finishProcessing();
		
		// finish
		return;
	}

}
