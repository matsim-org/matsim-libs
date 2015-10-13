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

package playground.gregor.ctsim.run;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.casim.simulation.CAMobsimFactory;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.ctsim.simulation.CTTripRouterFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;

public class CTRunner implements IterationStartsListener {

	private Controler controller;
	private QSimDensityDrawer qSimDrawer;

	public static void main(String[] args) {
		if (args.length != 3) {
			printUsage();
			System.exit(-1);
		}
		String qsimConf = args[0];
		Config c = ConfigUtils.loadConfig(qsimConf);

		c.controler().setOutputDirectory("/Users/laemmel/devel/cttest/output/");

		c.controler().setWriteEventsInterval(1);
		c.controler().setMobsim("ctsim");
		Scenario sc = ScenarioUtils.loadScenario(c);


		final Controler controller = new Controler(sc);


		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		LeastCostPathCalculatorFactory cost = createDefaultLeastCostPathCalculatorFactory(sc);
		CTTripRouterFactory tripRouter = new CTTripRouterFactory(sc, cost);

		controller.setTripRouterFactory(tripRouter);


		final CAMobsimFactory factory = new CAMobsimFactory();
		if (args[1].equals("false")) {
			factory.setCANetworkFactory(new CASingleLaneNetworkFactory());
		}
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals("casim")) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return factory.createMobsim(controller.getScenario(), controller.getEvents());
						}
					});
				}
			}
		});

		controller.addControlerListener(new IterationStartsListener() {

			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				AbstractCANetwork.EMIT_VIS_EVENTS = (event.getIteration()) % 100 == 0 && (event.getIteration()) > 0;

			}
		});

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
		}
		else {
			if (config
					.controler()
					.getRoutingAlgorithmType()
					.equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
				return new AStarLandmarksFactory(
						scenario.getNetwork(),
						new FreespeedTravelTimeAndDisutility(config.planCalcScore()),
						config.global().getNumberOfThreads());
			}
			else {
				if (config.controler().getRoutingAlgorithmType()
						.equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
					return new FastDijkstraFactory();
				}
				else {
					if (config
							.controler()
							.getRoutingAlgorithmType()
							.equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
						return new FastAStarLandmarksFactory(
								scenario.getNetwork(),
								new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
					}
					else {
						throw new IllegalStateException(
								"Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
					}
				}
			}
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
		if ((event.getIteration()) % 5 == 0 || event.getIteration() > 0) {
			// this.factory.debug(this.visDebugger);
			this.controller.getEvents().addHandler(this.qSimDrawer);
			this.controller.getConfig().controler().setCreateGraphs(true);
		}
		else {
			// this.factory.debug(null);
			this.controller.getEvents().removeHandler(this.qSimDrawer);
			this.controller.getConfig().controler().setCreateGraphs(false);
		}
		// this.visDebugger.setIteration(event.getIteration());
	}
}
