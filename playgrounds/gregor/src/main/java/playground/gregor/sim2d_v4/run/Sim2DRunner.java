/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;
import playground.gregor.sim2d_v4.simulation.HybridQ2DMobsimFactory;

public class Sim2DRunner {

	
	public static void main(String [] args) {
		if (args.length != 2) {
			printUsage();
			System.exit(-1);
		}
		String sim2DConf = args[0];
		String qsimConf = args[1];
		Sim2DConfig sim2dc = Sim2DConfigUtils.loadConfig(sim2DConf);
		Sim2DScenario sim2dsc = Sim2DScenarioUtils.loadSim2DScenario(sim2dc);
		Config c = ConfigUtils.loadConfig(qsimConf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		sc.addScenarioElement(sim2dsc);
		sim2dsc.connect(sc);
		
		
		Controler controller = new Controler(sc);
		
		
		HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory();
		controller.addMobsimFactory("hybridQ2D", factory);
		
		controller.run();
	}
	
	protected static void printUsage() {
		System.out.println();
		System.out.println("Controller2D");
		System.out.println("Controller for hybrid sim2d qsim (pedestrian) simulations.");
		System.out.println();
		System.out.println("usage : Controller2D sim2d-config-file qsim-config-file");
		System.out.println();
		System.out.println("sim2d-config-file:  A sim2d config file.");
		System.out.println("qsim-config-file:   A MATSim config file.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2012, matsim.org");
		System.out.println();
	}
}
