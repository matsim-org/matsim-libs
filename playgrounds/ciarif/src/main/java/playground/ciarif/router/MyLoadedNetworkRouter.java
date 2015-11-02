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

package playground.ciarif.router;

import java.util.Iterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.ArgumentParser;

/**
 * Use this tool, if you want to find out the travel times for certain routes for a previous run.
 *
 * input: network, events file, plans (only those, which you want to find out the route/distance/estimated travel time)
 * output: plans file with the route, distance and estimated tavel time filled in.
 *
 * ===============
 * inorder to set it up for a different user:
 * 1.) change root path (and package it as a jar file)
 * 2.) place all files (network, config, events, input (plan) there
 * 3.) change the path of the network in the config file accordingly
 *
 *
 * @author wrashid
 *
 *
 *
 */

public class MyLoadedNetworkRouter {

	Config config;
	String configfile = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MyLoadedNetworkRouter loadedNetworkRouter=new MyLoadedNetworkRouter();
		loadedNetworkRouter.run(args);
	}

	/**
	 * Parses all arguments and sets the corresponding members.
	 *
	 * @param args
	 */
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			System.exit(0);
		} else {
			this.configfile = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				System.exit(1);
			}
		}
	}

	public void run(final String[] args) {
		
		String rootPathOut="/data/matsim/ciarif/output/routing/";
		String rootPath="/data/matsim/ciarif/input/routing/";
		String networkFile=rootPath + "network.car.xml.gz";
		String eventsFile=rootPath + "50.events.txt.gz";
		String inputPlansFile=rootPath + "inputPlanFile.xml";
		String outputPlansFile=rootPathOut + "outputPlanFileX.xml";

		parseArguments(args);
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(this.configfile);
		sl.loadNetwork();
		Network network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(outputPlansFile);

		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) plans.getFactory()).getModeRouteFactory();
		
		// add algorithm to map coordinates to links
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));

		// add algorithm to estimate travel cost
		// and which performs routing based on that
		TravelTimeCalculator travelTimeCalculator= Events2TTCalculator.getTravelTimeCalculator(sl.getScenario(), eventsFile);
		TravelDisutilityFactory travelCostCalculatorFactory = new Builder( TransportMode.car );
		TravelDisutility travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), this.config.planCalcScore());
		plans.addAlgorithm(
				new PlanRouter(
				new TripRouterFactoryBuilderWithDefaults().build(
						sl.getScenario() ).get(
				) ) );

		// add algorithm to write out the plans
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(inputPlansFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

}

