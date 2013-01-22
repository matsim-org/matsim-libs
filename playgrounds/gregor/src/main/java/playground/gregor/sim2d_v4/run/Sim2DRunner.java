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
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v3.trafficmonitoring.MSATravelTimeCalculatorFactory;
import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;
import playground.gregor.sim2d_v4.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;

public class Sim2DRunner implements IterationStartsListener{

	private final VisDebugger visDebugger = new VisDebugger();
	private Controler controller;

	public static void main(String [] args) {
		if (args.length != 3) {
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

		controller.setOverwriteFiles(true);

		HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory();
		controller.addMobsimFactory("hybridQ2D", factory);

		if (args[2].equals("true")) {
			Sim2DRunner runner = new Sim2DRunner();
			PhysicalSim2DEnvironment.visDebugger = runner.visDebugger;
			controller.addControlerListener(runner);
			runner.controller = controller;
		}

//		controller.setCreateGraphs(false);
		controller.setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		controller.run();
	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("Controller2D");
		System.out.println("Controller for hybrid sim2d qsim (pedestrian) simulations.");
		System.out.println();
		System.out.println("usage : Controller2D sim2d-config-file qsim-config-file visualize");
		System.out.println();
		System.out.println("sim2d-config-file:  A sim2d config file.");
		System.out.println("qsim-config-file:   A MATSim config file.");
		System.out.println("visualize:   one of {true,false}.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2012, matsim.org");
		System.out.println();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if ((event.getIteration()) % 1 == 0 || event.getIteration() > 400) {
			PhysicalSim2DEnvironment.DEBUG = true;
			this.controller.setCreateGraphs(true);
		} else {
			PhysicalSim2DEnvironment.DEBUG = false;
			this.controller.setCreateGraphs(false);
		}
		this.visDebugger.setIteration(event.getIteration());
	}
}
