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

package playground.gregor.casim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.casim.simulation.HybridQCAMobsimFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.Branding;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CARunner implements IterationStartsListener{

	private Controler controller;
	private QSimDensityDrawer qSimDrawer;

	public static void main(String [] args) {
		if (args.length != 2) {
			printUsage();
			System.exit(-1);
		}
		String qsimConf = args[0];
		Config c = ConfigUtils.loadConfig(qsimConf);
		c.scenario().setUseVehicles(true);
		
		c.controler().setWriteEventsInterval(1);
		Scenario sc = ScenarioUtils.loadScenario(c);
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sim2dsc);
		
//		c.qsim().setEndTime(120);
//		c.qsim().setEndTime(23*3600);
//		c.qsim().setEndTime(41*60);//+30*60);


		
		
		//offsets needed to convert to doubles later in program
		double minX = Double.POSITIVE_INFINITY;
		double minY = minX;
		for (Node n : sc.getNetwork().getNodes().values()) {
			if (n.getCoord().getX() < minX) {
				minX = n.getCoord().getX(); 
			}
			if (n.getCoord().getY() < minY) {
				minY = n.getCoord().getY(); 
			}
		}
//		sim2dc.setOffsets(minX, minY);
		
		Controler controller = new Controler(sc);

		controller.setOverwriteFiles(true);
		

		HybridQCAMobsimFactory factory = new HybridQCAMobsimFactory();
		controller.addMobsimFactory("casim", factory);

		
		if (args[1].equals("true")) {
			//VIS only
			Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
			Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
			sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
			
			EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
			InfoBox iBox = new InfoBox(dbg,sc);
			dbg.addAdditionalDrawer(iBox);
			dbg.addAdditionalDrawer(new Branding());
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			dbg.addAdditionalDrawer(qDbg);
			controller.getEvents().addHandler(dbg);
			controller.getEvents().addHandler(qDbg);
		}
		
//		DefaultTripRouterFactoryImpl fac = builder.build(sc);
//		DefaultTripRouterFactoryImpl fac = new DefaultTripRouterFactoryImpl(sc, null, null);
	
//		controller.setTripRouterFactory(fac);
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
		if ((event.getIteration()) % 1 == 0 || event.getIteration() > 50) {
//			this.factory.debug(this.visDebugger);
			this.controller.getEvents().addHandler(this.qSimDrawer);
			this.controller.setCreateGraphs(true);
		} else {
//			this.factory.debug(null);
			this.controller.getEvents().removeHandler(this.qSimDrawer);
			this.controller.setCreateGraphs(false);
		}
//		this.visDebugger.setIteration(event.getIteration());
	}
}
