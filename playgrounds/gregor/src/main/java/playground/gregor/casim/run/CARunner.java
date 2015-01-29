/* *********************************************************************** *
 * project: org.matsim.*
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.casim.simulation.CAMobsimFactory;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.vis.CASimVisRequestHandler;
import playground.gregor.vis.VisRequestHandler;
import playground.gregor.vis.VisServer;

public class CARunner implements IterationStartsListener {

	private Controler controller;
	private QSimDensityDrawer qSimDrawer;

	public static void main(String[] args) {
		if (args.length != 3) {
			printUsage();
			System.exit(-1);
		}
		String qsimConf = args[0];
		Config c = ConfigUtils.loadConfig(qsimConf);

		c.controler().setWriteEventsInterval(1);

		Scenario sc = ScenarioUtils.loadScenario(c);
		// sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sim2dsc);

		// c.qsim().setEndTime(120);
		// c.qsim().setEndTime(23*3600);
		// c.qsim().setEndTime(41*60);//+30*60);

		Controler controller = new Controler(sc);

		controller.setOverwriteFiles(true);
		LeastCostPathCalculatorFactory cost = createDefaultLeastCostPathCalculatorFactory(sc);
		CATripRouterFactory tripRouter = new CATripRouterFactory(sc, cost);

		controller.setTripRouterFactory(tripRouter);

		CAMobsimFactory factory = new CAMobsimFactory();
		if (args[1].equals("false")) {
			factory.setCANetworkFactory(new CASingleLaneNetworkFactory());
		}
		controller.addMobsimFactory("casim", factory);

		if (args[2].equals("true")) {
			AbstractCANetwork.EMIT_VIS_EVENTS = true;

			VisRequestHandler rHandle = new CASimVisRequestHandler();
			VisServer s = new VisServer(sc, rHandle);
			// AbstractCANetwork.STATIC_VIS_SERVER = s;
			AbstractCANetwork.STATIC_VIS_HANDLER = rHandle;
			// // VIS only
			// Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
			// Sim2DScenario sc2d =
			// Sim2DScenarioUtils.createSim2dScenario(conf2d);
			// sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
			//
			// EventBasedVisDebuggerEngine dbg = new
			// EventBasedVisDebuggerEngine(
			// sc);
			// InfoBox iBox = new InfoBox(dbg, sc);
			// dbg.addAdditionalDrawer(iBox);
			// dbg.addAdditionalDrawer(new Branding());
			// QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			// dbg.addAdditionalDrawer(qDbg);
			// controller.getEvents().addHandler(dbg);
			// controller.getEvents().addHandler(qDbg);
		}

		// DefaultTripRouterFactoryImpl fac = builder.build(sc);
		// DefaultTripRouterFactoryImpl fac = new
		// DefaultTripRouterFactoryImpl(sc, null, null);

		// controller.setTripRouterFactory(fac);
		controller.run();
	}

	private static LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(
			Scenario scenario) {
		Config config = scenario.getConfig();
		if (config.controler().getRoutingAlgorithmType()
				.equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
			return new DijkstraFactory();
		} else if (config
				.controler()
				.getRoutingAlgorithmType()
				.equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
			return new AStarLandmarksFactory(
					scenario.getNetwork(),
					new FreespeedTravelTimeAndDisutility(config.planCalcScore()),
					config.global().getNumberOfThreads());
		} else if (config.controler().getRoutingAlgorithmType()
				.equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
			return new FastDijkstraFactory();
		} else if (config
				.controler()
				.getRoutingAlgorithmType()
				.equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
			return new FastAStarLandmarksFactory(
					scenario.getNetwork(),
					new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
		} else {
			throw new IllegalStateException(
					"Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
		}
	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("CARunner");
		System.out.println("Controller for ca (pedestrian) simulations.");
		System.out.println();
		System.out.println("usage : CARunner config multilane_mode visualize");
		System.out.println();
		System.out.println("config:   A MATSim config file.");
		System.out.println("multilane_mode:   one of {true,false}.");
		System.out.println("visualize:   one of {true,false}.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2014, matsim.org");
		System.out.println();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if ((event.getIteration()) % 1 == 0 || event.getIteration() > 50) {
			// this.factory.debug(this.visDebugger);
			this.controller.getEvents().addHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(true);
        } else {
			// this.factory.debug(null);
			this.controller.getEvents().removeHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(false);
        }
		// this.visDebugger.setIteration(event.getIteration());
	}
}
