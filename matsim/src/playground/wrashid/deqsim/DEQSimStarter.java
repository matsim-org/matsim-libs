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

import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.config.MatsimConfigReader;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.events.parallelEventsHandler.ParallelEvents;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;


public class DEQSimStarter {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: DEQSimStarter configfile.xml [config_v1.dtd]");
			return;
		}
		
		// read, prepare configuration
		Config config = Gbl.createConfig(null);
		if (args.length > 1 && args[1].toLowerCase().endsWith(".dtd")) {
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
		ScenarioData data = new ScenarioData(config);
		NetworkLayer network = data.getNetwork();
		Population population = data.getPopulation();
		// TODO: remove this after integration into core.
		Events events = new ParallelEvents(1);
		
		// run simulation
		JDEQSimulation client = new JDEQSimulation(network, population, events);
		client.run();

		// finish
		return;
	}

}
